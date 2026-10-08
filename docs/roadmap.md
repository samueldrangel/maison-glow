# Roadmap de desarrollo — Entrega 1

> Plan de trabajo exacto por integrante para la **Entrega 1** (≈ 14 días): toda la lógica de negocio, con persistencia en archivos de texto,
> más avances de UI en mockups y la BD **diseñada** (no conectada). Contexto: [`../PLANIFICACION.md`](../PLANIFICACION.md) ·
> clases: [`fase2/diseno-clases.md`](fase2/diseno-clases.md) · TEAM: [`../TEAM.md`](../TEAM.md).

## 0. Tabla resumen (orden de ejecución)

> **Esta es la tabla que se consulta cada día.** Está en el orden exacto en que se inicia cada tarea. El detalle de cada una (descripción, rama y criterio de "listo") está en la sección 4.

| Orden | ID | Tarea | Responsable | Pri. | Inicia | Depende de | Estado |
|:--:|---|---|---|:--:|:--:|---|:--:|
| 1 | S-01 | Cerrar la base del repo (fusionar esqueleto, colaboradores, protección de ramas, contratos) | Samuel | 🔴 | Día 1 | — | ✅ Hecha |
| 2 | S-02 | Diseño de la BD (MER y `schema.sql`) | Samuel | 🔴 | Día 1 | — | ✅ Hecha |
| 3 | N-01 | `Person`, `Customer`, `Professional` | Nicoll | 🔴 | Día 1 | S-01 | ⬜ Pendiente |
| 4 | I-01 | `BeautyService` | Isabella | 🔴 | Día 1 | S-01 | ⬜ Pendiente |
| 5 | I-04 | Mockups base M1–M4 | Isabella | 🔴 | Día 1 | — | ⬜ Pendiente |
| 6 | S-03 | Base `TextFileDao<T>` (archivos de texto) | Samuel | 🔴 | Día 2 | S-01 | ⬜ Pendiente |
| 7 | I-02 | `Product` | Isabella | 🔴 | Día 2 | S-01 | ⬜ Pendiente |
| 8 | N-02 | Módulo de clientes (DAO + servicio) | Nicoll | 🔴 | Día 3 | N-01, S-03 | ⬜ Pendiente |
| 9 | I-03 | Módulo de productos (DAO + servicio) | Isabella | 🔴 | Día 3 | I-02, S-03 | ⬜ Pendiente |
| 10 | S-04 | `Appointment` y su DAO | Samuel | 🔴 | Día 3 | N-01, I-01, S-03 | ⬜ Pendiente |
| 11 | N-03 | Catálogo de servicios (DAO + servicio) | Nicoll | 🔴 | Día 4 | I-01, S-03 | ⬜ Pendiente |
| 12 | N-04 | Módulo de profesionales (DAO + servicio) | Nicoll | 🔴 | Día 5 | N-01, N-03 | ⬜ Pendiente |
| 13 | S-05 | `AppointmentService`: agendar y evitar cruces | Samuel | 🔴 | Día 5 | S-04 | ⬜ Pendiente |
| 14 | S-06 | `Sale` y `SaleLine` | Samuel | 🔴 | Día 5 | I-02 | ⬜ Pendiente |
| 15 | I-06 | Diagrama de paquetes y de clases final | Isabella | 🔴 | Día 7 | N-01, I-01, I-02, S-04, S-06 | ⬜ Pendiente |
| 16 | S-07 | `SaleService`: registrar venta y descontar stock | Samuel | 🔴 | Día 7 | S-06, I-03 | ⬜ Pendiente |
| 17 | I-05 | Mockups M5–M7 | Isabella | 🔴 | Día 8 | I-04 | ⬜ Pendiente |
| 18 | N-05 | `StatisticsService` (cliente y administrador) | Nicoll | 🟠 | Día 8 | S-05, S-07 *(o contrato)* | ⬜ Pendiente |
| 19 | S-08 | Reglas de cita (anticipo, confirmación, recordatorio) | Samuel | 🟠 | Día 9 | S-05 | ⬜ Pendiente |
| 20 | I-07 | `EmailNotifier` simulado y factura | Isabella | 🟠 | Día 9 | S-06 | ⬜ Pendiente |
| 21 | N-06 | Productos de baja rotación y aviso al administrador | Nicoll | 🟠 | Día 10 | N-05 | ⬜ Pendiente |
| 22 | S-09 | Pruebas de integración (escenario completo) | Samuel | 🟠 | Día 11 | S-07, N-05 | ⬜ Pendiente |
| 23 | N-07 | Sugerencia de promociones | Nicoll | 🟢 | Día 11 | N-06 | ⬜ Pendiente |
| 24 | N-09 | Documento GRASP y SOLID con clases reales | Nicoll | 🔴 | Día 11 | N-01…N-06 | ⬜ Pendiente |
| 25 | S-11 | Consolidar documentos de Fase 1 y 2 | Samuel | 🔴 | Día 11 | todas las 🔴 | ⬜ Pendiente |
| 26 | S-10 | Demo de consola (`Main`) | Samuel | 🟢 | Día 12 | S-09 | ⬜ Pendiente |
| 27 | I-08 | Mockups M8–M9 (IA) | Isabella | 🟢 | Día 12 | I-05 | ⬜ Pendiente |

