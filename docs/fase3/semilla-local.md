# Semilla local para pruebas y presentación

La semilla se genera de forma explícita en una carpeta nueva, separada de `data/session/`. Incluye datos ficticios, cuentas de administrador y cliente, imágenes de ejemplo, pedidos y registros de cambios. No usa IA ni envía correos.

Desde PowerShell, en la raíz del proyecto:

```powershell
$claveDemo = Read-Host "Clave de las cuentas ficticias (12–128 caracteres)" -AsSecureString
$env:MAISON_GLOW_DEMO_PASSWORD = [System.Net.NetworkCredential]::new("", $claveDemo).Password
Remove-Variable claveDemo
mvn -q compile
mvn -q exec:java '-Dexec.args=seed-local SMALL data/demo-small 2026-11-17T12:00 42'
```

Para abrir esa carpeta en JavaFX, en la misma terminal:

```powershell
$env:MAISON_GLOW_DEMO_FOLDER = (Resolve-Path data/demo-small).Path
mvn javafx:run
```

Las cuentas de ejemplo son `demo-admin-1`, `demo-cliente-0001` y `demo-cliente-0002`. Todas usan la clave introducida en la terminal. No se debe compartir esa clave ni ponerla en Git. La cuenta del segundo cliente permite probar el aislamiento de información.

El perfil `LARGE` usa el mismo comando con `LARGE` y otra carpeta nueva. Contiene 1.000 clientes, 200 productos, 20 profesionales, 30 tratamientos, 5.000 citas, 3.000 ventas, 1.000 pedidos y 1.000 cambios. El perfil `SMALL` cubre el recorrido de presentación. La fecha y semilla numérica fijan los datos de negocio; las sales criptográficas de las contraseñas y los nombres internos de las imágenes siguen siendo aleatorios.

Repetir el comando con los mismos parámetros reutiliza la carpeta sin duplicar datos. Cambiar parámetros exige otra carpeta. Un destino que ya contiene datos ajenos se rechaza. Si una generación se interrumpe, no usar esa carpeta para la presentación: crear una nueva. Para volver a la base habitual, quitar `MAISON_GLOW_DEMO_FOLDER` de la terminal (`Remove-Item Env:MAISON_GLOW_DEMO_FOLDER`).
