package com.maisonglow.view.orders;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.model.Product;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Orders gateway over the real order service")
class OrderServiceGatewayTest {

    private static final char[] ADMIN_PASSWORD = "clave-de-prueba-1".toCharArray();
    private static final char[] CUSTOMER_PASSWORD = "clave-segura-123".toCharArray();

    @TempDir
    Path data;

    private AppContext context;
    private OrdersGateway gateway;
    private Product serum;

    @BeforeEach
    void prepare() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        gateway = context.ordersGateway();
        context.auth().createFirstAdmin("jefa", ADMIN_PASSWORD);
        context.auth().registerCustomer(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"), "ana", CUSTOMER_PASSWORD);
        context.auth().login("jefa", ADMIN_PASSWORD);
        serum = context.products().register(new ProductData("Sérum Vitamina C", 1, 48000, 2), 10);
        context.catalog().setPublished(serum.getId(), true);
    }

    /** A pending order of 2 units placed by the customer; leaves the administrator signed in. */
    private int placeAnOrder() throws Exception {
        context.auth().login("ana", CUSTOMER_PASSWORD);
        int id = context.orders().place(UUID.randomUUID().toString(), Map.of(serum.getId(), 2)).id();
        context.auth().login("jefa", ADMIN_PASSWORD);
        return id;
    }

    @Test
    @DisplayName("a pending order shows the customer's name, its lines, the total and when the reservation ends")
    void pendingOrderIsListedAndDetailed() throws Exception {
        int id = placeAnOrder();

        OrderSummary row = gateway.list().getFirst();
        OrderDetail detail = gateway.detail(id);

        assertThat(row.id()).isEqualTo(id);
        assertThat(row.customerName()).isEqualTo("Ana García");
        assertThat(row.state()).isEqualTo(OrderState.PENDING_PAYMENT);
        assertThat(row.total()).isEqualByComparingTo("96000");
        assertThat(row.reservedUntil()).isAfter(row.createdAt());
        assertThat(detail.lines()).singleElement().satisfies(line -> {
            assertThat(line.name()).isEqualTo("Sérum Vitamina C");
            assertThat(line.quantity()).isEqualTo(2);
        });
        assertThat(detail.saleId()).isEmpty();
    }

    @Test
    @DisplayName("confirming the payment generates one sale, discounts the stock once and leaves a receipt")
    void confirmPaymentSellsOnce() throws Exception {
        int id = placeAnOrder();

        int saleId = gateway.confirmPayment(id, PaymentMethod.CASH);
        int again = gateway.confirmPayment(id, PaymentMethod.CASH);

        assertThat(saleId).isPositive().isEqualTo(again);
        assertThat(gateway.detail(id).summary().state()).isEqualTo(OrderState.PAID);
        assertThat(gateway.detail(id).summary().reservedUntil()).isNull();
        assertThat(gateway.detail(id).saleId()).contains(saleId);
        assertThat(context.products().findById(serum.getId()).getStock()).isEqualTo(8);
        assertThat(gateway.receipt(id)).contains("Sérum Vitamina C");
    }

    @Test
    @DisplayName("cancelling releases the reservation without touching the stock, and a paid order can no longer be cancelled")
    void cancelReleasesTheReservation() throws Exception {
        int id = placeAnOrder();
        assertThat(context.catalog().findPublished(serum.getId()).availableStock()).isEqualTo(8);

        gateway.cancel(id);

        assertThat(gateway.detail(id).summary().state()).isEqualTo(OrderState.CANCELLED);
        assertThat(context.catalog().findPublished(serum.getId()).availableStock()).isEqualTo(10);
        assertThat(context.products().findById(serum.getId()).getStock()).isEqualTo(10);
        assertThatThrownBy(() -> gateway.receipt(id)).isInstanceOf(BusinessRuleException.class);

        int paid = placeAnOrder();
        gateway.confirmPayment(paid, PaymentMethod.CARD);
        assertThatThrownBy(() -> gateway.cancel(paid)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("a customer cannot list the orders of the shop or pay one: the service refuses it")
    void customerIsRefused() throws Exception {
        int id = placeAnOrder();
        context.auth().login("ana", CUSTOMER_PASSWORD);

        assertThatThrownBy(() -> gateway.list()).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> gateway.confirmPayment(id, PaymentMethod.CASH)).isInstanceOf(BusinessRuleException.class);
        context.auth().logout();
        assertThatThrownBy(() -> gateway.list()).isInstanceOf(BusinessRuleException.class);
    }
}