**Estados:** ⬜ Pendiente · 🟨 En progreso · ✅ Hecha (fusionada en `develop`).

### Cómo usar la tabla

1. **¿Puedo empezar?** Sí, cuando **todas** las tareas de tu columna "Depende de" estén ✅ (es decir, su PR ya está fusionado en `develop`). Antes de eso, no abras tu rama.
2. **¿Cuándo?** La columna "Inicia" es el día **más temprano** previsto. Si la dependencia se fusiona antes, puedes empezar antes; si se atrasa, ver el punto 3.
3. **¿Y si la dependencia no está lista?** Trabaja contra el **contrato** de la sección 3 usando `Mockito` y avisa en el canal del equipo. No esperes parado ni copies código de una rama ajena.
4. **¿Cómo?** Rama y pasos en la sección 4; flujo de Git en la sección 5.
5. **Actualizar el estado:** el estado de tu tarea se cambia **en el mismo PR** de la tarea: 🟨 al abrir la rama (primer commit) y ✅ al fusionar. Tarea con varias dependencias: se revisa cada ✅ antes de empezar.
6. Las pruebas unitarias (N-08, I-09) no tienen fila propia: van **dentro** de cada tarea y son requisito para marcarla ✅.

## 1. Cómo leer este documento

- **Prioridad (MoSCoW):** 🔴 **Must** (sin esto no hay entrega) → 🟠 **Should** (muy deseable) → 🟢 **Could** (si hay tiempo). Cada persona hace **todos sus Must antes que cualquier Should**.
- **Día:** referencia dentro de los 14 días de la entrega (Día 1 = el día que se fusiona el esqueleto).
- **Depende de:** tarea que debe estar fusionada en `develop` (o su contrato acordado) antes de empezar.
- **Commits:** cada tarea genera 2 a 4 commits atómicos. Mínimo del equipo: **12 commits por integrante** y **6 PR** en total.
- Los nombres de clases están en inglés (ver mapa de nomenclatura en `fase2/diseno-clases.md`).

## 2. Ruta crítica

**Ruta crítica:** `Customer/Professional/BeautyService/Product` (Días 1–3) → `Appointment` y `Sale` (Samuel) → `Statistics` (Nicoll). Por eso los **modelos** de Nicoll e Isabella van primero: desbloquean a Samuel.

## 3. Contratos a congelar el Día 1

Para trabajar en paralelo sin esperar, estas firmas se acuerdan **antes de programar** (reunión de 30 min) y no cambian sin avisar:

