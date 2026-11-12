# Roadmap de desarrollo — Corte 3 (IA + interfaz JavaFX)

> Plan de trabajo por integrante para el **Corte 3** (≈ 14 días): **interfaz gráfica JavaFX** conectada a los servicios existentes
> y **módulos de inteligencia artificial** (asistente con herramientas y probador virtual), simulados y por API.
> Contexto: [`../PLANIFICACION.md`](../PLANIFICACION.md) · roadmap anterior: [`roadmap.md`](roadmap.md) · UI: [ADR-0009](adr/0009-ui-con-javafx.md) ·
> IA: [ADR-0005](adr/0005-ia-desacoplada-por-interfaces.md), [ADR-0006](adr/0006-herramientas-del-asistente-tipo-mcp.md) · mockups: [`fase2/mockups/`](fase2/mockups/).

## 0. Reparto y orden del trabajo

| Integrante | Qué hace en este corte | Cómo se ordena |
|---|---|---|
| **Isabella Celedón** | **Solo frontend:** base JavaFX, ventana principal, estilos, componentes comunes, Productos, Configuración y Ventas | **Bloque 1:** sus 6 tareas **seguidas**, una tras otra (Días 1–7) |
| **Nicoll Gómez** | **Solo frontend:** Clientes, Profesionales y Tratamientos, Agenda, Estadísticas y las vistas de IA | **Bloque 2:** sus 5 tareas **seguidas**, al terminar el bloque de Isabella (Días 8–14) |
| **Samuel David Rangel** | **Solo backend de IA** (y la integración final) | **En paralelo**, a su ritmo, durante todo el corte. Única fecha fija: S-12, S-13 y S-14 fusionadas antes de que Nicoll abra N-16 |

**Por qué este orden:** Isabella construye primero la base sobre la que se montan todas las vistas (arranque, navegación, estilos y componentes). Cuando Nicoll empieza, todo eso ya está en `develop` y sus vistas solo reutilizan piezas existentes. Como cada una trabaja en su bloque, nunca editan los mismos `.fxml` a la vez (ADR-0009). Samuel no bloquea a nadie salvo en N-16, por eso puede avanzar cuando quiera.

## 1. Tabla resumen (orden de ejecución)

> **Esta es la tabla que se consulta cada día.** El detalle de cada tarea (rama, descripción y criterio de "listo") está en la sección 4.

| Orden | ID | Tarea | Responsable | Pri. | Inicia | Depende de | Estado |
|:--:|---|---|---|:--:|:--:|---|:--:|
| 1 | I-12 | Arranque JavaFX y `AppContext` (armado de servicios) | Isabella | 🔴 | Día 1 | — | ⬜ Pendiente |
| 2 | I-13 | Ventana principal, navegación y `styles.css` (M1) | Isabella | 🔴 | Día 2 | I-12 | ⬜ Pendiente |
| 3 | I-14 | Componentes comunes de UI (alertas, validación, formatos) | Isabella | 🔴 | Día 3 | I-13 | ⬜ Pendiente |
| 4 | I-15 | Vista de Productos e Inventario (M4) | Isabella | 🔴 | Día 4 | I-14 | ⬜ Pendiente |
| 5 | I-16 | Vista de Configuración del negocio | Isabella | 🟠 | Día 5 | I-14 | ⬜ Pendiente |
| 6 | I-17 | Vista de Nueva venta con carrito y factura (M6) | Isabella | 🔴 | Día 6 | I-15 | ⬜ Pendiente |
| 7 | N-12 | Vista de Clientes y sus estadísticas (M2) | Nicoll | 🔴 | Día 8 | I-17 (fin del bloque 1) | ⬜ Pendiente |
| 8 | N-13 | Vista de Profesionales y Tratamientos (M3) | Nicoll | 🔴 | Día 9 | N-12 | ⬜ Pendiente |
| 9 | N-14 | Vista de Agenda de citas (M5) | Nicoll | 🔴 | Día 10 | N-13 | ⬜ Pendiente |
| 10 | N-15 | Vista de Estadísticas y BI (M7) | Nicoll | 🟠 | Día 12 | N-14 | ⬜ Pendiente |
| 11 | N-16 | Vistas de IA: Asistente (M9) y Probador virtual (M8) | Nicoll | 🟢 | Día 13 | N-15, S-12, S-13, S-14 | ⬜ Pendiente |
| — | S-12 | **IA:** asistente con herramientas (simulado) | Samuel | 🔴 | libre | — | ⬜ Pendiente |
| — | S-13 | **IA:** probador virtual (simulado) | Samuel | 🔴 | libre | — | ⬜ Pendiente |
| — | S-14 | **IA:** `AiConfig` y fábrica de proveedores (simulado/API) | Samuel | 🔴 | libre | S-12, S-13 | ⬜ Pendiente |
| — | S-15 | **IA:** adaptador por API del asistente | Samuel | 🟠 | libre | S-14 | ⬜ Pendiente |
| — | S-16 | **IA:** adaptador por API del probador virtual | Samuel | 🟢 | libre | S-14 | ⬜ Pendiente |
| — | S-17 | **IA:** persistencia de conversaciones y sesiones de prueba | Samuel | 🟢 | libre | S-12, S-13 | ⬜ Pendiente |
| — | S-18 | Integración final y entrega del corte | Samuel | 🔴 | Día 14 | todas las 🔴 | ⬜ Pendiente |

