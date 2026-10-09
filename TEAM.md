# Team

## Members

| Name | GitHub Username | Role | Modules | Feature Branches |
|---|---|---|---|---|
| Samuel David Rangel Martínez | samueldrangel | Technical Lead | Core, Appointments, Sales, Artificial Intelligence (assistant and virtual try-on) | `feature/core`, `feature/appointment-module`, `feature/sale-module`, `feature/ai-assistant`, `feature/ai-try-on` |
| Nicoll Gómez | _pending_ | Developer 1 | Customers, Professionals, Services, Statistics, Integration and demo | `feature/customer-module`, `feature/service-module`, `feature/statistics-module`, `feature/integration-tests`, `feature/console-demo` |
| Isabella Celedón | _pending_ | Developer 2 | Products and Inventory, Business Settings, Appointment Rules, GUI | `feature/product-module`, `feature/settings-service`, `feature/appointment-rules`, `feature/gui-base` |

## Class Distribution

### Technical Lead
- `CrudDao`, `TextFileDao`, `ValidationException`, `BusinessRuleException`, `DataAccessException`, `Validator`
- `Appointment`, `AppointmentStatus`, `AppointmentDao`, `AppointmentService`, `AppointmentRule` (interface)
- `Sale`, `SaleLine`, `SaleDao`, `SaleService`
- AI module (package `ai`): `SmartAssistant`, `AssistantTool` and its implementations, `ToolRegistry`, `TryOnService` and their simulated implementations

### Developer 1
- `Person` (abstract), `Customer`, `Professional`
- `CustomerService`, `ProfessionalService`, `BeautyServiceCatalog`
- `StatisticsService` (customer and administrator statistics, low-rotation products), `AdminNotifier`
- Integration tests and `Main` (console demo)

### Developer 2
- `Sellable`, `Product`, `ProductCategory`, `BeautyService`
- `ProductService` (stock control and low-stock alerts)
- `Setting`, `SettingDao`, `SettingsService` (business parameters read by the rules and the statistics)
- `DepositRule`, `ConfirmationRule`, `ReminderRule` (implementations of `AppointmentRule`)
- `EmailNotifier`, `SimulatedEmailNotifier`, `InvoiceFormatter`
- JavaFX views, controllers and stylesheet

## Committed Activities

### Technical Lead
1. Create the repository, configure `main` and `develop`, and enable branch protection.
2. Configure the Maven project and the layered package structure.
3. Implement the shared foundation: DAO contract, text-file DAO base, exceptions, and input validation.
4. Design the database (ER model and `schema.sql`) together with the team.
5. Implement appointments with schedule-conflict control, and sales with stock deduction.
6. Implement the backend of the AI module (assistant with whitelisted tools and virtual try-on) behind interfaces, with simulated implementations; real API adapters in delivery 2.
7. Review and merge the Pull Requests of the other members.
8. Consolidate the documentation of each phase.

### Developer 1
1. Implement the `Person` hierarchy with `Customer` and `Professional`.
2. Implement the services for customers, professionals, and the service catalog.
3. Implement customer and administrator statistics and low-rotation detection.
4. Write the integration tests and the console demo.
5. Add Javadoc and unit tests to every class of the assigned modules.

### Developer 2
1. Implement `Sellable`, `Product`, `ProductCategory`, and `BeautyService`.
2. Implement product management, stock control, and low-stock alerts.
3. Implement the settings service and the appointment rules (deposit, confirmation, reminder).
4. Implement the simulated email notifier and the invoice text.
5. Design the mockups and implement the JavaFX views and navigation.
6. Add Javadoc and unit tests to every class of the assigned modules.
