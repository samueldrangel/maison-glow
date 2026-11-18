package com.maisonglow.integration;

import com.maisonglow.*;
import com.maisonglow.ai.*;
import com.maisonglow.dao.*;
import com.maisonglow.exception.*;
import com.maisonglow.model.*;
import com.maisonglow.model.Commerce.*;
import com.maisonglow.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import static org.assertj.core.api.Assertions.*;

class RoleBackendIntegrationTest {
    @TempDir Path folder;
    private AppContext app;
    private final MutableClock clock=new MutableClock();
    private UserAccount alice,bob;
    private Product product;
    private Treatment treatment;
    private Professional professional;
    private static char[] password(){return "role-backend-fixture".toCharArray();}
    private AppContext open(){return new AppContext(folder,clock,AiConfig.simulated());}
    private void login(String user)throws Exception{app.auth().login(user,password());}
    private ContactData contact(String id){return new ContactData(id,id,"3001234567",id+"@example.invalid");}
    @BeforeEach void setup()throws Exception {
        app=open();app.auth().createFirstAdmin("admin",password());login("admin");
        alice=app.auth().registerCustomer(contact("alice"),"alice",password());bob=app.auth().registerCustomer(contact("bobby"),"bobby",password());
        product=app.products().register(new ProductData("Producto",1,new BigDecimal("12.35"),1),3);
        treatment=app.treatments().register(new TreatmentData("Corte","",new BigDecimal("100"),60));
        professional=app.professionals().register(contact("pro"),"Cabello",new WorkSchedule(LocalTime.of(8,0),LocalTime.of(18,0)));
        app.professionals().assignTreatment(professional.getId(),treatment.getId());
    }
    private void publish()throws Exception{login("admin");app.catalog().setPublished(product.getId(),true);login("alice");}
    private LocalDateTime slot(){return LocalDateTime.now(clock).plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);}
    private Path image(String name)throws Exception{Path file=folder.resolve(name);ImageIO.write(new BufferedImage(30,20,BufferedImage.TYPE_INT_RGB),"png",file.toFile());return file;}
    private long count(String table)throws Exception{return app.database().transaction(c -> {try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){r.next();return r.getLong(1);}});}

    @Test void publicationAndCoverPersistAndCannotBeChangedByCustomer()throws Exception {
        Path source=image("source.png");app.catalog().setCover(product.getId(),source);app.catalog().setPublished(product.getId(),true);
        login("alice");assertThat(app.catalog().listPublished("PRODUCTO")).hasSize(1);
        Path saved=app.catalog().cover(product.getId());assertThat(saved).exists().isNotEqualTo(source);Files.delete(source);
        var reopened=open();reopened.auth().login("alice",password());assertThat(reopened.catalog().cover(product.getId())).exists();
        assertThatThrownBy(() -> app.catalog().setPublished(product.getId(),false)).isInstanceOf(AuthorizationException.class);
        login("admin");app.catalog().setPublished(product.getId(),false);login("alice");
        assertThat(app.catalog().listPublished("")).isEmpty();assertThatThrownBy(() -> app.catalog().cover(product.getId())).isInstanceOf(AuthorizationException.class);
    }
    @Test void invalidPhotoPreservesOldCoverAndReplacingRemovesOldManagedFile()throws Exception {
        app.catalog().setCover(product.getId(),image("one.png"));Path old=app.catalog().cover(product.getId());
        Path bad=folder.resolve("bad.jpg");Files.writeString(bad,"not an image");
        assertThatThrownBy(() -> app.catalog().setCover(product.getId(),bad)).isInstanceOf(ValidationException.class);
        assertThat(app.catalog().cover(product.getId())).isEqualTo(old);assertThat(old).exists();
        app.catalog().setCover(product.getId(),image("two.png"));assertThat(old).doesNotExist();assertThat(app.catalog().cover(product.getId())).exists();
        assertThatThrownBy(() -> new PhotoStore(folder.resolve("media")).delete("../private.png")).isInstanceOf(DataAccessException.class);
    }
    @Test void checkoutIsIdempotentAndCannotTrustPricesOrAnotherCustomerId()throws Exception {
        publish();Order order=app.orders().place("request-0001",Map.of(product.getId(),2));
        assertThat(order.customerId()).isEqualTo(alice.customerId());assertThat(order.total()).isEqualByComparingTo("24.70");
        assertThat(app.orders().place("request-0001",Map.of(product.getId(),2)).id()).isEqualTo(order.id());
        assertThat(count("customer_order")).isEqualTo(1);assertThat(count("sale")).isZero();
        assertThat(app.catalog().findPublished(product.getId()).availableStock()).isEqualTo(1);
        assertThatThrownBy(() -> app.orders().place("request-0001",Map.of(product.getId(),1))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.orders().confirmPayment(order.id(),"CASH")).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.orders().receipt(order.id())).isInstanceOf(BusinessRuleException.class);
        login("bobby");assertThat(app.orders().mine()).isEmpty();assertThatThrownBy(() -> app.orders().find(order.id())).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.orders().cancel(order.id())).isInstanceOf(AuthorizationException.class);
    }
    @Test void cancellingAndExpiryReleaseExactlyOnce()throws Exception {
        publish();Order order=app.orders().place("request-cancel",Map.of(product.getId(),3));
        app.orders().cancel(order.id());app.orders().cancel(order.id());
        assertThat(app.catalog().findPublished(product.getId()).availableStock()).isEqualTo(3);
        Order expires=app.orders().place("request-expire",Map.of(product.getId(),3));clock.now=clock.now.plus(Duration.ofMinutes(30));
        assertThat(app.orders().find(expires.id()).status()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(app.catalog().findPublished(product.getId()).availableStock()).isEqualTo(3);
        login("admin");assertThatThrownBy(() -> app.orders().confirmPayment(expires.id(),"CASH")).isInstanceOf(BusinessRuleException.class);
    }
    @Test void paymentCreatesExactlyOneSaleAndPaymentAndKeepsReservedPrice()throws Exception {
        publish();Order order=app.orders().place("request-payment",Map.of(product.getId(),2));
        login("admin");app.products().update(product.getId(),new ProductData("Producto",1,new BigDecimal("99"),1));
        Order paid=app.orders().confirmPayment(order.id(),"CASH");
        assertThat(app.orders().confirmPayment(order.id(),"CASH")).isEqualTo(paid);
        assertThat(count("sale")).isEqualTo(1);assertThat(count("payment")).isEqualTo(1);assertThat(app.products().findById(product.getId()).getStock()).isEqualTo(1);
        login("alice");assertThat(app.orders().receipt(order.id()).getTotal()).isEqualByComparingTo("24.70");assertThat(app.portal().myPurchases()).hasSize(1);
        assertThatThrownBy(() -> app.orders().cancel(order.id())).isInstanceOf(BusinessRuleException.class);
    }
    @Test void failedPaymentRollsBackStateStockSaleAndPayment()throws Exception {
        publish();Order order=app.orders().place("request-failure",Map.of(product.getId(),2));login("admin");
        app.database().transaction(c -> {try(var s=c.createStatement()){s.execute("CREATE TRIGGER fail_payment BEFORE INSERT ON payment BEGIN SELECT RAISE(ABORT,'test_failure'); END;");}return null;});
        assertThatThrownBy(() -> app.orders().confirmPayment(order.id(),"CASH")).isInstanceOf(DataAccessException.class);
        assertThat(app.orders().find(order.id()).status()).isEqualTo(OrderStatus.PENDING_PAYMENT);assertThat(app.products().findById(product.getId()).getStock()).isEqualTo(3);
        assertThat(count("sale")).isZero();assertThat(count("payment")).isZero();
    }
    @Test void oldCashRegisterAndStockAdjustmentsCannotConsumeReservedUnits()throws Exception {
        publish();app.orders().place("request-reserved",Map.of(product.getId(),3));login("admin");
        Sale sale=new Sale(app.customers().findById(alice.customerId()));SaleService.addLine(sale,product,1);
        assertThatThrownBy(() -> app.sales().register(sale)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> app.products().adjustStock(product.getId(),-1)).isInstanceOf(DataAccessException.class);
        assertThat(app.products().findById(product.getId()).getStock()).isEqualTo(3);
    }
    @Test void twoContextsCannotReserveTheLastUnitTwice()throws Exception {
        app.products().adjustStock(product.getId(),-2);publish();
        AppContext second=open();second.auth().login("bobby",password());
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> first=() -> {start.await();try{app.orders().place("last-unit-1",Map.of(product.getId(),1));return true;}catch(BusinessRuleException|DataAccessException e){return false;}};
            Callable<Boolean> other=() -> {start.await();try{second.orders().place("last-unit-2",Map.of(product.getId(),1));return true;}catch(BusinessRuleException|DataAccessException e){return false;}};
            var a=pool.submit(first);var b=pool.submit(other);start.countDown();
            assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(count("customer_order")).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void unpublishedAndOverstockedCartsAreRejectedWithoutPartialOrders()throws Exception {
        login("alice");assertThatThrownBy(() -> app.orders().place("request-unpub",Map.of(product.getId(),1))).isInstanceOf(BusinessRuleException.class);
        publish();assertThatThrownBy(() -> app.orders().place("request-stock",Map.of(product.getId(),4))).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.orders().place("request-invalid",Map.of(product.getId(),0))).isInstanceOf(ValidationException.class);
        assertThat(count("customer_order")).isZero();
    }
    @Test void profileAndAppointmentsAreOwnedAndDoNotRecordFictitiousDeposits()throws Exception {
        login("alice");var appointment=app.portal().book(professional.getId(),treatment.getId(),slot());
        assertThat(appointment.getCustomer().getId()).isEqualTo(alice.customerId());assertThat(appointment.getDeposit()).isZero();assertThat(app.portal().requiredDeposit(appointment.getId())).isGreaterThan(BigDecimal.ZERO);
        app.portal().updateContact("Nombre editado","3007654321","new@example.invalid");assertThat(app.portal().profile().getDocument()).isEqualTo("alice");
        login("bobby");assertThat(app.portal().myAppointments()).isEmpty();assertThat(app.portal().available(professional.getId(),treatment.getId(),slot())).isFalse();
        assertThatThrownBy(() -> app.portal().reschedule(appointment.getId(),slot().plusHours(2))).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.portal().book(professional.getId(),treatment.getId(),slot())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.portal().recordDeposit(appointment.getId(),new BigDecimal("50"),"CASH")).isInstanceOf(AuthorizationException.class);
        login("admin");assertThatThrownBy(() -> app.appointments().confirm(appointment.getId())).isInstanceOf(BusinessRuleException.class);
        app.portal().recordDeposit(appointment.getId(),new BigDecimal("100"),"CASH");app.portal().recordDeposit(appointment.getId(),new BigDecimal("100"),"CASH");app.appointments().confirm(appointment.getId());
        assertThat(count("payment")).isEqualTo(1);
    }
    @Test void customerCanMoveAndCancelOnlyWithinAllowedWindow()throws Exception {
        login("alice");var a=app.portal().book(professional.getId(),treatment.getId(),slot());
        assertThat(app.portal().reschedule(a.getId(),slot().plusHours(1)).getStart()).isEqualTo(slot().plusHours(1));
        app.portal().cancelAppointment(a.getId());assertThat(app.portal().myAppointments().getFirst().getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        var other=app.portal().book(professional.getId(),treatment.getId(),slot().plusHours(2));clock.now=slot().plusHours(2).toInstant(ZoneOffset.UTC);
        assertThatThrownBy(() -> app.portal().cancelAppointment(other.getId())).isInstanceOf(BusinessRuleException.class);
    }
    @Test void personalHistoryIsPrivatePersistentAndPhotosCanBeRemoved()throws Exception {
        login("alice");Path source=image("before.png"),result=image("after.png");
        var change=app.changes().addOwn(ChangeKind.SIMULATION,"Prueba de tono",LocalDate.now(clock),source,result);
        Path saved=app.changes().photo(change.id(),false);Files.delete(source);Files.delete(result);assertThat(saved).exists();
        var reopened=open();reopened.auth().login("alice",password());assertThat(reopened.changes().mine()).hasSize(1);assertThat(reopened.changes().photo(change.id(),true)).exists();
        login("bobby");assertThatThrownBy(() -> app.changes().photo(change.id(),false)).isInstanceOf(AuthorizationException.class);
        login("admin");assertThatThrownBy(() -> app.changes().forCustomer(alice.customerId())).isInstanceOf(AuthorizationException.class);
        login("alice");app.changes().allowStaffAccess(true);login("admin");assertThat(app.changes().forCustomer(alice.customerId())).hasSize(1);
        login("alice");app.changes().allowStaffAccess(false);login("admin");assertThatThrownBy(() -> app.changes().photo(change.id(),false)).isInstanceOf(AuthorizationException.class);
        login("alice");app.changes().removePhotos(change.id());assertThat(saved).doesNotExist();assertThat(app.changes().mine().getFirst().notes()).isEqualTo("Prueba de tono");
    }
    @Test void invalidHistoryAndMissingImagesDoNotLeaveFilesOrRecords()throws Exception {
        login("alice");Path source=image("before.png");
        assertThatThrownBy(() -> app.changes().addOwn(ChangeKind.SERVICE,"fake",LocalDate.now(clock),source,null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> app.changes().addOwn(ChangeKind.SIMULATION,"missing",LocalDate.now(clock),source,null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> app.changes().addOwn(ChangeKind.SIMULATION,"bad",LocalDate.now(clock),source,folder.resolve("missing.png"))).isInstanceOf(ValidationException.class);
        assertThat(app.changes().mine()).isEmpty();
        try(var files=Files.list(folder.resolve("media"))){assertThat(files.count()).isZero();}
    }
    @Test void serviceHistoryRequiresCompletedAppointmentAndConsent()throws Exception {
        login("alice");app.changes().allowStaffAccess(true);var a=app.portal().book(professional.getId(),treatment.getId(),slot());
        login("admin");assertThatThrownBy(() -> app.changes().addService(a.getId(),"Trabajo",null,null)).isInstanceOf(BusinessRuleException.class);
        clock.now=a.getEnd().toInstant(ZoneOffset.UTC);login("admin");app.appointments().complete(a.getId());var record=app.changes().addService(a.getId(),"Trabajo realizado",null,null);assertThat(record.kind()).isEqualTo(ChangeKind.SERVICE);assertThat(record.appointmentId()).isEqualTo(a.getId());
    }
    @Test void retainedLegacyServicesAndSessionsNeverBypassPermissions()throws Exception {
        var retained=app.customers();app.auth().logout();
        assertThatThrownBy(retained::listAll).isInstanceOfSatisfying(AccessDeniedDataException.class,e -> assertThat(e.isSessionExpired()).isTrue());
        login("alice");assertThatThrownBy(() -> app.products().listAll()).isInstanceOf(AccessDeniedDataException.class);
        assertThatThrownBy(() -> app.appointments().listByDate(LocalDate.now(clock))).isInstanceOf(AccessDeniedDataException.class);
        assertThatThrownBy(() -> app.statistics().customerStatistics(bob.customerId())).isInstanceOf(AccessDeniedDataException.class);
        assertThatThrownBy(() -> app.auth().listAccounts()).isInstanceOf(AuthorizationException.class);
        login("admin");assertThat(app.auth().listAccounts()).hasSize(3);
    }
    @Test void aiWhitelistsAndPendingActionsAreBoundToOneLogin()throws Exception {
        login("alice");var assistant=app.ai().assistant();
        assertThat(assistant.reply("stock bajo").text()).contains("no registrada");
        assertThat(assistant.reply("buscar cliente bobby").text()).contains("no registrada");
        String command="book_appointment professionalId="+professional.getId()+"; treatmentId="+treatment.getId()+"; start="+slot();
        var pending=assistant.reply(command).pendingConfirmation().orElseThrow();assertThat(app.portal().myAppointments()).isEmpty();
        app.auth().logout();login("alice");assertThat(assistant.confirm(pending).text()).contains("no está pendiente");assertThat(app.portal().myAppointments()).isEmpty();
        var valid=assistant.reply(command).pendingConfirmation().orElseThrow();assistant.confirm(valid);assertThat(app.portal().myAppointments()).hasSize(1);
        assertThatThrownBy(() -> app.ai().tryOn().generate(new TryOnRequest(bob.customerId(),image("other.png"),"rubio"))).isInstanceOf(ValidationException.class);
    }
    @Test void aiHistoriesCannotBeReadAcrossAccountsOrClaimLegacyFiles()throws Exception {
        Files.writeString(folder.resolve("assistant_conversations.txt"),"1|1|2026-11-17T10:00\n");
        login("alice");var conversation=app.aiHistory().startConversation(alice.id());app.aiHistory().addMessage(conversation.id(),AiHistory.Sender.USER,"privado");
        assertThat(app.aiHistory().conversations()).hasSize(1);
        login("bobby");assertThat(app.aiHistory().conversations()).isEmpty();assertThatThrownBy(() -> app.aiHistory().messages(conversation.id())).isInstanceOf(AccessDeniedDataException.class);
        assertThatThrownBy(() -> app.aiHistory().startConversation(alice.id())).isInstanceOf(AccessDeniedDataException.class);
    }
    @Test void restartExpiresOrdersAndMigrationRetainsBusinessAndPhotos()throws Exception {
        publish();app.orders().place("request-restart",Map.of(product.getId(),3));clock.now=clock.now.plus(Duration.ofMinutes(31));
        AppContext reopened=open();reopened.auth().login("alice",password());assertThat(reopened.catalog().findPublished(product.getId()).availableStock()).isEqualTo(3);
        assertThat(reopened.orders().mine().getFirst().status()).isEqualTo(OrderStatus.EXPIRED);assertThat(count("schema_migration")).isEqualTo(2);
    }
    @Test void failedCoverWriteLeavesOldPhotoAndNoOrphan()throws Exception {
        app.catalog().setCover(product.getId(),image("old-cover.png"));Path old=app.catalog().cover(product.getId());
        app.database().transaction(c -> {try(var s=c.createStatement()){s.execute("CREATE TRIGGER fail_cover BEFORE UPDATE ON product_listing BEGIN SELECT RAISE(ABORT,'test_failure'); END;");}return null;});
        assertThatThrownBy(() -> app.catalog().setCover(product.getId(),image("new-cover.png"))).isInstanceOf(DataAccessException.class);
        assertThat(app.catalog().cover(product.getId())).isEqualTo(old);assertThat(old).exists();
        try(var files=Files.list(folder.resolve("media"))){assertThat(files.count()).isEqualTo(1);}
    }
    @Test void completeBackupRestoresOrdersAndManagedPhotosInAnotherFolder(@TempDir Path restoredFolder)throws Exception {
        app.catalog().setCover(product.getId(),image("cover.png"));publish();
        var order=app.orders().place("request-backup",Map.of(product.getId(),1));
        var change=app.changes().addOwn(ChangeKind.PERSONAL,"Cambio guardado",LocalDate.now(clock),image("own.png"),null);
        List<Path> snapshot;try(var paths=Files.walk(folder)){snapshot=paths.toList();}
        for(Path source:snapshot) {
            Path target=restoredFolder.resolve(folder.relativize(source)).normalize();assertThat(target.normalize().startsWith(restoredFolder.normalize())).isTrue();
            if(Files.isDirectory(source))Files.createDirectories(target);else Files.copy(source,target);
        }
        var restored=new AppContext(restoredFolder,clock,AiConfig.simulated());restored.auth().login("alice",password());
        assertThat(restored.orders().find(order.id()).total()).isEqualByComparingTo(order.total());
        assertThat(restored.catalog().cover(product.getId())).exists().startsWith(restoredFolder);
        assertThat(restored.changes().photo(change.id(),false)).exists().startsWith(restoredFolder);
        assertThat(restored.changes().mine()).hasSize(1);
    }
    private static final class MutableClock extends Clock {
        private Instant now=Instant.parse("2026-11-17T12:00:00Z");
        @Override public ZoneId getZone(){return ZoneOffset.UTC;}
        @Override public Clock withZone(ZoneId zone){return this;}
        @Override public Instant instant(){return now;}
    }
}