| Clase | Constructor / métodos clave | Dueño |
|---|---|---|
| `Person` (abstract) | `getId()`, `getDocument()`, `getName()`, `getPhone()`, `getEmail()`, `getRoleDescription()` | Nicoll |
| `Customer` | `Customer(int id, String document, String name, String phone, String email)` · `registerNoShow()` | Nicoll |
| `Professional` | `Professional(int id, String document, String name, String phone, String email, String specialty, LocalTime workStart, LocalTime workEnd)` · `addService(BeautyService)` · `offers(BeautyService)` · `worksDuring(LocalDateTime, LocalDateTime)` | Nicoll |
| `BeautyService` implements `Sellable` | `BeautyService(int id, String name, String description, double price, int durationMinutes)` · `calculateEnd(LocalDateTime)` | Isabella |
| `Product` implements `Sellable` | `Product(int id, String name, String category, double price, int stock, int minimumStock)` · `hasStock(int)` · `deductStock(int)` · `addStock(int)` · `isBelowMinimum()` | Isabella |
| `Appointment` | `Appointment(int id, Customer, Professional, BeautyService, LocalDateTime start)` · `getEnd()` · `overlapsWith(Appointment)` · `confirm()` · `cancel()` | Samuel |
| `Sale` / `SaleLine` | `Sale(Customer)` · `addLine(Sellable, int)` · `calculateTotal()` · `getLines()` | Samuel |
| DAO por entidad | `CustomerDao`, `ProfessionalDao`, `BeautyServiceDao`, `ProductDao`, `AppointmentDao`, `SaleDao`: cada uno `extends CrudDao<T>`; además `AppointmentDao.findByProfessional(int)` y `findByDate(LocalDate)`; `SaleDao.findByCustomer(int)` y `findBetween(LocalDate, LocalDate)` | Quien lo implementa |
| Servicios | Un `*Service` por módulo; reciben sus DAO **por constructor** | Quien lo implementa |

## 4. Tareas por integrante

### 4.1 Samuel David Rangel — Technical Lead

| ID | Pri. | Día | Tarea | Depende de | Rama | Descripción y criterio de aceptación |
|---|---|---|---|---|---|---|
| S-01 | 🔴 | 1 | Cerrar la base del repo | — | `feature/project-skeleton` (existe) | Fusionar el PR del esqueleto a `develop`; agregar colaboradores y docente; proteger `main`; poner `develop` por defecto; reunión de contratos (sección 3). **Listo cuando** Nicoll e Isabella pueden clonar y compilar con `mvn clean verify`. |
| S-02 | 🔴 | 1–2 | Diseño de la BD | — | `docs/database-design` | MER (Mermaid en `docs/fase4/`) y `sql/schema.sql` con tablas `customer`, `professional`, `beauty_service`, `professional_service`, `product`, `appointment`, `sale`, `sale_line`. PK/FK, `CHECK` de precios y stock ≥ 0. Revisado por los tres. **Listo cuando** el script corre sin error en SQLite. |
| S-03 | 🔴 | 2 | Base `TextFileDao<T>` | S-01 | `feature/text-file-dao` | Clase abstracta genérica que implementa `CrudDao<T>` sobre un archivo `.txt` ([ADR-0012](adr/0012-persistencia-en-archivos-de-texto.md)): carga al construirse, mantiene un `Map<Integer,T>`, guarda tras cada cambio, asigna ids y usa `|` como separador con escape. Cada DAO solo define `toFields`, `fromFields` y el acceso al id; la ruta llega por constructor. Agrega `data/settings.txt` precargado (de `sql/seed.sql`) y las reglas de `.gitignore` para `data/*.txt`. **Listo cuando** hay pruebas con archivo temporal de crear, buscar, listar, actualizar, eliminar, recarga desde disco y escape de `|`. *Desbloquea todos los DAO.* |
| S-04 | 🔴 | 3–4 | `Appointment` y su DAO | N-01, I-01 | `feature/appointment-model` | `Appointment` con `getEnd()`, `overlapsWith()`, transiciones de estado y `AppointmentStatus.blocksSchedule()`; `AppointmentDao` + `AppointmentDaoText`. **Listo cuando** las transiciones inválidas lanzan `BusinessRuleException`. |
| S-05 | 🔴 | 5–7 | `AppointmentService.book` y gestión de citas | S-04 | `feature/appointment-service` | `book(...)`: valida datos (`Validator`), que el profesional ofrezca el servicio y trabaje en el horario y que **no haya cruce** con citas activas; `reschedule`, `cancel`, `confirm`, `listByDate`, `checkAvailability` (RF-10 a 13). **Listo cuando** una prueba intenta dos citas cruzadas y la segunda se rechaza. |
| S-06 | 🔴 | 5–6 | `Sale` y `SaleLine` | I-02 | `feature/sale-model` | `Sale.addLine(Sellable, qty)` **crea** el `SaleLine` (Creator), copia el precio del momento y suma cantidad si el ítem ya está; `calculateTotal()`. **Listo cuando** una venta mixta (producto + servicio) calcula bien el total. |
| S-07 | 🔴 | 7–9 | `SaleService.register` | S-06, I-03 | `feature/sale-service` | Valida que haya líneas y stock suficiente; **descuenta stock** de los productos (polimorfismo vía `Sellable`, sin `instanceof` de tipos de negocio — se usa un método del modelo); guarda la venta; `listByCustomer` (RF-14 a 17). **Listo cuando** vender más que el stock falla y no descuenta nada (todo o nada). |
| S-08 | 🟠 | 9–10 | Reglas de cita | S-05 | `feature/appointment-rules` | Interfaz `AppointmentRule` y `DepositRule`, `ConfirmationRule`, `ReminderRule` inyectadas en `AppointmentService` (ADR-0007) (RF-26). **Listo cuando** agregar una regla nueva no obliga a modificar `AppointmentService`. |
| S-09 | 🟠 | 11–12 | Pruebas de integración | S-07, N-05 | `feature/integration-tests` | Escenario completo con DAO de archivo de texto (sobre archivos temporales): registrar cliente, profesional, servicio y producto → agendar (y rechazar cruce) → vender → verificar stock y estadísticas. |
| S-10 | 🟢 | 12–13 | Demo de consola y cierre | S-09 | `feature/console-demo` | `Main` que ejecuta el escenario e imprime resultados (para mostrar la lógica sin UI). |
| S-11 | 🔴 | 11–14 | Consolidar documentos | todos | `docs/phase1-phase2` | Documento de Fase 1 (portada a bibliografía), actualizar `TEAM.md` y `docs/version-control.md` (commits y PR por integrante). |