**Estados:** ⬜ Pendiente · 🟨 En progreso · ✅ Hecha (fusionada en `develop`).

### Cómo usar la tabla

1. **Isabella** sigue el orden 1 → 6 sin saltar: no abre una tarea hasta que la anterior esté ✅.
2. **Nicoll** empieza su bloque cuando I-17 esté ✅ y sigue el orden 7 → 11. Mientras espera, revisa los PR de Isabella y prepara sus vistas en Scene Builder **sin abrir rama**.
3. **Samuel** toma sus tareas (filas "—") en el orden que quiera, respetando la columna "Depende de". Lo único con fecha es tener S-12, S-13 y S-14 ✅ **antes del Día 13** (desbloquean N-16).
4. Si una dependencia se atrasa, se trabaja contra el **contrato** de la sección 3 y se avisa en el canal del equipo.
5. El estado se actualiza **en el mismo PR** de la tarea: 🟨 al primer commit y ✅ al fusionar.

## 2. Ruta crítica

`I-12 → I-13 → I-14 → I-15 → I-17` (base y Ventas) `→ N-12 → N-13 → N-14` (Clientes, Profesionales y Agenda). Sin estas tareas no hay aplicación navegable: son todas 🔴.

**La IA va en paralelo y no bloquea la entrega** (RNF-13). S-12/S-13/S-14 son 🔴 para Samuel porque son el entregable de IA del corte, pero la app debe funcionar aunque falten S-15, S-16 y S-17. Si N-16 no está lista el Día 14, el menú de IA muestra "Próximamente" y se documenta.

## 3. Contratos a congelar el Día 1

Se acuerdan en una reunión de 30 min y no cambian sin avisar.

