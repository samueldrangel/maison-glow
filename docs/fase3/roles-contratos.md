# S-R01 / S-R02 — Contratos y base de autenticación local

Fecha: 17 de noviembre de 2026. Responsabilidad: Samuel. Base: JavaFX + SQLite local.

## Estado y límite de esta entrega

Esta entrega implementa el backend de cuentas y sesión, la migración aditiva 1 y el armado en `AppContext`. No modifica pantallas de Isabella o Nicoll. Los servicios anteriores de productos, ventas, citas, estadísticas, consola e IA conservan su comportamiento; **todavía no están protegidos por esta nueva sesión**. Su adaptación corresponde a S-R03–S-R07 y es requisito del cierre de roles.

Las operaciones nuevas de cuentas verifican sus propios permisos. `auth.requireAdmin()` y `auth.requireCustomerAccess(...)` son guardas de backend: en las tareas siguientes se invocan dentro de cada operación protegida, no solo desde los controladores. No exponer los DAO a los controladores ni interpretar una cuenta recibida del formulario como autorización.

## Contrato implementado para Isabella

Los controladores reciben el `AppContext` existente y acceden a `context.auth()`. El contexto arranca sin sesión y no crea un administrador automáticamente.

| Operación | Firma en `AuthenticationService` | Resultado / reglas |
|---|---|---|
| Registro público | `registerCustomer(ContactData, String username, char[] password)` | Devuelve `UserAccount`; crea ficha nueva + cuenta `CUSTOMER` en una transacción; no inicia sesión |
| Login | `login(String username, char[] password)` | Devuelve `UserAccount`; reemplaza sesión anterior, incluso si falla; no devuelve credenciales |
| Sesión actual | `currentSession()` | `Optional<UserAccount>`; consulta cuenta persistida y comprueba versión, actividad y vencimiento |
| Logout | `logout()` | Elimina la sesión en memoria; la UI debe limpiar sus vistas, carrito, fotos y acciones pendientes |
| Cambio de contraseña | `changePassword(char[] actual, char[] nueva)` | Exige sesión y contraseña actual; invalida todas las sesiones de la cuenta; volver al login |
| Desactivar / activar | `setActive(int userId, boolean active)` | Solo administrador; no permite desactivar al último administrador activo; invalida sesiones anteriores |
| Vinculación verificada | `linkCustomerAccount(int customerId, String username, char[] password)` | Solo administrador, después de verificar identidad por fuera del formulario público; exige ficha existente sin cuenta |
| Configuración inicial local | `createFirstAdmin(String username, char[] password)` | Solo para la instalación local; rechaza si ya existe cualquier administrador; no mostrarlo como selector de rol en registro |

`UserAccount` contiene únicamente `id`, `username`, `role` (`ADMIN` / `CUSTOMER`), `customerId` (nulo para ADMIN) y `active`. No incluye hash ni token que pueda construirse para iniciar sesión.

Los usuarios se normalizan con `trim()` y minúsculas. Se admiten 3–80 caracteres ASCII: letras, números y `. _ @ + -`, comenzando por letra o número. La contraseña tiene 12–128 caracteres y se conserva exactamente, incluidos los espacios; no se recorta. El contacto utiliza la validación existente de `ContactData`.

La sesión dura como máximo 8 horas desde el login, no se persiste en disco y no se restablece al reabrir la aplicación. Cada comprobación vuelve a consultar `active` y `auth_version`; reactivar una cuenta no revive sus sesiones anteriores. Cada instancia de `AppContext` mantiene su propia sesión.

### Errores y trabajo en segundo plano

- `ValidationException`: campo inválido; mostrar en el formulario.
- `AuthorizationException extends BusinessRuleException`: credenciales incorrectas, falta de sesión o permiso; mensaje seguro para mostrar.
- `BusinessRuleException`: usuario/documento ocupado, instalación ya configurada o último administrador activo.
- `DataAccessException`: fallo de persistencia; no continuar con una autorización guardada en la UI.

Usar tareas JavaFX en segundo plano para login/registro/cambio de contraseña: el cálculo de hash es deliberadamente costoso. No invocarlos desde el hilo de renderizado. Al terminar, limpiar los campos y los arrays de contraseña con `Arrays.fill(password, '\0')`. No registrar los parámetros de estos métodos en logs.

### Integración visual sin atribuirse la sesión

```java
// Dentro de una tarea en segundo plano; pasar el resultado a JavaFX al terminar.
char[] password = passwordFieldValue.toCharArray();
try {
    UserAccount account = context.auth().login(usernameFieldValue, password);
    // La UI elige el menú por account.role(); el backend vuelve a autorizar cada operación.
} finally {
    java.util.Arrays.fill(password, '\0');
}
```

`logout()` no puede borrar nodos de JavaFX porque pertenece al backend. I-R02 debe vaciar la caché de `Navigator`, descartar acciones pendientes y volver a acceso antes de mostrar otra cuenta. Un resultado tardío de una tarea anterior tampoco debe aparecer en la siguiente sesión.

## Contratos de las siguientes tareas (diseño, aún no implementados)

