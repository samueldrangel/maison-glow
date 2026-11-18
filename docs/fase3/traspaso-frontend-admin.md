# Traspaso del frontend base y del administrador (I-R06) — para Nicoll

Fecha: 18 de noviembre de 2026. Autora: Isabella. Destinataria: Nicoll (bloque 2, N-R01 → N-R06).

Este documento entrega lo que el bloque 1 dejó listo para que el área del cliente se construya **sobre piezas ya existentes** y sin abrir estilos, navegación ni componentes paralelos. El backend que consumirán las pantallas del cliente está en [`roles-backend.md`](roles-backend.md).

## 1. Cómo arranca la aplicación y cómo se decide qué ve cada cuenta

| Pieza | Para qué sirve |
|---|---|
| `AppWindow` | Muestra el inicio de sesión, el registro o el área de la cuenta según la sesión. Al cerrar sesión descarta todas las pantallas. No hay que tocarlo para añadir pantallas. |
| `MainView` + `MainController` | Menú lateral y área central. Muestra solo las entradas del rol de la sesión; el perfil lee el nombre y el rol reales. |
| `View` | Una constante por pantalla, con su `.fxml` y **un solo rol**. Es el único sitio donde se registra una pantalla nueva. |
| `Navigator` | Carga y muestra la pantalla pedida y **rechaza** (muestra «Sin permiso») la de otro rol aunque se pida directamente. Conserva las pantallas con `keepsState`. |
| `AccessGateway` / `AuthenticationAccessGateway` | Lo que usan las pantallas de acceso para entrar, registrarse y cambiar la contraseña. Las pantallas de negocio usan `context.auth()`, `context.portal()`, etc. |

### Rutas del área del cliente (ya creadas, hoy «En construcción»)

`View.CUSTOMER_HOME` (Inicio), `PROFILE` (Mi perfil), `SHOP` (Tienda), `MY_PURCHASES` (Mis compras), `MY_APPOINTMENTS` (Mis citas) y `MY_CHANGES` (Mis cambios). Ya tienen su entrada en el menú y su `.fxml` esperado en `src/main/resources/com/maisonglow/view/`:

| Constante | Archivo que hay que crear | Tarea |
|---|---|---|
| `CUSTOMER_HOME` | `CustomerHomeView.fxml` | N-R01 |
| `PROFILE` | `ProfileView.fxml` | N-R01 |
| `SHOP` | `ShopView.fxml` | N-R02 |
| `MY_PURCHASES` | `MyPurchasesView.fxml` | N-R03 |
| `MY_APPOINTMENTS` | `MyAppointmentsView.fxml` | N-R04 |
| `MY_CHANGES` | `MyChangesView.fxml` | N-R05 |

En cuanto exista el `.fxml`, el `Navigator` lo carga solo. Si la pantalla guarda un carrito, una foto o un chat, pasa su constante a `keepsState = true` (ya lo está `MY_CHANGES`). El probador y el asistente del cliente (N-R05) son rutas nuevas con rol `CUSTOMER`: hoy `TRY_ON` y `ASSISTANT` son solo del administrador.

**Reglas que cumple toda pantalla nueva**
1. El controlador recibe `AppContext` por constructor y **solo llama a servicios**, nunca a un DAO.
2. Las operaciones lentas (base de datos, archivos, cámara, IA) van fuera del hilo de JavaFX con `BackgroundTask`.
3. Si guarda estado o tiene algo pendiente, implementa `ViewLifecycle.dispose()`: al cerrar sesión debe soltar lo pendiente e ignorar respuestas tardías.
4. Nunca recibe el `customerId` de un formulario: el cliente sale de la sesión (`portal()`, `orders().mine()`, `changes().mine()`).

## 2. Componentes compartidos

| Componente | Uso |
|---|---|
| `AlertPresenter` | `showError(e)`, `showInfo(texto)`, `toast(texto)`, `confirm(pregunta)`. Traduce las excepciones a mensajes claros; una sesión terminada (`SessionExpiredException`) avisa y vuelve al inicio de sesión. |
| `FieldValidator` | Marca en rojo el campo cuyo nombre aparece en el mensaje del servicio y escribe el error debajo. Se registran los campos con palabras clave; los más específicos primero. |
| `BackgroundTask.run(executor, trabajo, alExito, alError, sigueVigente)` | Ejecuta fuera del hilo de JavaFX y devuelve el resultado en él. Si `sigueVigente` dice que no (pantalla descartada), no llama a nada. |
| `LoadingOverlay.over(stackPane)` | Capa con indicador que bloquea los clics mientras algo corre: `show("Cargando…")` / `hide()`. |
| `PhotoPickerPane` + `PhotoPickerController` | Panel para **tomar con la cámara o subir un archivo**, con vista previa, repetir y descartar; rechaza lo que no es imagen o pesa más de 10 MB. Entrega la foto con `onUse`; no guarda nada. Se carga con `ViewLoader.loadWithController(...)` y se coloca en el formulario. Ejemplos: `ProductsController` (portada) y `HistoryController` (antes y después). |
| `CameraCaptureDialog` | Ventana de la cámara (la usa el panel de foto y el probador). |
| `ViewLifecycle` | `dispose()` al descartar una pantalla. |
| `CurrencyFormatter`, `DateFormats` | Pesos `es-CO` y fechas: no escribir formatos propios. |