| Pieza | Contrato | Dueño |
|---|---|---|
| `MaisonGlowApp` | `extends javafx.application.Application`; en `start(Stage)` crea `AppContext` y abre `MainView`. Pasa a ser el `mainClass` de `javafx-maven-plugin` (`mvn javafx:run`). La demo de consola `Main` sigue con `exec-maven-plugin` | Isabella |
| `AppContext` | Único lugar donde se crean DAO, servicios, reglas y notificadores (DIP). Getters: `customers()`, `professionals()`, `treatments()`, `products()`, `appointments()`, `sales()`, `settings()`, `statistics()`, `lowRotation()`, `promotions()`, `ai()` | Isabella (Samuel agrega `ai()` en S-14) |
| `Navigator` | `show(View view)`; enum `View` con una constante por pantalla (`CUSTOMERS`, `PROFESSIONALS`, `APPOINTMENTS`, `PRODUCTS`, `SALES`, `STATISTICS`, `SETTINGS`, `ASSISTANT`, `TRY_ON`) y la ruta de su `.fxml` | Isabella |
| Controladores | Paquete `com.maisonglow.view.controller`, uno por `.fxml`; reciben `AppContext` por constructor mediante `FXMLLoader.setControllerFactory`. **Solo llaman a servicios**, nunca a DAO | Quien hace la vista |
| Recursos | `src/main/resources/com/maisonglow/view/*.fxml` y `.../view/styles.css` | Quien hace la vista |
| `AlertPresenter` | `showError(Exception)`, `showInfo(String)`, `confirm(String): boolean`. Traduce `ValidationException`, `BusinessRuleException` y `DataAccessException` a mensajes claros | Isabella |
| `AiModule` | Lo devuelve `AppContext.ai()`: `assistant(): SmartAssistant`, `tryOn(): TryOnService`, `isSimulated(): boolean` | Samuel |
| `SmartAssistant` | `AssistantReply reply(String message)`; `AssistantReply`: `text()`, `pendingConfirmation(): Optional<PendingAction>`; `confirm(PendingAction)` y `reject(PendingAction)` | Samuel |
| `TryOnService` | `TryOnResult generate(TryOnRequest)`; `TryOnRequest(int customerId, Path photo, String transformation)`; `TryOnResult`: `image()`, `disclaimer()` ("simulación, no garantía") | Samuel |

## 4. Tareas por integrante

### 4.1 Isabella Celedón — Bloque 1 (frontend base y módulos de Productos y Ventas)

| ID | Pri. | Día | Tarea | Depende de | Rama | Descripción y criterio de aceptación |
|---|---|---|---|---|---|---|
| I-12 | 🔴 | 1 | Arranque JavaFX y `AppContext` | — | `feature/javafx-bootstrap` | `MaisonGlowApp` y `AppContext` (contrato sección 3): mueve el armado de DAO y servicios que hoy está en `Main` a `AppContext` y hace que `Main` lo reutilice. Configura `javafx-maven-plugin` con `MaisonGlowApp`. **Listo cuando** `mvn javafx:run` abre una ventana vacía con el título "Maison Glow", `mvn exec:java` sigue corriendo la demo de consola y hay una prueba de que `AppContext` arma todos los servicios. |
| I-13 | 🔴 | 2 | Ventana principal, navegación y estilos (M1) | I-12 | `feature/main-window` | `MainView.fxml` (barra lateral + área central), `Navigator` y enum `View`; `styles.css` con la paleta Maison Glow (variables de color, tipografía, botones, tablas). Cada opción del menú carga su vista en el área central (las que aún no existen muestran un marcador "En construcción"). **Listo cuando** se navega por todas las opciones sin errores y el diseño coincide con el mockup M1. |
| I-14 | 🔴 | 3 | Componentes comunes de UI | I-13 | `feature/ui-common` | `AlertPresenter` (contrato), `FieldValidator` (marca en rojo el campo inválido y muestra el mensaje de la excepción), `CurrencyFormatter` (pesos `es-CO`) y `DateTimeFormatter` comunes, y una plantilla base de formulario + tabla reutilizable. Pruebas unitarias de los formateadores. **Listo cuando** cualquier vista puede mostrar un error de servicio con una línea de código y no hay formato de moneda/fecha repetido. |
| I-15 | 🔴 | 4–5 | Vista de Productos e Inventario (M4) | I-14 | `feature/products-view` | `ProductsView.fxml` + `ProductsController`: tabla de productos (nombre, categoría, precio, stock, mínimo), formulario registrar/editar, `ComboBox` de categorías, ajuste de stock y filtro "solo stock bajo" con las filas bajo el mínimo resaltadas (RF-03, 04, 05, 19). **Listo cuando** registrar un nombre repetido o un precio ≤ 0 muestra el error en el formulario y la tabla se actualiza sin reiniciar. |
| I-16 | 🟠 | 5 | Vista de Configuración del negocio | I-14 | `feature/settings-view` | `SettingsView.fxml` + `SettingsController`: edita los parámetros de `SettingsService` (anticipo %, aviso mínimo, recordatorio, umbral de inasistencias, ventana y top de baja rotación, descuento). **Listo cuando** un valor fuera de rango se rechaza con mensaje y un valor guardado se refleja en `data/settings.txt` sin tocar código. |
| I-17 | 🔴 | 6–7 | Vista de Nueva venta con carrito y factura (M6) | I-15 | `feature/sales-view` | `SalesView.fxml` + `SalesController`: elegir cliente, agregar productos y tratamientos al carrito (cantidad, subtotal, total en vivo), quitar líneas, registrar con `SaleService` y mostrar la factura de `InvoiceFormatter` en un diálogo con "Enviar por correo" (`SimulatedEmailNotifier`) (RF-14 a 17, 27). **Listo cuando** vender más que el stock muestra el error y no cambia nada, y una venta válida descuenta stock visible en la vista de Productos. |