### 4.2 Nicoll Gómez — Developer 1

| ID | Pri. | Día | Tarea | Depende de | Rama | Descripción y criterio de aceptación |
|---|---|---|---|---|---|---|
| N-01 | 🔴 | 1–2 | `Person`, `Customer`, `Professional` | S-01 | `feature/person-model` | `Person` abstracta con `getRoleDescription()`; `Customer` con `registerNoShow()`; `Professional` con lista de servicios (`ArrayList`), `offers()` y `worksDuring()`. `toString`/`equals`/`hashCode` por documento. **Listo cuando** hay pruebas de `worksDuring` (dentro, fuera y en el borde del horario). *Desbloquea a Samuel: fusionar el Día 2.* |
| N-02 | 🔴 | 3–4 | Módulo de clientes | N-01, S-03 | `feature/customer-service` | `CustomerDao` + `CustomerDaoText` + `CustomerService`: registrar (documento único, correo y teléfono válidos), buscar por id y por nombre, actualizar, eliminar (RF-01, 02). **Listo cuando** registrar un documento repetido lanza `BusinessRuleException`. |
| N-03 | 🔴 | 4–5 | Catálogo de servicios | I-01, S-03 | `feature/beauty-service-catalog` | `BeautyServiceDao` + `...Text` + `BeautyServiceCatalog`: registrar (precio > 0, duración > 0), actualizar, activar/desactivar, listar (RF-06, 07). |
| N-04 | 🔴 | 5–6 | Módulo de profesionales | N-01, N-03 | `feature/professional-service` | `ProfessionalDao` + `...Text` + `ProfessionalService`: registrar, `assignService`, `listByService` (RF-08, 09). **Listo cuando** asignar un servicio inactivo se rechaza. |
| N-05 | 🟠 | 8–10 | Estadísticas | S-07 (contrato), S-05 | `feature/statistics-service` | `StatisticsService` sobre `SaleDao` y `AppointmentDao`: `customerStatistics(id)` (compras y citas, RF-18), `topProducts(n, from, to)`, `topServices(n)` y `frequentCustomers(n)` (RF-28), devolviendo `Map`/`List`. Se puede empezar con `Mockito` mientras llegan los DAO reales. |
| N-06 | 🟠 | 10–11 | Baja rotación | N-05 | `feature/low-rotation` | `lowRotationProducts(n, from, to)` (RF-29) y `AdminNotifier` (interfaz) con implementación simulada que lista los productos de baja rotación (RF-30). |
| N-07 | 🟢 | 11–12 | Sugerencia de promociones | N-06 | `feature/promotion-suggestion` | `suggestPromotions(customerId)`: productos de baja rotación que el cliente aún no compró (RF-31). |
| N-08 | 🔴 | 2–14 | Pruebas de sus clases | cada tarea | en su propia rama | JUnit 5 + AssertJ (+ Mockito en `StatisticsService`); cobertura ≥ 70 % en `service`. |
| N-09 | 🔴 | 11–13 | Documento GRASP y SOLID | N-01…N-06 | `docs/grasp-solid` | Revisar y completar las secciones 7 y 8 de `diseno-clases.md` con las clases reales implementadas (clase y método concretos). |

