# Team

## Members

| Name | GitHub Username | Role | Modules | Feature Branches |
|---|---|---|---|---|
| Samuel David Rangel Martínez | samueldrangel | Technical Lead | Core, Appointments, Sales | `feature/core`, `feature/appointment-module`, `feature/sale-module` |
| Nicoll Gómez | _pending_ | Developer 1 | Customers, Professionals, Services, Statistics, Assistant | `feature/customer-module`, `feature/service-module`, `feature/statistics-module`, `feature/ai-assistant` |
| Isabella Celedón | _pending_ | Developer 2 | Products and Inventory, GUI, Virtual Try-On | `feature/product-module`, `feature/gui-base`, `feature/ai-try-on` |

## Class Distribution

### Technical Lead
- `CrudDao`, `ValidationException`, `BusinessRuleException`, `DataAccessException`, `Validator`
- `Appointment`, `AppointmentStatus`, `AppointmentDao`, `AppointmentService`
- `AppointmentRule` and its implementations (deposit, confirmation, reminder)
- `Sale`, `SaleLine`, `SaleDao`, `SaleService`, `EmailNotifier`
- `Main`

### Developer 1
- `Person` (abstract), `Customer`, `Professional`
- `CustomerService`, `ProfessionalService`, `BeautyServiceCatalog`
- `StatisticsService` (customer and administrator statistics, low-rotation products)
- AI assistant tools (`AssistantTool` and its implementations)

### Developer 2
- `Sellable`, `Product`, `BeautyService`
- `ProductService` (stock control and low-stock alerts)
- JavaFX views, controllers and stylesheet
- `TryOnService` (virtual try-on)

## Committed Activities

### Technical Lead
1. Create the repository, configure `main` and `develop`, and enable branch protection.
2. Configure the Maven project and the layered package structure.
3. Implement the shared foundation: DAO contract, exceptions, and input validation.
4. Design the database (ER model and `schema.sql`) together with the team.
5. Implement appointments with schedule-conflict control and configurable business rules.
6. Implement sales with stock deduction and invoice delivery by email.
7. Review and merge the Pull Requests of the other members.
8. Consolidate the documentation of each phase.

### Developer 1
1. Implement the `Person` hierarchy with `Customer` and `Professional`.
2. Implement the services for customers, professionals, and the service catalog.
3. Implement customer and administrator statistics and low-rotation detection.
4. Implement the AI assistant tools on top of the services.
5. Add Javadoc and unit tests to every class of the assigned modules.

### Developer 2
1. Implement `Sellable`, `Product`, and `BeautyService`.
2. Implement product management, stock control, and low-stock alerts.
3. Design the mockups and implement the JavaFX views and navigation.
4. Implement the virtual try-on module behind its interface.
5. Add Javadoc and unit tests to every class of the assigned modules.
