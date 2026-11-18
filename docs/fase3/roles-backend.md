# Backend local de roles — S-R03 a S-R07

Esta entrega continúa el PR #60 sobre SQLite local. No añade pantallas ni despliega servicios en la nube.

## Qué debe integrar cada frontend

Los controladores reciben el mismo `AppContext`. Los servicios nuevos se obtienen de `catalog()`, `orders()`, `portal()` y `changes()`. `auth()` conserva su contrato y añade `listAccounts()` para administración. Las operaciones consultan la sesión en backend; un formulario no decide el cliente propietario ni puede declarar un pago.

**Puente con I-R01/I-R02:** `access()` devuelve `AuthenticationAccessGateway`, que implementa exactamente los contratos `AccessGateway`, `AccessSession`, `AccessRole` y `RegistrationData` ya integrados en `develop` (PR #57–#59). El registro de esa interfaz abre la sesión después de persistir la cuenta; login/logout y cambio de contraseña usan `auth()`. Se conserva el adaptador que ya trae `develop`, conectado al servicio interno de clientes para resolver el nombre sin exigir permisos administrativos al cliente. `useAccess(...)` continúa como punto de prueba de presentación: el backend autoriza exclusivamente mediante `auth()`, nunca por un rol recibido del gateway de prueba.

**Cambio importante:** `customers()`, `products()`, `professionals()`, `treatments()`, `appointments()`, `sales()`, `statistics()`, `settings()`, `lowRotation()` y `promotions()` son administrativos. Cada operación vuelve a comprobar la cuenta, aunque el controlador retenga el servicio después del logout. Una denegación en esos contratos antiguos usa `AccessDeniedDataException`, compatible con `DataAccessException`, y se presenta como «Acceso restringido».

Las pantallas de acceso deben integrarse antes de permitir la entrada a Inicio administrativo. Esta rama no inicia una sesión por defecto ni abre datos de negocio para que una pantalla antigua funcione sin login. La integración visual y el vaciado de cachés de `Navigator` pertenecen a I-R01/I-R02 y al cierre de S-R07.

Ejecutar operaciones de base de datos, archivos e IA fuera del hilo JavaFX. Una respuesta tardía se descarta si la sesión cambió. La UI limpia carrito, fotos, conversaciones, selectores y acciones pendientes al cerrar sesión.

## Productos publicados — Isabella / Nicoll

| Método | Acceso | Resultado |
|---|---|---|
| `catalog().setPublished(productId, boolean)` | ADMIN | Publica/retira un producto activo; no altera historial ni inventario |
| `catalog().setCover(productId, Path)` | ADMIN | Importa y conserva una portada; cámara y archivo usan el mismo método |
| `catalog().listPublished(search)` | Cuenta autenticada | Lista `Commerce.CatalogProduct`; buscar con `""` muestra todos los publicados activos |
| `catalog().findPublished(productId)` | Cuenta autenticada | Detalle con precio y existencias disponibles después de reservas |
| `catalog().cover(productId)` | ADMIN o producto publicado para cliente | Ruta gestionada; consultar `hasCover()` antes para mostrar un marcador si falta |

Crear/editar/ajustar productos sigue usando `products()` como ADMIN. Los productos anteriores y los nuevos comienzan **sin publicar**. Activar un producto no lo publica; retirarlo de publicación impide nuevos pedidos.

Las fotos admiten PNG/JPEG reales, hasta 10 MB y 4096 píxeles por lado. Se decodifican y vuelven a guardar como PNG con nombre generado en `data/session/media/`, sin conservar el nombre original ni metadatos de cámara. Una foto inválida conserva la portada anterior. La sustitución se confirma antes de retirar el archivo anterior; las retiradas fallidas quedan en una cola de limpieza para reintentar al arrancar.

## Pedidos y pagos — Isabella / Nicoll

| Método | Acceso | Uso |
|---|---|---|
| `orders().place(requestKey, Map<productId, quantity>)` | CUSTOMER | Confirmar carrito propio; backend fija cliente y precios |
| `orders().mine()` | CUSTOMER | Mis pedidos |
| `orders().all()` | ADMIN | Panel de pedidos |
| `orders().find(orderId)` | Propietario o ADMIN | Detalle autorizado |
| `orders().cancel(orderId)` | Propietario o ADMIN | Libera una reserva pendiente, sin crear devolución ficticia de una venta pagada |
| `orders().confirmPayment(orderId, method)` | ADMIN | Cobrar y registrar venta; método `CASH`, `CARD` o `TRANSFER` |
| `orders().receipt(orderId)` | Propietario o ADMIN | `Sale` pagada para reutilizar el formato de comprobante existente |

Generar `requestKey` una vez por confirmación de carrito, por ejemplo `UUID.randomUUID().toString()`, y conservarla en reintentos/doble clic. Repetir clave y cantidades devuelve el mismo pedido; usarla para otro carrito se rechaza. Cambiar el carrito exige una nueva clave. No enviar precios ni `customerId`.

Estados: `PENDING_PAYMENT`, `PAID`, `CANCELLED`, `EXPIRED`. Un pedido no es una venta ni un recibo de pago. El pago es en el local y no hay pasarela ni envíos. La reserva dura 30 minutos; se procesa el vencimiento al iniciar, consultar catálogo/pedidos y antes de escrituras administrativas de inventario.

Disponibilidad = stock físico − reservas pendientes. Crear pedido no descuenta stock físico. Cobrar libera su reserva, registra una venta y un pago y descuenta stock dentro de una única transacción; repetir el cobro no duplica nada. Un fallo restaura pedido, stock, venta y pago. Se conserva el precio aceptado al crear el pedido. La venta anterior de mostrador y los ajustes de stock tampoco pueden consumir unidades reservadas.

## Perfil y citas — Nicoll / administración de Isabella

| Método de `portal()` | Acceso | Resultado |
|---|---|---|
| `profile()` / `updateContact(name, phone, email)` | CUSTOMER | Perfil propio; documento, rol e inasistencias no editables |
| `treatments()` / `professionals(treatmentId)` | Autenticado | Tratamientos activos y opciones de profesional sin documentos/contactos privados |
| `available(professionalId, treatmentId, start)` | Autenticado | Disponibilidad booleana; no expone citas de otros clientes |
| `book(professionalId, treatmentId, start)` | CUSTOMER | Cita propia pendiente con anticipo pagado **cero** |
| `myAppointments()` / `reschedule(appointmentId, start)` / `cancelAppointment(appointmentId)` | CUSTOMER | Solo citas propias y dentro del plazo |
| `requiredDeposit(appointmentId)` | CUSTOMER propietario | Anticipo exigido por configuración e inasistencias; no significa pago recibido |
| `recordDeposit(appointmentId, cumulativeAmount, method)` | ADMIN | Registra dinero recibido; importe acumulado evita duplicación por doble clic |
| `myPurchases()` / `summary()` | CUSTOMER | Ventas y resumen personales |

El cliente reserva sin inventar un anticipo. El administrador registra el importe efectivamente recibido y confirma la cita con `appointments().confirm(id)`, que exige el mínimo configurado. El anticipo acumulado no puede disminuir ni superar el precio. Registrar dos veces el mismo acumulado no crea dos pagos.

Se mantienen jornada, tratamientos ofrecidos, aviso mínimo, recordatorio y rechazo de cruces. El plazo mínimo configurado de reserva se aplica también a cancelación/reprogramación por cliente; el administrador gestiona excepciones mediante la agenda existente. Crear/reprogramar se verifica otra vez al escribir y SQLite rechaza cruces concurrentes.

## Historial y fotos — Nicoll / Isabella

| Método de `changes()` | Acceso | Resultado |
|---|---|---|
| `addOwn(kind, notes, date, sourcePath, resultPath)` | CUSTOMER | `PERSONAL` o `SIMULATION`; cliente derivado de sesión |
| `mine()` | CUSTOMER | Línea de tiempo propia |
| `allowStaffAccess(boolean)` / `staffAccessAllowed()` | CUSTOMER | Autorizar/revocar consulta administrativa del historial y fotos; por defecto desautorizada |
| `forCustomer(customerId)` | Propietario o ADMIN con autorización | Historial del cliente seleccionado |
| `addService(appointmentId, notes, sourcePath, resultPath)` | ADMIN con autorización | Registro `SERVICE` ligado a una cita completada |
| `photo(changeId, isResult)` | Propietario o ADMIN autorizado | Ruta protegida para original/resultado |
| `removePhotos(changeId)` | Propietario o ADMIN autorizado | Retira fotos; conserva descripción, autor y registro oficial |

`date` no puede ser futura; notas obligatorias de hasta 4000 caracteres. Para `SIMULATION` se conservan original y resultado y se rotula la simulación; no se registra como una atención real. Las fotos son copias permanentes de archivos aceptados, aunque el resultado temporal del probador desaparezca. Si una importación o escritura falla, se compensa la copia nueva. Cambiar de sesión durante una importación impide adjuntar la foto al siguiente usuario.

## IA y persistencia por cuenta

`ai()` conserva `assistant()`, `tryOn()` e `isSimulated()`. Los proveedores y registros de confirmación se construyen por login; salir y volver a entrar con la misma cuenta también descarta acciones pendientes. Cada herramienta está ligada al login que la originó y comprueba la sesión al ejecutar. Las llamadas HTTP se hacen sin bloquear el cierre de sesión; una respuesta tardía no se entrega a otra cuenta.

El administrador conserva sus herramientas. El cliente tiene una lista limitada: `my_profile`, `list_appointments_by_date` (solo propias), `published_products` y `book_appointment` (sin cliente ni anticipo elegibles). La reserva exige confirmación. No tiene búsqueda global de clientes, stock bajo ni agenda del negocio. El probador comprueba que el cliente solicitado sea el propietario o una operación administrativa autorizada.

`aiHistory()` comprueba propietario de conversaciones, mensajes y llamadas; filtra sesiones del probador. El historial nuevo se guarda bajo `data/session/account-ai/`. Los archivos anteriores sin propietario comprobado se conservan fuera de esa carpeta y **no se asignan** al usuario cuyo ID coincida por casualidad. Las fotos que el cliente decida conservar se incorporan a `changes()`.

## SQLite, respaldo y consola

La migración aditiva **2** se aplica en una transacción. Añade `product_listing`, `customer_order`, `order_item`, `customer_photo_access`, `customer_change`, `media_delete_queue`, índices y la restricción de stock reservado. No borra ni reinterpreta ventas, productos, citas o clientes anteriores. La versión 1 de autenticación permanece registrada.

Respaldar/restaurar **toda `data/session/` con la aplicación cerrada**: SQLite, `media/`, `account-ai/`, capturas y archivos históricos. No volver a una aplicación anterior para escribir sobre la base actual: la versión anterior no interpreta pedidos ni sus reservas. Si se restaura una copia anterior, reconciliar primero operaciones posteriores.

La consola administrativa `menu` exige `MAISON_GLOW_USERNAME` y `MAISON_GLOW_PASSWORD` como variables temporales de proceso y una cuenta ADMIN. Pedir la contraseña oculta con `Read-Host -AsSecureString`, siguiendo el procedimiento de configuración inicial, y eliminar ambas variables al terminar. `demo` sigue operando exclusivamente sobre una carpeta temporal, con un administrador aleatorio de esa demo; no abre una sesión automática en datos reales.

## Seguimiento

Validación sobre `develop` actualizado a `b79296b` (incluye PR #57–#60): **`mvn clean verify` aprobado, 756 pruebas sin fallos, errores ni omisiones; cobertura requerida aprobada**. Incluye 20 pruebas nuevas del backend para publicación/fotos, reintentos, competencia por la última unidad, pagos con rollback, datos propios, consentimientos, sesiones de IA y restauración de BD + medios en otra carpeta. Las pruebas de JavaFX mantienen el toolkit activo entre ventanas; no se modifica ese comportamiento en la aplicación.

S-R03–S-R06 se entregan como backend para revisión. S-R07 incorpora comprobaciones de permisos, compatibilidad, rollback y restauración, pero su cierre completo espera I-R06/N-R06 y el recorrido visual integrado. La semilla masiva S-R08 continúa opcional y posterior al cierre; no bloquea la presentación. Ejecutar `mvn clean verify` antes de integrar.
