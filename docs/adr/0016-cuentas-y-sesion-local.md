# ADR-0016: Cuentas y sesión con SQLite local

- Estado: Aceptado para la base de autenticación; integración por módulos en curso.
- Fecha: 2026-11-17.

## Decisión

Mantener SQLite local y añadir autenticación a través de `AppContext.auth()`, sin asignar pantallas a Samuel. `AuthenticationService` coordina reglas y transacciones; `AccountDaoSqlite` persiste credenciales y devuelve a la presentación únicamente `UserAccount`, sin hash.

El registro público crea una ficha nueva y cuenta CUSTOMER atómicamente. Las fichas antiguas solo se vinculan mediante operación administrativa después de verificar identidad. La configuración inicial del ADMIN es explícita y se ofrece al abrir una instalación que aún carece de administrador. Quien opera la base local puede restablecer la contraseña de un administrador existente desde la terminal. La contraseña siempre se elige durante la configuración o el restablecimiento.

Se usa PBKDF2-HMAC-SHA256 del JDK, sal aleatoria e iteraciones registradas en el hash para no añadir una dependencia criptográfica a esta entrega. Las sesiones son locales, en memoria, duran ocho horas y se verifican contra versión de credenciales y estado persistidos. Un cambio de contraseña o actividad invalida otras instancias.

La migración 1 añade `auth_version` e índice normalizado de usuario, en una transacción con registro de versión. El fallo no elimina ni fusiona datos. Mantener copia completa para restaurar; las versiones previas no ofrecen la protección de cuentas nueva.

## Consecuencias y límite

El hash debe ejecutarse fuera del hilo JavaFX. La UI debe limpiar vistas y estado al salir. Cada módulo de negocio debe aplicar las guardas del servicio en S-R03–S-R07: esta base por sí sola no protege los métodos antiguos ni la consola. La conexión en nube queda fuera de este alcance.

Contratos, procedimiento de configuración y pruebas: [roles-contratos.md](../fase3/roles-contratos.md).