### 4.2 Nicoll Gómez — Bloque 2 (módulos de Clientes, Agenda, Estadísticas e IA)

> Empieza cuando I-17 está ✅. Reutiliza `Navigator`, `AlertPresenter`, los formateadores y `styles.css` de Isabella: **no** crea estilos ni utilidades paralelas.

| ID | Pri. | Día | Tarea | Depende de | Rama | Descripción y criterio de aceptación |
|---|---|---|---|---|---|---|
| N-12 | 🔴 | 8 | Vista de Clientes y sus estadísticas (M2) | I-17 | `feature/customers-view` | `CustomersView.fxml` + `CustomersController`: tabla, búsqueda por nombre, registrar/editar/eliminar y panel lateral con `customerStatistics` del cliente seleccionado (compras, citas, inasistencias) (RF-01, 02, 18). **Listo cuando** un documento repetido o un correo inválido muestra el error en el campo correspondiente. |
| N-13 | 🔴 | 9 | Vista de Profesionales y Tratamientos (M3) | N-12 | `feature/professionals-view` | `ProfessionalsView.fxml` con dos pestañas: **Tratamientos** (registrar, editar, activar/desactivar, precio y duración) y **Profesionales** (registrar con jornada, asignar tratamientos, filtrar por tratamiento) (RF-06 a 09). **Listo cuando** asignar un tratamiento inactivo o una jornada con inicio ≥ fin muestra el error. |
| N-14 | 🔴 | 10–11 | Vista de Agenda de citas (M5) | N-13 | `feature/appointments-view` | `AppointmentsView.fxml` + `AppointmentsController`: `DatePicker` del día y tabla de citas (`listByDate`), formulario de agendar (cliente, profesional filtrado por tratamiento, hora) con verificación de disponibilidad antes de guardar, y botones confirmar, reprogramar, cancelar, completar e inasistencia; muestra anticipo y recordatorio calculados por las reglas (RF-10 a 13, 26). **Listo cuando** agendar una cita cruzada muestra el rechazo y el estado de la cita cambia en la tabla al usar cada botón. |
| N-15 | 🟠 | 12 | Vista de Estadísticas y BI (M7) | N-14 | `feature/statistics-view` | `StatisticsView.fxml`: `BarChart` de productos y tratamientos más vendidos (con rango de fechas), clientes frecuentes, tabla de baja rotación con "Avisar al administrador" y promociones sugeridas por cliente (RF-28 a 31). **Listo cuando** cambiar el rango de fechas actualiza las gráficas y las listas vacías muestran un mensaje en vez de una gráfica en blanco. |
| N-16 | 🟢 | 13–14 | Vistas de IA: Asistente (M9) y Probador (M8) | N-15, S-12, S-13, S-14 | `feature/ai-views` | **Asistente:** `AssistantView.fxml` tipo chat que llama a `AppContext.ai().assistant()`; cuando la respuesta trae `pendingConfirmation` muestra "Confirmar / Cancelar" antes de ejecutar (ADR-0006). **Probador:** `TryOnView.fxml` con selección de cliente, `FileChooser` de foto, tipo de transformación y el resultado con el rótulo "simulación, no garantía". Ambas muestran un aviso si `isSimulated()` es verdadero (RF-21 a 23). **Listo cuando** el asistente no agenda una cita sin pulsar "Confirmar" y el probador rechaza una solicitud sin cliente. |

