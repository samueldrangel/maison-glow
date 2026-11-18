package com.maisonglow.service;

import com.maisonglow.dao.*;
import com.maisonglow.exception.*;
import com.maisonglow.model.*;
import com.maisonglow.model.Commerce.*;
import com.maisonglow.service.auth.PasswordHasher;
import com.maisonglow.service.rule.DepositRule;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Explicit, repeatable fixtures in an isolated folder. Never invoked by application startup. */
public final class LocalSeedService {
    public enum Profile {
        SMALL(12,2,1,12,6,3,36,24,12,12),
        LARGE(1000,300,3,200,30,20,5000,3000,1000,1000);
        final int customers,customerAccounts,admins,products,treatments,professionals,appointments,sales,orders,changes;
        Profile(int customers,int customerAccounts,int admins,int products,int treatments,int professionals,int appointments,int sales,int orders,int changes){
            this.customers=customers;this.customerAccounts=customerAccounts;this.admins=admins;this.products=products;this.treatments=treatments;this.professionals=professionals;this.appointments=appointments;this.sales=sales;this.orders=orders;this.changes=changes;
        }
    }
    public record Report(Profile profile,Path folder,Map<String,Long> counts,long elapsedMillis,boolean reused) { }
    private static final String MANIFEST="seed-manifest.txt";
    private final PasswordHasher passwords=new PasswordHasher();

    /** Signature fixes the domain dataset; password salts and generated media IDs remain random by design. */
    public Report generate(Path destination,Profile profile,LocalDateTime reference,long randomSeed,char[] password)
        throws IOException,ValidationException,DataAccessException {
        if(destination==null||profile==null||reference==null)throw new ValidationException("Indica destino, perfil y fecha/hora de referencia.");
        passwords.validate(password);
        Path folder=destination.toAbsolutePath().normalize();
        requireDemoLocation(folder);
        Files.createDirectories(folder);
        String signature="seed-v1|"+profile+"|"+reference+"|"+randomSeed;
        Path manifest=folder.resolve(MANIFEST),lock=folder.resolve("seed.lock");
        boolean owned=Files.isRegularFile(manifest)&&!Files.isSymbolicLink(manifest);
        if(owned&&!Files.readString(manifest).trim().equals(signature))throw new ValidationException("Ese destino pertenece a otra semilla. Usa una carpeta distinta.");
        if(!owned)try(var entries=Files.list(folder)){if(entries.findAny().isPresent())throw new ValidationException("El destino no está vacío ni pertenece a esta semilla. No se modificó su contenido.");}
        long started=System.nanoTime();
        Files.writeString(lock,signature,StandardOpenOption.CREATE_NEW);
        try {
            if(!owned)Files.writeString(manifest,signature,StandardOpenOption.CREATE_NEW);
            Path file=folder.resolve("maison-glow.db");
            if(Files.isSymbolicLink(file))throw new ValidationException("La base de demo no puede ser un enlace.");
            if(owned&&Files.isRegularFile(file)){
                var audit=new SeedAuditDao(new SqliteDatabase(file));
                var completed=audit.signature();
                if(completed.isPresent()){
                    if(!completed.get().equals(signature))throw new ValidationException("La firma de la base no coincide con la semilla.");
                    return new Report(profile,folder,audit.counts(),elapsed(started),true);
                }
                var counts=audit.counts();
                if(counts.entrySet().stream().anyMatch(e -> !e.getKey().equals("product_category")&&e.getValue()!=0))
                    throw new ValidationException("Hay datos en una semilla incompleta; usa otra carpeta. No se sobrescribió nada.");
            }
            SqliteDatabase db=SqliteBootstrap.open(folder);
            SeedAuditDao audit=new SeedAuditDao(db);
            PhotoStore photos=new PhotoStore(folder.resolve("media"));List<String> imported=new ArrayList<>();
            try {
                db.transaction(c -> {
                    try {populate(db,photos,imported,profile,reference,randomSeed,password);}
                    catch(ValidationException|IOException e){throw new DataAccessException("No se pudo construir la semilla.",e);}
                    audit.validate();audit.complete(signature);return null;
                });
            }catch(DataAccessException|RuntimeException e){for(String key:imported)try{photos.delete(key);}catch(DataAccessException cleanup){e.addSuppressed(cleanup);}throw e;}
            return new Report(profile,folder,audit.counts(),elapsed(started),false);
        } finally {Files.deleteIfExists(lock);}
    }
    private static long elapsed(long started){return (System.nanoTime()-started)/1_000_000;}