**Estilos** (`styles.css`): la paleta vive en `.root, .main-root, .view-root`; no se definen colores nuevos. Clases útiles: `card`, `form-card`, `form-title`, `form-label`, `form-hint`, `field-error-message`, `button-gold` / `button-primary` / `button-secondary` / `button-compact`, `filter-chip`, `chip-success` / `chip-gold` / `chip-rose` / `chip-neutral` / `chip-warning`, `banner-warning`, `stat-tile` / `stat-number`, `photo-frame`, y las del inicio (`home-*`) y del acceso (`auth-*`). El FXML se revisa en Scene Builder: nada de `<fx:define>` con `$ref`; el texto que empieza con `$` o `%` se escapa (`\$`).

## 3. Estados de lo que verá el cliente

| Objeto | Estados | Notas |
|---|---|---|
| Pedido (`Commerce.OrderStatus`) | `PENDING_PAYMENT`, `PAID`, `CANCELLED`, `EXPIRED` | Un pedido pendiente **no es** una venta: el stock está reservado 30 minutos, no vendido. Pagar lo hace solo el administrador. Usar una clave de reintento nueva por cada confirmación de carrito. |
| Cita (`AppointmentStatus`) | pendiente, confirmada, completada, cancelada, inasistencia | `AppointmentLook` da la palabra y el chip de cada estado. La reserva del cliente declara anticipo cero. |
| Producto del catálogo | publicado / no publicado, `hasCover`, stock disponible | El cliente solo recibe publicados. Preguntar `hasCover()` antes de pedir la portada. |
| Cambio del historial (`ChangeKind`) | `PERSONAL`, `SERVICE`, `SIMULATION` | La simulación se distingue siempre de una atención real. |

## 4. Ejemplos de uso

```java
// Una llamada lenta con capa de carga y descarte si se cierra la sesión.
loading.show("Guardando…");
BackgroundTask.run(executor, () -> service.algoLento(dato), resultado -> {
    loading.hide();
    alerts.toast("Listo.");
}, error -> {
    loading.hide();
    alerts.showError(error);
}, () -> !disposed);
```

```java
// El panel de foto dentro de un formulario.
var loaded = ViewLoader.loadWithController(MiControlador.class.getResource("/com/maisonglow/view/PhotoPickerPane.fxml"), context);
loaded.controller().onUse(foto -> guardar(foto));   // solo al pulsar «Usar esta foto»
caja.getChildren().add(loaded.root());
```

## 5. Cómo probar una pantalla

- `FxTest.start()` / `FxTest.onFxThread(...)` arrancan JavaFX y ejecutan en su hilo; las pruebas se omiten si no hay pantalla.
- **Con sesión de administrador:** `TestAccounts.open(carpeta)`.
- **Con cuentas reales:** crear el administrador con `context.auth().createFirstAdmin(...)`, registrar clientes con `registerCustomer(...)` e iniciar sesión con `auth().login(...)`. Ver `OrderServiceGatewayTest` e `HistoryViewTest` para el patrón completo, incluido un reloj que se mueve.
- Los diálogos se sustituyen con `new AlertPresenter(() -> null, alerta -> ...)`; las respuestas en segundo plano, con `useExecutor(Runnable::run)` o reteniéndolas para probar el descarte.
- `AdminFlowTest` recorre de punta a punta el inicio de sesión, todo el menú del administrador, el cierre de sesión y la nueva entrada. Es el modelo para el recorrido del cliente (N-R06).

## 6. Lo que queda del lado administrativo

- Pedidos, Cuentas, Historial, Productos con portada y publicación, Ventas, Agenda, Clientes, Estadísticas, Configuración, Asistente y Probador están conectados al servicio real y probados.
- La forma de pago de un pedido pagado no se muestra: el servicio no la devuelve.
- Registrar el anticipo recibido de una cita (`portal().recordDeposit`) no tiene pantalla propia en Agenda; si el equipo lo necesita para el cierre, es una tarea pequeña del administrador.
- Verificación manual en la ventana real (cámara física, IA con clave): pendiente de la persona que presente.