### 4.3 Samuel David Rangel — Backend de IA (en paralelo)

> Samuel trabaja **solo backend**. Puede tomar estas tareas en cualquier orden que respete "Depende de". Además revisa y aprueba los PR de Nicoll e Isabella.

| ID | Pri. | Tarea | Depende de | Rama | Descripción y criterio de aceptación |
|---|---|---|---|---|---|
| S-12 | 🔴 | Asistente con herramientas (simulado) | — | `feature/ai-assistant` | Viene de la Entrega 1. Paquete `ai`: `SmartAssistant`, `AssistantTool` (`getName`, `getDescription`, `getParameters`, `requiresConfirmation`, `execute(Map<String,String>)`), `ToolRegistry` (lista blanca) y herramientas `FindCustomerTool`, `ListLowStockTool`, `ListAppointmentsByDateTool`, `CheckAvailabilityTool`, `BookAppointmentTool` (exige confirmación). Las herramientas llaman a los `*Service`, **nunca a los DAO**. `SimulatedAssistant` responde sin red. **Listo cuando** una herramienta no registrada se rechaza y `BookAppointmentTool` respeta cruce de horarios y confirmación (pruebas con Mockito). |
| S-13 | 🔴 | Probador virtual (simulado) | — | `feature/ai-try-on` | Viene de la Entrega 1. `TryOnService`, `TryOnRequest`/`TryOnResult` y `SimulatedTryOnService` (devuelve una imagen de ejemplo rotulada "simulación, no garantía"); exige cliente registrado y valida la transformación con `Validator`. **Listo cuando** una solicitud inválida lanza `ValidationException` y la válida devuelve el resultado simulado. |
| S-14 | 🔴 | `AiConfig` y fábrica de proveedores | S-12, S-13 | `feature/ai-config` | `AiModule` (contrato), `AiConfig` que lee `config.properties` (proveedor, clave por variable de entorno, nunca en el repo) y `AiProviderFactory` que elige la implementación API o la simulada (ADR-0005). Sin clave o sin red ⇒ simulada. Agrega `ai()` a `AppContext` y un ADR-0014 con el proveedor elegido. **Listo cuando** sin `config.properties` la app arranca en modo simulado y hay prueba de cada rama de la fábrica. *Desbloquea N-16: fusionar antes del Día 13.* |
| S-15 | 🟠 | Adaptador por API del asistente | S-14 | `feature/ai-assistant-api` | `ApiAssistant implements SmartAssistant` con `java.net.http.HttpClient`: envía el mensaje y la descripción de las herramientas de `ToolRegistry`, ejecuta solo herramientas registradas y devuelve `pendingConfirmation` para las que lo exigen. Timeout y error de red ⇒ `AssistantReply` con mensaje claro (nunca excepción a la UI). **Listo cuando** hay pruebas con un `HttpClient` falso de respuesta normal, llamada a herramienta, herramienta desconocida y timeout. |
| S-16 | 🟢 | Adaptador por API del probador | S-14 | `feature/ai-try-on-api` | `ApiTryOnService implements TryOnService` contra el proveedor de imágenes elegido, con el mismo rótulo y la misma validación que el simulado. **Listo cuando** un fallo del proveedor devuelve el error controlado y la app sigue usable. |
| S-17 | 🟢 | Persistencia de IA | S-12, S-13 | `feature/ai-persistence` | DAO de texto para conversaciones del asistente y sesiones del probador con los campos de las tablas `assistant_*` y `try_on_session` de `sql/schema.sql`. **Listo cuando** una conversación y una sesión se recargan iguales desde disco. |
| S-18 | 🔴 | Integración final y entrega | todas las 🔴 | `docs/corte3` | Prueba manual guiada de todas las vistas sobre `develop`, actualiza `TEAM.md`, `docs/README.md` y la evidencia de commits/PR por integrante en `docs/fase3/`; PR `develop` → `main` con tag `v0.3-corte3`. |