### 4.3 Isabella Celedón — Developer 2

| ID | Pri. | Día | Tarea | Depende de | Rama | Descripción y criterio de aceptación |
|---|---|---|---|---|---|---|
| I-01 | 🔴 | 1–2 | `BeautyService` | S-01 | `feature/beauty-service-model` | Implementa `Sellable`; `calculateEnd(start)` suma la duración. `toString`/`equals`/`hashCode` por id. *Desbloquea a Samuel (Appointment) y a Nicoll (catálogo): fusionar el Día 2.* |
| I-02 | 🔴 | 2–3 | `Product` | S-01 | `feature/product-model` | Implementa `Sellable`; `hasStock`, `deductStock` (lanza `BusinessRuleException` si no alcanza), `addStock`, `isBelowMinimum()`. **Listo cuando** hay pruebas de descuento exacto, descuento excesivo y umbral. |
| I-03 | 🔴 | 3–5 | Módulo de productos | I-02, S-03 | `feature/product-service` | `ProductDao` + `ProductDaoText` + `ProductService`: registrar (precio > 0, stock ≥ 0), actualizar, `adjustStock`, `listLowStock()` (RF-03, 04, 05, 19). |
| I-04 | 🔴 | 1–8 | Mockups base M1–M4 | — | `docs/mockups-core` | En Figma o draw.io, exportados a `docs/fase2/mockups/`: M1 principal y navegación, M2 clientes, M3 profesionales/servicios, M4 productos. Cada uno con componentes y eventos anotados. |
| I-05 | 🔴 | 8–11 | Mockups M5–M7 | I-04 | `docs/mockups-operations` | M5 agenda de citas, M6 nueva venta (carrito), M7 estadísticas. |
| I-06 | 🔴 | 7–12 | Diagrama de paquetes y de clases | N-01, I-01, I-02, S-04, S-06 | `docs/diagrams` | Diagrama de paquetes y de clases **final** (draw.io o Mermaid) basado en el código real, guardado en `docs/fase2/`. |
| I-07 | 🟠 | 9–10 | Factura y correo simulado | S-06 | `feature/email-notifier` | `EmailNotifier` (interfaz) y `SimulatedEmailNotifier` (imprime el correo); `InvoiceFormatter` que arma el texto de la factura desde una `Sale` (RF-27). |
| I-08 | 🟢 | 12–14 | Mockups M8–M9 | I-05 | `docs/mockups-ai` | Probador virtual y asistente (para la Entrega 2). |
| I-09 | 🔴 | 2–14 | Pruebas de sus clases | cada tarea | en su propia rama | JUnit 5 + AssertJ; cobertura ≥ 70 % en `service`. |

## 5. Flujo de Git de cada integrante

El mismo flujo para los tres, **una rama por tarea** (PR pequeños):

