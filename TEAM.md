# Team

## Members

| Name | GitHub Username | Role | Modules | Feature Branches |
|---|---|---|---|---|
| Samuel David Rangel Martínez | samueldrangel | Technical Lead | Core, Appointments, Sales, Artificial Intelligence (assistant and virtual try-on), UI mockups, final diagrams and documentation | `feature/project-skeleton`, `feature/text-file-dao`, `feature/appointment-model`, `feature/appointment-service`, `feature/sale-model`, `feature/sale-service`, `feature/ai-assistant`, `feature/ai-try-on` |
| Nicoll Gómez | _pending_ | Developer 1 | Customers, Professionals, Treatments, Statistics, Low rotation and promotions, Integration and demo | `feature/person-model`, `feature/customer-service`, `feature/treatment-catalog`, `feature/professional-service`, `feature/statistics-service`, `feature/low-rotation`, `feature/promotion-suggestion`, `feature/integration-tests`, `feature/console-demo` |
| Isabella Celedón | iceledon | Developer 2 | Products and Inventory, Business Settings, Appointment Rules, Invoice | `feature/beauty-service-model`, `feature/product-model`, `feature/settings-service`, `feature/product-service`, `feature/appointment-rules`, `feature/email-notifier` |

Delivery 2 (not in this delivery): Developer 1 and Developer 2 take the JavaFX views, the JDBC DAOs and the real email ([roadmap, section 9](docs/roadmap.md#9-fuera-de-la-entrega-1-entrega-2)).

## Class Distribution

Classes of `src/main/java/com/maisonglow` grouped by the member who wrote them (author of the commit that added each file). Details and diagrams: [`docs/fase2/diagramas.md`](docs/fase2/diagramas.md).

### Technical Lead
- Core: `CrudDao`, `TextFileDao`, `TextFileStore`, `TextLineCodec`, `ValidationException`, `BusinessRuleException`, `DataAccessException`, `Validator`
- `Sellable` (contract shared by `Product` and `Treatment`)
- Appointments: `Appointment`, `AppointmentStatus`, `AppointmentDao`, `AppointmentDaoText`, `AppointmentService`, `AppointmentRule` (interface)
- Sales: `Sale`, `SaleLine`, `SaleDao`, `SaleDaoText`, `SaleLineDaoText`, `SaleService`
- `Main` skeleton (completed by Developer 1, see below)
- AI module (package `ai`, optional in delivery 1): `SmartAssistant`, `AssistantTool` and its implementations, `ToolRegistry`, `TryOnService` and their simulated implementations

### Developer 1
- `Person` (abstract), `Customer`, `Professional`
- `CustomerDao`, `CustomerDaoText`, `CustomerService`, `ContactData`
- `TreatmentDao`, `TreatmentDaoText`, `TreatmentService`, `TreatmentData`
- `ProfessionalDao`, `ProfessionalDaoText`, `ProfessionalService`, `WorkSchedule`
- `StatisticsService`, `CustomerStatistics`
- `LowRotationService`, `ProductSales`, `AdminNotifier`, `SimulatedAdminNotifier`
- `PromotionService`, `PromotionSuggestion`
- Integration tests and the console demo (`Main`)

### Developer 2
- `Product`, `ProductCategory`, `Treatment`
- `CategoryDao`, `CategoryDaoText`, `ProductDao`, `ProductDaoText`, `ProductService`, `ProductData`
- `Setting`, `SettingDao`, `SettingDaoText`, `SettingsService`
- `DepositRule`, `ConfirmationRule`, `ReminderRule` (implementations of `AppointmentRule`)
- `EmailNotifier`, `SimulatedEmailNotifier`, `InvoiceFormatter`

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
9. Reassigned on 2026-10-28 (see below): UI mockups, final package and class diagrams, and the GRASP and SOLID document.

### Developer 1
1. Implement the `Person` hierarchy with `Customer` and `Professional`.
2. Implement the services for customers, professionals, and the treatment catalog.
3. Implement customer and administrator statistics, low-rotation detection, and promotion suggestions.
4. Write the integration tests and the console demo.
5. Add Javadoc and unit tests to every class of the assigned modules.

### Developer 2
1. Implement `Product`, `Treatment` (both implement `Sellable`) and `ProductCategory`.
2. Implement product management, stock control, and low-stock alerts.
3. Implement the settings service and the appointment rules (deposit, confirmation, reminder).
4. Implement the simulated email notifier and the invoice text.
5. Add Javadoc and unit tests to every class of the assigned modules.

## Reassignments

| Date | Task | From | To | Reason |
|---|---|---|---|---|
| 2026-10-28 | I-04, I-05, I-08: UI mockups M1–M9 | Developer 2 | Technical Lead | Speed up the closing of delivery 1; mockups were produced with an image-generation tool from `mockup_base.jpeg` |
| 2026-10-28 | I-06: final package and class diagrams | Developer 2 | Technical Lead | Same |
| 2026-10-28 | N-09: GRASP and SOLID document | Developer 1 | Technical Lead | Same |

The unit tests of each member (N-08, I-09) and all tasks already merged keep their original owner. The review exception that goes with this change is in [`docs/roadmap.md`](docs/roadmap.md#5-flujo-de-git-de-cada-integrante), rule 3. Commits and Pull Requests per member: [`docs/version-control.md`](docs/version-control.md).