## 5. Flujo de Git

El mismo de la Entrega 1 ([`roadmap.md`](roadmap.md), sección 5): una rama por tarea desde `develop` actualizado, commits atómicos en Conventional Commits, `mvn clean verify` antes del PR, merge normal (no *squash*) y rama borrada tras fusionar.

- **Revisor:** Samuel revisa los PR de Isabella y Nicoll; Isabella o Nicoll (alternando) revisan los de Samuel.
- **Archivos compartidos:** `AppContext` y `Navigator` son de Isabella. Si Nicoll o Samuel necesitan tocarlos (registrar una vista o el módulo `ai()`), lo hacen en su propio PR y lo avisan en el canal.
- Ejemplo de commits de una vista (I-15): `feat(view): add products table and form layout` → `feat(view): wire ProductsController to ProductService` → `feat(view): highlight low-stock products` → `test(view): add formatter tests`.

## 6. Hitos

| Día | Hito | Qué se revisa |
|---|---|---|
| 1 | Reunión de contratos (30 min) | Sección 3 aceptada por los tres |
| 3 | **Base de UI lista** (I-12, I-13, I-14) | Navegación, estilos y alertas en `develop` |
| 7 | **Fin del bloque de Isabella** (I-17) | Productos, Configuración y Ventas funcionando; Nicoll abre N-12 |
| 7 y 11 | Sync de 20 min | Bloqueos y avance de la IA de Samuel |
| 12 | **IA lista para la UI** | S-12, S-13 y S-14 ✅; si no, N-16 pasa al siguiente corte |
| 13 | Congelamiento | Solo correcciones y pruebas |
| 14 | **Entrega** | S-18: `develop` → `main`, tag `v0.3-corte3` |

## 7. Definición de "Terminado"

- [ ] Las vistas solo llaman a servicios (`view → service → dao`); ningún controlador importa un DAO ni una clase de `ai` concreta.
- [ ] Ningún error llega como traza a la pantalla: todo pasa por `AlertPresenter`.
- [ ] Sin colores, textos de moneda ni formatos de fecha repetidos: salen de `styles.css` y de los formateadores de I-14.
- [ ] Javadoc en clases y métodos públicos; nombres en inglés; checklist GRASP/SOLID/Clean Code de [`roadmap.md`](roadmap.md) sección 7.
- [ ] Pruebas JUnit de la lógica no visual (formateadores, `AppContext`, todo el paquete `ai`); `mvn clean verify` en verde.
- [ ] PR aprobado y fusionado; fila actualizada en la tabla de la sección 1.

## 8. Criterio de aceptación del Corte 3

1. `mvn javafx:run` abre Maison Glow y se puede navegar por todos los módulos.
2. Desde la interfaz se registra cliente, profesional, tratamiento y producto; se agenda una cita (y se ve el rechazo de un cruce); se vende con carrito y se ve el stock descontado y la factura; se consultan estadísticas y baja rotación.
3. El asistente responde en modo simulado, usa solo herramientas de la lista blanca y pide confirmación antes de agendar.
4. El probador devuelve un resultado rotulado "simulación, no garantía".
5. Sin clave de API ni internet la aplicación funciona completa en modo simulado (RNF-13).
6. Isabella y Nicoll con todos sus commits en frontend; Samuel con todos los suyos en `ai`; todo por PR.

## 9. Fuera del Corte 3

- Conexión SQLite y DAO JDBC (la UI sigue sobre los DAO de archivo de texto; el cambio de DAO no afectará las vistas).
- Usuarios y roles (`app_user`), promociones persistidas, pagos, facturas y notificaciones.
- Correo real para la factura.