```bash
# 1. Antes de cada tarea
git switch develop
git pull origin develop
git switch -c feature/<nombre-de-la-tarea>   # nombre de la columna "Rama"

# 2. Trabajo: commits atómicos y empujados enseguida
git add <archivos>
git commit -m "feat(product): add Product with stock rules"
git push -u origin feature/<nombre-de-la-tarea>   # el primero con -u; luego git push

# 3. Antes de abrir el PR
git fetch origin
git rebase origin/develop        # solo en TU rama, nunca en develop/main
mvn clean verify                 # debe pasar

# 4. Pull Request a develop → revisión → merge normal → borrar la rama
```

**Reglas**
1. **Nunca** commits directos a `develop` ni `main`; **nunca** `push --force` en ramas compartidas.
2. Un PR = una tarea. Título en formato Conventional Commits (`feat(product): add product service`).
3. **Revisor:** Samuel revisa los PR de Nicoll e Isabella; **Nicoll o Isabella** (una de las dos, alternando) revisan los de Samuel. Ningún PR se fusiona sin una aprobación ajena al autor.
4. **Merge normal** (no *squash*) para conservar los commits individuales (evidencia de cada integrante).
5. Borrar la rama remota tras el merge; el siguiente trabajo empieza otra vez desde `develop` actualizado.
6. Mensajes de commit: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, con alcance: `feat(customer): add duplicate document check`.
7. Si dos personas necesitan el mismo archivo (p. ej. `Main`), se avisa antes de editar.

**Ejemplo de secuencia de commits de una tarea (I-02 `Product`):**
`feat(model): add Product attributes and constructor` → `feat(model): add stock deduction rules to Product` → `test(model): add ProductTest` → `docs(model): add Javadoc to Product`.

## 6. Hitos y sincronización

| Día | Hito | Qué se revisa |
|---|---|---|
| 1 | Reunión de contratos (30 min) | Sección 3 firmada por los tres |
| 2–3 | **Modelos base fusionados** (N-01, I-01, S-03 el Día 2; I-02 el Día 3) | Samuel puede empezar `Appointment` (Día 3) y `Sale` (Día 5) |
| 3 y 7 | Sync de 20 min | Bloqueos, cambios de contrato, reasignar si alguien se atrasa |
| 10 | **Congelamiento** de código Must + Should | Solo se corrige y prueba; Could queda para después |
| 12 | Integración | S-09 corre completa sobre `develop` |
| 13 | Documentos | Fase 1, diagramas, GRASP/SOLID, `version-control.md` |
| 14 | **Entrega** | `develop` → `main` por PR, con tag `v0.1-entrega1` |

**Regla de desbloqueo:** si una tarea de la que depende otra no está lista en su día, el dependiente trabaja contra el **contrato** de la sección 3 usando `Mockito`, y se integra cuando llegue la real.

## 7. Definición de "Terminado" de una tarea

- [ ] Código con Javadoc en clases y métodos públicos; sin comentarios dentro de los métodos; nombres en inglés.
- [ ] Validación de entrada con `Validator` y excepciones propias (nunca `catch` vacío).
- [ ] Pruebas JUnit 5 + AssertJ del comportamiento principal y de los errores.
- [ ] `mvn clean verify` en verde.
- [ ] PR aprobado y fusionado; rama borrada; fila actualizada en la tabla de tareas.

## 8. Criterio de aceptación de la Entrega 1

1. `mvn clean verify` pasa en `main`, con ≥ 70 % de cobertura en `service`.
2. El escenario de S-09 demuestra: clientes, profesionales, servicios y productos registrados; cita agendada y cruce rechazado; venta mixta que descuenta stock; stock bajo y estadísticas consultables.
3. Mockups M1–M7 exportados y enlazados.
4. `sql/schema.sql` y MER revisados por los tres.
5. Documentos de Fase 1 y 2 completos (incluye GRASP y SOLID con clase y método reales).
6. Cada integrante con ≥ 12 commits y todos los cambios por PR.

## 9. Fuera de la Entrega 1 (Entrega 2)

Conexión SQLite y DAO JDBC, vistas JavaFX conectadas, asistente inteligente (RF-22/23), probador virtual (RF-21) y correo real. Ver `PLANIFICACION.md`, sección 4.3.