    /** Opens only a completed, identified demo. Does not create, copy, reset or seed anything. */
    public static Path openDemoFolder(Path destination)throws IOException,ValidationException,DataAccessException {
        Path folder=destination.toAbsolutePath().normalize();requireDemoLocation(folder);
        Path manifest=folder.resolve(MANIFEST),file=folder.resolve("maison-glow.db");
        if(!Files.isRegularFile(manifest)||Files.isSymbolicLink(manifest)||!Files.isRegularFile(file)||Files.isSymbolicLink(file))
            throw new ValidationException("La carpeta no contiene una demo local completa.");
        String expected=Files.readString(manifest).trim();
        if(!new SeedAuditDao(new SqliteDatabase(file)).signature().filter(expected::equals).isPresent())
            throw new ValidationException("La firma de la demo no coincide o su generación está incompleta.");
        return folder;
    }
    private static void requireDemoLocation(Path folder)throws ValidationException {
        for(Path part=folder;part!=null;part=part.getParent())
            if(part.endsWith(Path.of("data","session")))throw new ValidationException("La semilla debe ir fuera de data/session/ y sus subcarpetas.");
        if(Files.isSymbolicLink(folder))throw new ValidationException("La carpeta de demo no puede ser un enlace.");
    }

    private void populate(SqliteDatabase db,PhotoStore photos,List<String> imported,Profile p,LocalDateTime now,long seed,char[] password)
        throws DataAccessException,ValidationException,IOException {
        var customers=new CustomerDaoSqlite(db);var treatments=new TreatmentDaoSqlite(db);var categories=new CategoryDaoSqlite(db);
        var products=new ProductDaoSqlite(db,categories);var professionals=new ProfessionalDaoSqlite(db,treatments);
        var appointments=new AppointmentDaoSqlite(db,customers,professionals,treatments);
        var sales=new SaleDaoSqlite(db,customers,appointments,products,treatments);var accounts=new AccountDaoSqlite(db);
        var store=new CommerceDao(db);var audit=new SeedAuditDao(db);
        var settings=new SettingsService(new SettingDaoSqlite(db));var depositRule=new DepositRule(settings);
        settings.update(SettingsService.BUSINESS_NAME,"Maison Glow - DEMO FICTICIA");
        Random random=new Random(seed);
        List<UserAccount> admins=new ArrayList<>();
        for(int i=1;i<=p.admins;i++)admins.add(accounts.create("demo-admin-"+i,passwords.hash(password),UserRole.ADMIN,null));
        List<Customer> clientList=new ArrayList<>();List<UserAccount> customerAccounts=new ArrayList<>();
        for(int i=1;i<=p.customers;i++){
            var client=customers.create(new Customer(0,"DEMO-C-"+i,"Cliente demo "+i+" (ficticio)","3000000000",String.format(Locale.ROOT,"cliente%04d@example.invalid",i),now.toLocalDate().minusDays(365L-i%300),0));
            clientList.add(client);
            if(i<=p.customerAccounts)customerAccounts.add(accounts.create(String.format(Locale.ROOT,"demo-cliente-%04d",i),passwords.hash(password),UserRole.CUSTOMER,client.getId()));
        }
        audit.accountDates(now.minusDays(1));
        int categoryCount=p==Profile.LARGE?12:5;
        while(categories.findAll().size()<categoryCount)categories.create(new ProductCategory(0,"Categoría demo "+(categories.findAll().size()+1)));
        List<ProductCategory> categoryList=categories.findAll();List<Product> productList=new ArrayList<>(),sellable=new ArrayList<>();
        for(int i=0;i<p.products;i++){
            Product product=new Product(0,"Producto demo "+(i+1),categoryList.get(i%categoryList.size()),BigDecimal.valueOf(1000L+random.nextInt(9000),2),0,5);
            product.setActive(i%17!=0);products.create(product);productList.add(product);
            int initial=i%11==0?0:(i%13==0?2:10000);audit.restock(product.getId(),initial,admins.getFirst().id(),now.minusMonths(8));
            store.saveListing(product.getId(),product.isActive()&&i%7!=0,null);
            if(product.isActive()&&initial==10000)sellable.add(product);
        }
        List<Treatment> treatmentList=new ArrayList<>();
        for(int i=0;i<p.treatments;i++){
            var treatment=new Treatment(0,"Tratamiento demo "+(i+1),"Servicio ficticio para pruebas",BigDecimal.valueOf(2000+random.nextInt(8000)),30+(i%3)*15);
            treatmentList.add(treatments.create(treatment));
        }
        List<Professional> professionalList=new ArrayList<>();
        for(int i=0;i<p.professionals;i++){
            var professional=new Professional(0,"DEMO-P-"+(i+1),"Profesional demo "+(i+1),"3000000000","profesional"+i+"@example.invalid","Belleza",LocalTime.of(8,0),LocalTime.of(18,0));
            professional.setTreatments(treatmentList);professionalList.add(professionals.create(professional));
        }
        List<Appointment> completed=new ArrayList<>();int past=p.appointments*4/5;
        for(int i=0;i<p.appointments;i++){
            boolean future=i>=past;int position=future?i-past:i;
            Customer customer=clientList.get(i%(p.customers-2));
            var professional=professionalList.get(position%p.professionals);var treatment=treatmentList.get(i%p.treatments);
            LocalDate date=(future?now.toLocalDate().plusDays(1):now.toLocalDate().minusMonths(6)).plusDays(position/(p.professionals*4));
            LocalDateTime start=date.atTime(9+((position/p.professionals)%4)*2,0);
            AppointmentStatus status=future?(i%2==0?AppointmentStatus.PENDING:AppointmentStatus.CONFIRMED):(i%7==0?AppointmentStatus.NO_SHOW:(i%5==0?AppointmentStatus.CANCELLED:AppointmentStatus.COMPLETED));
            if(status==AppointmentStatus.NO_SHOW)customer.setNoShowCount(customer.getNoShowCount()+1);
            var appointment=new Appointment(0,customer,professional,treatment,start,AppointmentService.calculateEnd(treatment,start),status,(future?now.minusHours(1):start.minusDays(2)));
            if(status==AppointmentStatus.CONFIRMED||status==AppointmentStatus.COMPLETED)appointment.setDeposit(depositRule.requiredDeposit(appointment));
            appointment.setReminderAt(start.minusHours(24));appointments.create(appointment);
            if(appointment.getDeposit().signum()>0)store.recordDeposit(appointment.getId(),"CASH",appointment.getDeposit(),appointment.getCreatedAt());
            if(status==AppointmentStatus.COMPLETED&&customer.getId()<=p.customerAccounts)completed.add(appointment);
        }
        for(Customer customer:clientList)customers.update(customer);
        List<Sale> saleList=new ArrayList<>();
        for(int i=0;i<p.sales;i++){
            Product one=sellable.get(i%sellable.size()),two=sellable.get((i+1)%sellable.size());
            List<SaleLine> lines=List.of(new SaleLine(one,1+random.nextInt(3),one.getPrice()),new SaleLine(two,1+random.nextInt(3),two.getPrice()));
            LocalDateTime sold=now.minusMinutes(5L+i*15L);
            Sale sale=SaleService.restoreSale(0,clientList.get(i%(p.customers-2)),sold,lines);sales.create(sale);saleList.add(sale);
            audit.saleMovements(sale.getId(),admins.getFirst().id(),sold);store.recordPayment(sale.getId(),"CASH",sale.getTotal(),sold);
        }
        for(int i=0;i<p.orders;i++){
            OrderStatus state=OrderStatus.values()[i%4];
            Sale sale=saleList.get(i%saleList.size());int customer=state==OrderStatus.PAID?sale.getCustomer().getId():clientList.get(i%(p.customers-2)).getId();
            List<OrderLine> lines=state==OrderStatus.PAID?sale.getLines().stream().map(l -> new OrderLine(l.getItem().getId(),l.getItem().getName(),l.getQuantity(),l.getUnitPrice())).toList():List.of(new OrderLine(sellable.get(i%sellable.size()).getId(),sellable.get(i%sellable.size()).getName(),1,sellable.get(i%sellable.size()).getPrice()));
            BigDecimal total=lines.stream().map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity()))).reduce(BigDecimal.ZERO,BigDecimal::add);
            LocalDateTime created=state==OrderStatus.PENDING_PAYMENT?now.minusMinutes(1):(state==OrderStatus.PAID?sale.getSoldAt().minusMinutes(5):now.minusHours(2));
            var order=store.insertOrder(customer,"demo-order-"+i,created,created.plusMinutes(30),total,lines);
            store.orderState(order.id(),state,state==OrderStatus.PAID?sale.getId():null);
        }
        Path sample=Files.createTempFile("maison-glow-demo-image-",".png");
        try {
            try(var resource=LocalSeedService.class.getResourceAsStream("/com/maisonglow/view/images/app-icon.png")){
                if(resource==null)throw new IOException("Falta la imagen de ejemplo incluida.");Files.copy(resource,sample,StandardCopyOption.REPLACE_EXISTING);
            }
            for(int i=0;i<Math.min(20,p.products);i++)if(i%3==0){String key=imported(photos,imported,sample);var listing=store.listing(productList.get(i).getId());store.saveListing(productList.get(i).getId(),listing.published(),key);}
            for(int i=0;i<p.changes;i++){
                UserAccount owner=customerAccounts.get(i%customerAccounts.size());Integer appointmentId=null;
                LocalDate happened=now.toLocalDate().minusDays(i%90);
                ChangeKind kind=i%3==0?ChangeKind.SIMULATION:ChangeKind.PERSONAL;int author=owner.id(),customer=owner.customerId();
                if(i%5==0&&!completed.isEmpty()){
                    var appointment=completed.get(i%completed.size());customer=appointment.getCustomer().getId();appointmentId=appointment.getId();kind=ChangeKind.SERVICE;author=admins.getFirst().id();store.setStaffAccess(customer,true);
                    happened=appointment.getStart().toLocalDate();
                }
                boolean withPhoto=i<Math.min(100,p.changes);
                // Reuse the bundled icon as a clearly marked placeholder; no real people or AI calls.
                String source=withPhoto?imported(photos,imported,sample):null;
                String result=withPhoto&&kind==ChangeKind.SIMULATION?imported(photos,imported,sample):null;
                if(kind==ChangeKind.SIMULATION&&!withPhoto)kind=ChangeKind.PERSONAL;
                store.insertChange(customer,author,appointmentId,kind,"(DEMO FICTICIA) Registro "+i+". Imagen de ejemplo: icono de la aplicación, sin personas.",happened,now.minusMinutes(i),source,result);
            }
        } finally {Files.deleteIfExists(sample);}
    }
    private static String imported(PhotoStore photos,List<String> keys,Path source)throws ValidationException,DataAccessException {
        String key=photos.importPhoto(source);keys.add(key);return key;
    }
}
