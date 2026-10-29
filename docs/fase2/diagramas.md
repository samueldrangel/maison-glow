# Diagramas finales · Maison Glow (Entrega 1)

> Diagramas de paquetes y de clases **generados a partir del código real** en `src/main/java/com/maisonglow` (tarea I-06).
> Los diagramas de diseño previos, con nombres en español, están en [`diseno-clases.md`](diseno-clases.md); aquí se usan los nombres del código (inglés).
> Los diagramas son Mermaid y se ven directamente en GitHub.

**Contenido**
1. [Diagrama de paquetes](#1-diagrama-de-paquetes)
2. [Modelo de dominio](#2-modelo-de-dominio)
3. [Acceso a datos (DAO)](#3-acceso-a-datos-dao)
4. [Servicios, reglas y notificadores](#4-servicios-reglas-y-notificadores)
5. [Diferencias frente al diseño inicial](#5-diferencias-frente-al-diseño-inicial)

---

## 1. Diagrama de paquetes

Las flechas son dependencias `import` verificadas en el código. Ningún paquete de capa inferior depende de uno superior: `model` solo conoce a `exception`, y `dao` no conoce a `service`.

```mermaid
flowchart TB
    MAIN["com.maisonglow<br/>Main (demo de consola, composition root)"]

    subgraph NEG["Capa de negocio"]
        SERVICE["service<br/>(servicios, DTO, notificadores)"]
        RULE["service.rule<br/>(AppointmentRule y estrategias)"]
        MODEL["model<br/>(entidades del dominio)"]
    end
    subgraph DATOS["Capa de datos"]
        DAO["dao<br/>(CrudDao, DAO de texto)"]
    end
    subgraph TRANS["Transversales"]
        UTIL["util<br/>(Validator, InvoiceFormatter)"]
        EXC["exception<br/>(Validation, BusinessRule, DataAccess)"]
    end

    MAIN --> SERVICE
    MAIN --> RULE
    MAIN --> DAO
    MAIN --> MODEL
    MAIN --> UTIL
    SERVICE --> RULE
    RULE --> SERVICE
    SERVICE --> DAO
    SERVICE --> MODEL
    SERVICE --> UTIL
    SERVICE --> EXC
    RULE --> MODEL
    RULE --> EXC
    DAO --> MODEL
    DAO --> EXC
    MODEL --> EXC
    UTIL --> MODEL
    UTIL --> EXC
```

| Paquete | Contenido | Depende de |
|---|---|---|
| `model` | `Person`, `Customer`, `Professional`, `Sellable`, `Product`, `ProductCategory`, `Treatment`, `Appointment`, `AppointmentStatus`, `Sale`, `SaleLine`, `Setting` | `exception` |
| `dao` | `CrudDao<T>`, interfaces `XxxDao`, `TextFileDao<T>`, `TextFileStore`, `TextLineCodec` y las implementaciones `XxxDaoText` | `model`, `exception` |
| `service` | Un `XxxService` por módulo, DTO (`ContactData`, `ProductData`, `TreatmentData`, `WorkSchedule`, `CustomerStatistics`, `ProductSales`, `PromotionSuggestion`), `EmailNotifier`, `AdminNotifier` y sus versiones simuladas | `dao`, `model`, `service.rule`, `util`, `exception` |
| `service.rule` | `AppointmentRule`, `DepositRule`, `ConfirmationRule`, `ReminderRule` | `model`, `service` (`SettingsService`), `exception` |
| `util` | `Validator`, `InvoiceFormatter` | `model`, `exception` |
| `exception` | `ValidationException`, `BusinessRuleException`, `DataAccessException` | — |

> **Ciclo `service` ⇄ `service.rule`.** `AppointmentService` recibe la lista de `AppointmentRule` y las reglas leen `SettingsService`, que vive en `service`. Es un ciclo entre dos paquetes de la misma capa, sin ciclo entre clases: `SettingsService` no conoce las reglas. Mover `SettingsService` a un paquete propio lo eliminaría y puede hacerse en la Entrega 2.
>
> **Paquetes `ui` y `ai`.** No existen todavía: la interfaz JavaFX y la IA son de la Entrega 2 (o de S-12 y S-13 si entran antes).

---

## 2. Modelo de dominio

```mermaid
classDiagram
    class Person {
        <<abstract>>
        -int id
        -String document
        -String name
        -String phone
        -String email
        +getRoleDescription() String*
    }
    class Customer {
        -LocalDate registeredAt
        -int noShowCount
        +registerNoShow() void
    }
    class Professional {
        -String specialty
        -LocalTime workStart
        -LocalTime workEnd
        -boolean active
        +addTreatment(Treatment) void
        +removeTreatment(Treatment) void
        +offers(Treatment) boolean
        +worksDuring(LocalDateTime, LocalDateTime) boolean
        +activate() void
        +deactivate() void
    }
    class Sellable {
        <<interface>>
        +getId() int
        +getName() String
        +getPrice() double
        +hasStock(int) boolean
        +deductStock(int) void
        +addStock(int) void
    }
    class Product {
        -int stock
        -int minimumStock
        -boolean active
        +hasStock(int) boolean
        +deductStock(int) void
        +addStock(int) void
        +isBelowMinimum() boolean
    }
    class ProductCategory {
        -int id
        -String name
    }
    class Treatment {
        -String description
        -int durationMinutes
        -boolean active
        +calculateEnd(LocalDateTime) LocalDateTime
        +activate() void
        +deactivate() void
    }
    class Appointment {
        -LocalDateTime start
        -LocalDateTime end
        -LocalDateTime createdAt
        -double deposit
        -LocalDateTime reminderAt
        +overlapsWith(Appointment) boolean
        +rescheduledTo(LocalDateTime) Appointment
        +blocksSchedule() boolean
        +confirm() void
        +cancel() void
        +complete() void
        +markNoShow() void
    }
    class AppointmentStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        COMPLETED
        CANCELLED
        NO_SHOW
        +blocksSchedule() boolean
        +canChangeTo(AppointmentStatus) boolean
    }
    class Sale {
        -LocalDateTime soldAt
        +addLine(Sellable, int) SaleLine
        +calculateTotal() double
        +linkAppointment(Appointment) void
    }
    class SaleLine {
        -int quantity
        -double unitPrice
        +calculateSubtotal() double
        +sells(Sellable) boolean
    }
    class Setting {
        -String key
        -String value
        -String description
    }

    Person <|-- Customer
    Person <|-- Professional
    Sellable <|.. Product
    Sellable <|.. Treatment
    Product "*" --> "1" ProductCategory
    Professional "0..*" o-- "0..*" Treatment : offers
    Appointment "*" --> "1" Customer
    Appointment "*" --> "1" Professional
    Appointment "*" --> "1" Treatment
    Appointment --> AppointmentStatus
    Sale "1" *-- "1..*" SaleLine
    Sale "*" --> "1" Customer
    Sale "0..1" --> "0..1" Appointment : linkAppointment
    SaleLine "*" --> "1" Sellable
```

| Elemento | Dónde se ve en el código |
|---|---|
| Herencia | `Customer` y `Professional` extienden `Person` (método abstracto `getRoleDescription()`) |
| Polimorfismo por interfaz | `Sale.addLine(Sellable, int)` acepta `Product` o `Treatment`; el stock se descuenta con `Sellable.deductStock`, un método por defecto que `Product` sobrescribe |
| Composición | `Sale` crea y contiene sus `SaleLine` (`addLine`); una línea no existe sin su venta |
| Agregación N:M | `Professional` guarda sus `Treatment` (`professional_treatments.txt`) |
| Máquina de estados | `AppointmentStatus.canChangeTo` valida cada transición; las inválidas lanzan `BusinessRuleException` |
| Reconstrucción desde disco | `Appointment.restore`, `Sale.restore` y `SaleLine.restore` saltan las reglas de creación |

---

## 3. Acceso a datos (DAO)

Todas las interfaces extienden `CrudDao<T>`, salvo `SettingDao`, cuya clave es texto. Las implementaciones de texto heredan de `TextFileDao<T>` y solo definen `toFields` y `fromFields` ([ADR-0012](../adr/0012-persistencia-en-archivos-de-texto.md)).

```mermaid
classDiagram
    class CrudDao~T~ {
        <<interface>>
        +create(T) T
        +findById(int) Optional~T~
        +findAll() List~T~
        +update(T) void
        +delete(int) void
    }
    class CustomerDao {
        <<interface>>
        +findByDocument(String) Optional~Customer~
    }
    class ProfessionalDao {
        <<interface>>
        +findByDocument(String) Optional~Professional~
        +findByTreatment(int) List~Professional~
    }
    class TreatmentDao {
        <<interface>>
    }
    class CategoryDao {
        <<interface>>
    }
    class ProductDao {
        <<interface>>
        +findByName(String) Optional~Product~
    }
    class AppointmentDao {
        <<interface>>
        +findByProfessional(int) List~Appointment~
        +findByDate(LocalDate) List~Appointment~
    }
    class SaleDao {
        <<interface>>
        +findByCustomer(int) List~Sale~
        +findBetween(LocalDate, LocalDate) List~Sale~
    }
    class SettingDao {
        <<interface>>
        +loadAll() Map~String, Setting~
        +save(String, String) void
    }
    class TextFileDao~T~ {
        <<abstract>>
        #toFields(T) String[]*
        #fromFields(String[]) T*
        #findRelated(CrudDao, String, String) E
    }
    class TextFileStore {
        <<final>>
    }
    class TextLineCodec {
        <<final>>
    }

    CrudDao <|-- CustomerDao
    CrudDao <|-- ProfessionalDao
    CrudDao <|-- TreatmentDao
    CrudDao <|-- CategoryDao
    CrudDao <|-- ProductDao
    CrudDao <|-- AppointmentDao
    CrudDao <|-- SaleDao
    CrudDao <|.. TextFileDao
    TextFileDao <|-- CustomerDaoText
    TextFileDao <|-- ProfessionalDaoText
    TextFileDao <|-- TreatmentDaoText
    TextFileDao <|-- CategoryDaoText
    TextFileDao <|-- ProductDaoText
    TextFileDao <|-- AppointmentDaoText
    TextFileDao <|-- SaleDaoText
    TextFileDao <|-- SaleLineDaoText
    CustomerDao <|.. CustomerDaoText
    ProfessionalDao <|.. ProfessionalDaoText
    TreatmentDao <|.. TreatmentDaoText
    CategoryDao <|.. CategoryDaoText
    ProductDao <|.. ProductDaoText
    AppointmentDao <|.. AppointmentDaoText
    SaleDao <|.. SaleDaoText
    SettingDao <|.. SettingDaoText
    TextFileDao ..> TextFileStore : lee y escribe
    TextFileDao ..> TextLineCodec : separador y escape
    SettingDaoText ..> TextFileStore
    SaleDaoText --> SaleLineDaoText : líneas de la venta
```

| Archivo (`data/`) | DAO | Tabla de `schema.sql` |
|---|---|---|
| `customers.txt` | `CustomerDaoText` | `customer` |
| `professionals.txt`, `professional_treatments.txt` | `ProfessionalDaoText` | `professional`, `professional_treatment` |
| `treatments.txt` | `TreatmentDaoText` | `treatment` |
| `categories.txt` | `CategoryDaoText` | `product_category` |
| `products.txt` | `ProductDaoText` | `product` |
| `appointments.txt` | `AppointmentDaoText` | `appointment` |
| `sales.txt`, `sale_lines.txt` | `SaleDaoText`, `SaleLineDaoText` | `sale`, `sale_line` |
| `settings.txt` | `SettingDaoText` | `app_setting` |

---

## 4. Servicios, reglas y notificadores

Cada servicio recibe sus dependencias **por constructor** y solo `Main` arma las piezas (DIP). Las reglas de cita se agregan como clases nuevas sin tocar `AppointmentService` (OCP, [ADR-0007](../adr/0007-reglas-de-cita-como-estrategias.md)).

```mermaid
classDiagram
    class AppointmentRule {
        <<interface>>
        +apply(Appointment) void
        +getName() String
    }
    class DepositRule
    class ConfirmationRule
    class ReminderRule
    class EmailNotifier {
        <<interface>>
        +sendInvoice(Customer, Sale) void
    }
    class SimulatedEmailNotifier
    class AdminNotifier {
        <<interface>>
        +notifyLowRotation(List~ProductSales~, int) void
    }
    class SimulatedAdminNotifier
    class SettingsService {
        +depositPercentage() double
        +minBookingNoticeHours() int
        +reminderHoursBefore() int
        +noShowPenaltyThreshold() int
        +lowRotationWindowDays() int
        +lowRotationTopN() int
        +lowRotationDiscountPercentage() double
        +update(String, String) void
    }
    class CustomerService {
        +register(ContactData) Customer
        +searchByName(String) List~Customer~
        +registerNoShow(int) void
    }
    class TreatmentService {
        +register(TreatmentData) Treatment
        +activate(int) void
        +deactivate(int) void
    }
    class ProfessionalService {
        +register(ContactData, String, WorkSchedule) Professional
        +assignTreatment(int, int) void
        +listByTreatment(int) List~Professional~
    }
    class ProductService {
        +register(ProductData, int) Product
        +adjustStock(int, int) Product
        +listLowStock() List~Product~
    }
    class AppointmentService {
        +book(...) Appointment
        +reschedule(int, LocalDateTime) Appointment
        +confirm(int) void
        +cancel(int) void
        +complete(int) void
        +markNoShow(int) void
        +listByDate(LocalDate) List~Appointment~
        +checkAvailability(Professional, LocalDateTime, LocalDateTime) boolean
    }
    class SaleService {
        +register(Sale) Sale
        +listByCustomer(int) List~Sale~
    }
    class StatisticsService {
        +customerStatistics(int) CustomerStatistics
        +topProducts(int, LocalDate, LocalDate) Map
        +topTreatments(int) Map
        +frequentCustomers(int) Map
    }
    class LowRotationService {
        +lowRotationProducts(LocalDate) List~ProductSales~
        +notifyAdministrator(AdminNotifier, LocalDate) List~ProductSales~
    }
    class PromotionService {
        +suggestPromotions(int, LocalDate) List~PromotionSuggestion~
    }
    class InvoiceFormatter {
        <<util>>
    }

    AppointmentRule <|.. DepositRule
    AppointmentRule <|.. ConfirmationRule
    AppointmentRule <|.. ReminderRule
    EmailNotifier <|.. SimulatedEmailNotifier
    AdminNotifier <|.. SimulatedAdminNotifier
    SimulatedEmailNotifier ..> InvoiceFormatter

    DepositRule --> SettingsService
    ConfirmationRule --> SettingsService
    ReminderRule --> SettingsService
    AppointmentService o-- "0..*" AppointmentRule : aplica
    LowRotationService --> SettingsService
    PromotionService --> SettingsService
    PromotionService --> LowRotationService
    LowRotationService ..> AdminNotifier : recibe por método
```

Dependencias de datos de cada servicio (constructor):

| Servicio | Recibe |
|---|---|
| `CustomerService` | `CustomerDao` |
| `TreatmentService` | `TreatmentDao` |
| `ProfessionalService` | `ProfessionalDao`, `TreatmentDao` |
| `ProductService` | `ProductDao`, `CategoryDao` |
| `AppointmentService` | `AppointmentDao`, `CrudDao<Customer>`, `List<AppointmentRule>` |
| `SaleService` | `SaleDao`, `ProductDao` |
| `StatisticsService` | `SaleDao`, `AppointmentDao` |
| `LowRotationService` | `SaleDao`, `CrudDao<Product>`, `SettingsService` |
| `PromotionService` | `LowRotationService`, `SaleDao`, `SettingsService` |
| `SettingsService` | `SettingDao` |
| `DepositRule`, `ConfirmationRule`, `ReminderRule` | `SettingsService` |

---

## 5. Diferencias frente al diseño inicial

Cambios entre [`diseno-clases.md`](diseno-clases.md) y el código final:

| Diseño inicial | Código final | Motivo |
|---|---|---|
| `Venta.agregarDetalle`, `DetalleVenta` | `Sale.addLine`, `SaleLine` | Nomenclatura en inglés; mismo comportamiento (Creator) |
| `Vendible` solo con `getId`, `getNombre`, `getPrecio` | `Sellable` suma `hasStock`, `deductStock` y `addStock` por defecto | Descontar stock sin `instanceof` (polimorfismo) |
| `DetalleVenta.esProducto()` con `instanceof` | `SaleLine.sells(Sellable)` | Se eliminó la comprobación por tipo |
| `Cita.getFechaHoraFin()` calculado | `Appointment` guarda `end` y `createdAt` | Se persiste la fila completa de `appointment` |
| `Cita.seCruzaCon(Cita)` | `overlapsWith(Appointment)` y `overlapsWith(LocalDateTime, LocalDateTime)` | Permite consultar disponibilidad sin crear una cita |
| `Cita.confirmar()` valida el estado | `AppointmentStatus.canChangeTo` + `Appointment.rescheduledTo` | La máquina de estados vive en el enum |
| `EstadisticaServicio` con baja rotación | `StatisticsService`, `LowRotationService` y `PromotionService` | Una responsabilidad por clase (SRP) |
| `NotificadorCorreo` en `servicio` | `EmailNotifier` y `AdminNotifier` en `service` | Igual ubicación, nombres en inglés |
| `ConfiguracionServicio` | `SettingsService` + `Setting` + `SettingDao` | Nomenclatura en inglés |
| `TextFileDao` base | `TextFileDao<T>` + `TextFileStore` + `TextLineCodec` | Se separó la lectura y escritura del formato de línea |
| `ConexionBD` en `util` | No existe | La conexión SQLite es de la Entrega 2 |
| Paquetes `vista`, `vista.controlador`, `ia` | No existen todavía | Entrega 2 (la IA backend es opcional: S-12, S-13) |