| Tarea | Superficie prevista | Invariantes para implementar |
|---|---|---|
| S-R03 | Publicación y foto de portada; consulta de catálogo publicado | Administración exige ADMIN; consulta del cliente devuelve únicamente publicados; archivos gestionados por la app, independientes de rutas seleccionadas |
| S-R04 | Crear pedido propio, consultar propios, cancelar propio; cobrar como ADMIN | Cliente derivado de sesión, precio persistido recalculado, clave de reintento única; reservar y cobrar transaccionalmente sin doble descuento |
| S-R05 | Perfil y citas propios; disponibilidad limitada | `currentCustomerId()` evita un selector de cliente; solo ADMIN confirma pagos, completa o marca inasistencias; revalidar horario y cruces al escribir |
| S-R06 | Historial/fotos y herramientas IA por propietario | Persistir imágenes aceptadas; validar propietario en lectura, escritura y confirmación de herramienta; denegar historiales antiguos sin dueño verificable |
| S-R07 | Integración sobre consola, servicios existentes y navegación | Ningún acceso antiguo debe saltarse las guardas; preservar funcionamiento y probar dos cuentas + ADMIN |

Para S-R04 se propone reserva de pedido de **30 minutos**, con vencimiento antes de calcular disponibilidad y al arrancar; expiración/cancelación libera una sola vez. Para citas se conservan las reglas existentes de horario, aviso mínimo y anticipo; las transiciones terminales y la modificación posterior al inicio quedan reservadas al administrador. Estas reglas se concretan con pruebas al implementar cada tarea; no hay pedidos ni configuración nueva de reservas en esta entrega.

## Evolución de SQLite y conservación

- La tabla `app_user` ya existía; se añade `auth_version INTEGER NOT NULL DEFAULT 0` y un índice único de `lower(trim(username))`.
- `schema_migration` registra la versión aditiva 1. La instalación nueva y la actualización existente usan la misma ruta en `SqliteDatabase.initialize()`.
- Si la cuenta ya contiene un hash no compatible, login lo rechaza; no se transforma en contraseña ni se restablece con una clave predeterminada.
- Si existen nombres equivalentes como `ana` y ` ANA `, el índice rechaza la actualización; se revierte la transacción y se conservan las cuentas y el negocio. Resolver la duplicidad en una copia revisada antes de reintentar. No se fusionan identidades silenciosamente.
- Un número de migración posterior a 1 requiere una aplicación más reciente y provoca rechazo controlado.
- La actualización no borra tablas, no reclama fichas antiguas y no inserta cuentas demo.

Antes de actualizar una instalación real, cerrar la aplicación y respaldar toda `data/session/`. El código anterior puede leer los datos de negocio porque la ampliación es aditiva, pero **no aplica los permisos nuevos**: no usar una versión anterior como alternativa de acceso protegido. Para volver a la versión previa con sus datos exactos, restaurar la copia completa con la app cerrada; reconciliar cualquier cambio posterior antes de restaurar.

## Configurar el primer administrador local

Ejecutar explícitamente desde una terminal PowerShell en la raíz del proyecto. La contraseña se pide oculta y no se incluye como argumento ni se sube al repositorio:

```powershell
$env:MAISON_GLOW_ADMIN_USERNAME = Read-Host 'Usuario del primer administrador'
$adminSecret = Read-Host 'Contraseña (12–128 caracteres)' -AsSecureString
try {
    $env:MAISON_GLOW_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', $adminSecret).Password
    mvn -q exec:java '-Dexec.args=setup-admin'
} finally {
    Remove-Item Env:MAISON_GLOW_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:MAISON_GLOW_ADMIN_USERNAME -ErrorAction SilentlyContinue
    Remove-Variable adminSecret
}
```

Se usa `data/session/`, proveedor IA simulado y el mismo servicio transaccional de cuentas. Un segundo intento no reemplaza la contraseña ni crea otro administrador. La pantalla de login se conecta en I-R01; este comando no añade una pantalla ni inicia una sesión JavaFX.

## Pruebas

`AuthenticationIntegrationTest` prueba registro, rollback de ficha ante fallo de cuenta, normalización/duplicados, autorización por propietario, configuración inicial, vinculación verificada, cambio de contraseña e invalidación entre contextos, desactivación/reactivación, expiración con reloj falso y actualización SQLite repetible con conservación y fallo atómico. `PasswordHasherTest` comprueba sal aleatoria, comparación exacta y rechazo de hashes inválidos.

El hash usa PBKDF2-HMAC-SHA256 con 600.000 iteraciones y sal aleatoria de 16 bytes, sin nuevas dependencias. Referencias de implementación: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) y [JDK 21 PBEKeySpec](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/spec/PBEKeySpec.html).

Validación de esta entrega: **693 pruebas, sin fallos, errores ni omisiones**, y control de cobertura aprobado con `mvn clean verify`. Incluye 15 pruebas nuevas de autenticación/migración/hash. También se ejercitó `Main setup-admin` con el JDK de Maven y datos temporales: sin credenciales no escribe; crea un ADMIN con hash; repetir no duplica cuenta ni reemplaza contraseña. La verificación no usó una base real del equipo.

```powershell
mvn '-Dtest=AuthenticationIntegrationTest,PasswordHasherTest' test
mvn clean verify
```
