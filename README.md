# Maison Glow

Aplicación de escritorio para la gestión de un negocio de belleza: clientes, profesionales, tratamientos, productos, inventario, citas, ventas, estadísticas, asistente IA y probador virtual. Proyecto de Programación de Computadores III, Universidad Popular del Cesar.

Seguimiento de la ampliación: [roles de cliente y administrador](docs/roadmap-roles.md), con tareas, responsables, dependencias y criterios de aceptación. Consultar el estado de cada tarea antes de asumir que está integrada.

Primera parte de backend de roles: `AppContext.auth()` proporciona cuentas, sesión y permisos básicos. Ver [contratos y configuración del primer administrador](docs/fase3/roles-contratos.md). Las pantallas y la protección de los módulos anteriores siguen pendientes de integración; el arranque JavaFX actual todavía abre Inicio.

La interfaz usa JavaFX y la persistencia del negocio usa SQLite. La base se crea automáticamente: **no hay que instalar un servidor de base de datos ni ejecutar scripts SQL manualmente**.

## 1. Qué necesita cada integrante

Los comandos de esta guía se muestran para PowerShell en Windows.

| Requisito | Para qué sirve |
|---|---|
| Git | Descargar y actualizar el proyecto |
| JDK 21 o superior, de 64 bits | Compilar y ejecutar Java; se recomienda JDK 21 para una instalación uniforme |
| Maven 3.9 o superior | Descargar dependencias, ejecutar pruebas y abrir la aplicación |
| Internet en la primera compilación | Maven descarga JavaFX, SQLite JDBC y el controlador de cámara |
| Cámara integrada o USB, opcional | Usar **Tomar foto**; también se puede subir un archivo |
| `config.properties`, enviado por Samuel en privado | Seleccionar el proveedor IA |
| Clave API válida, entregada por un canal privado | Solo para IA real; se configura en una variable de entorno, no en Git |

No se necesita instalar JavaFX, SQLite, FFmpeg ni una librería de cámara por separado: Maven trae las dependencias Java. Una instalación de Maven necesita `JAVA_HOME` apuntando al **JDK** y sus comandos disponibles en `PATH`.

Sin archivo de configuración o sin clave, el negocio funciona y la IA entra en **modo simulado**. Para usar la API real, la cuenta de la clave debe tener acceso a los modelos configurados en el código y facturación disponible. Los adaptadores actuales usan `gpt-5.4` y `gpt-image-1.5`.

## 2. Descargar esta entrega

Esta entrega se publica en **`feature/probar-nicoll`**. Hasta que se integre en `develop`, usar esa rama; la rama `main` conserva una versión anterior.

```powershell
git clone --branch feature/probar-nicoll https://github.com/samueldrangel/maison-glow.git
cd maison-glow
```

Si ya tienen el repositorio, guardar primero cualquier trabajo local propio y ejecutar:

```powershell
git fetch origin
git switch feature/probar-nicoll
git pull --ff-only
```

Si la rama todavía no existe localmente, `git switch` la creará a partir de la rama remota cuando solo haya una coincidencia.

## 3. Verificar Java y Maven

En una terminal nueva, desde la raíz del proyecto:

```powershell
java -version
mvn -version
```

Ambos deben usar el JDK correcto. Si `java` muestra una versión antigua, revisar `JAVA_HOME` y `PATH` antes de continuar.

## 4. Compilar y probar antes de configurar la IA

```powershell
mvn clean verify
```

La primera vez puede tardar mientras descarga dependencias. El resultado esperado es **BUILD SUCCESS**. Se ejecutan pruebas de negocio, SQLite y vistas JavaFX, además del control de cobertura de servicios. Las pruebas de cámara usan una cámara de prueba y no fotografían a nadie. Las pruebas del proveedor API usan respuestas HTTP controladas.

## 5. Colocar el archivo privado de configuración

Samuel comparte **`config.properties` por privado**. Guardarlo directamente en la raíz, al lado de `pom.xml`, conservando exactamente el nombre. En Windows, comprobar que no haya quedado como `config.properties.txt`.

```text
maison-glow/
  pom.xml
  config.properties    <- archivo privado
  src/
  sql/
```

El archivo indica el proveedor y el nombre de la variable de entorno. El formato esperado es:

```properties
ai.provider=api
ai.api_key_env=MAISON_GLOW_AI_API_KEY
```

**El archivo por sí solo no basta para IA real:** la aplicación busca la clave en la variable indicada. No reconoce una clave pegada como otra propiedad del archivo. `config.properties` está ignorado por Git; no subirlo ni pegar claves en commits, capturas o mensajes públicos.

Para ejecutar solamente la simulación se puede usar `ai.provider=simulated`, o ejecutar sin configuración privada.

## 6. Configurar la clave y abrir la aplicación

En PowerShell, pedir la clave sin mostrarla en la pantalla ni incluirla como texto en el comando:

```powershell
$clavePrivada = Read-Host "Clave API recibida por privado" -AsSecureString
$env:MAISON_GLOW_AI_API_KEY = [System.Net.NetworkCredential]::new("", $clavePrivada).Password
Remove-Variable clavePrivada
mvn javafx:run
```

Si el archivo privado indica otro nombre en `ai.api_key_env`, usar ese nombre en lugar de `MAISON_GLOW_AI_API_KEY`.

La variable anterior dura lo que dure esa terminal. Al cerrarla, hay que configurarla de nuevo. Si ejecutan desde un IDE, establecer la variable en su configuración de ejecución; una terminal o un IDE que ya estaba abierto no incorpora automáticamente cambios hechos en otro proceso.

Para abrir sin IA real:

```powershell
mvn javafx:run
```

La ventana abre en **Inicio**. La barra lateral se expande al pasar el cursor. **Probador virtual** está debajo de **Asistente IA**.

## 7. Base de datos y datos de demostración

El primer arranque crea `data/session/maison-glow.db` con las tablas, índices y reglas del esquema incluido en la aplicación. Importa una sola vez los archivos de texto del negocio que ya hubiera en esa carpeta, conservando IDs y relaciones. Los archivos originales no se borran ni se reescriben. Si la importación falla, se revierte y se muestra el error para poder corregirlo y reintentar.

La semilla añade categorías, configuración, tres clientes ficticios identificados con `(demo)`, tres tratamientos, un profesional y tres productos. Los siguientes arranques no duplican la semilla ni restablecen los cambios del usuario. No incluye citas o ventas artificiales.

Clientes, profesionales, tratamientos, productos, citas, ventas, líneas y configuración se guardan en SQLite. Los importes se conservan como decimales exactos. La venta y el descuento de inventario se confirman juntos. El historial de IA de la primera entrega conserva su formato de texto.

Para respaldar, cerrar la aplicación y copiar **toda la carpeta `data/session/`**, incluidas las capturas. Para restaurar una copia, cerrar la aplicación antes de reemplazar esa carpeta. No borrar la base para actualizar el código.

## 8. Probar el negocio

1. Entrar en **Clientes**, crear un cliente y comprobar búsqueda y edición.
2. Crear un tratamiento y un profesional; asignar el tratamiento al profesional.
3. Registrar un producto con existencias y mínimo de inventario.
4. Crear una cita con fecha futura, horario laboral y anticipo suficiente. Intentar otra en el mismo horario: debe rechazar el cruce.
5. Registrar una venta y comprobar el descuento de inventario y las estadísticas.
6. Volver a **Inicio** y revisar el resumen del día y los productos por reponer.
7. Cerrar y abrir la aplicación: los datos deben mantenerse.

El correo de factura sigue siendo simulado. Los archivos locales y las bases de datos no se suben a Git.

## 9. Tomar una foto y usar el probador

1. Crear primero el cliente en **Clientes**. Si el probador ya estaba abierto, volver a él: la lista se actualiza sin reiniciar. También se refresca al abrir el selector.
2. En **Probador virtual**, buscar y seleccionar el cliente por nombre o documento.
3. Pulsar **Tomar foto**. Se abre una ventana con el selector de cámara y la vista previa. Elegir la webcam física cuando aparezcan cámaras virtuales.
4. Encuadrar al cliente y pulsar **Capturar**. Usar **Volver a tomar** si hace falta repetirla.
5. Pulsar **Usar esta foto**: la imagen queda seleccionada directamente en el probador, sin buscar un archivo. **Cancelar** conserva la foto que ya estuviera seleccionada. La cámara se libera al cerrar la ventana.
6. Elegir una de las 15 muestras circulares; el borde y el nombre inferior indican la selección. Ajustar **Intensidad**.
7. Pulsar **Generar simulación** y esperar. Las entradas se bloquean durante la petición para no mezclar solicitudes.
8. Revisar **Antes / Después**. Si las proporciones coinciden aparece un comparador deslizante; si la API devuelve otra proporción, comparar con la referencia del panel izquierdo.
9. Pulsar **Guardar** y elegir dónde exportar el PNG. El archivo conserva el aviso de simulación.

También está disponible **Subir foto** para JPG o PNG de hasta 10 MB y 4096 píxeles por lado. Cambiar cliente, foto, tono o intensidad invalida el resultado anterior hasta generar uno nuevo.

Las fotos de cámara aceptadas se guardan localmente en `data/session/captures/`. Abrir la cámara y cancelar no guarda una foto. La vista previa y la captura no envían imágenes a Internet: la foto se envía al proveedor únicamente al solicitar una generación en modo API.

En **modo simulado** se aplica un tinte de ejemplo a la imagen completa; no detecta ni recolorea exclusivamente el cabello. El aviso del modo permite distinguirlo de la generación con API. El selector actual ofrece color de cabello; corte, maquillaje y uñas no están implementados.

## 10. Problemas habituales

| Problema | Qué revisar |
|---|---|
| `java` o `mvn` no se reconoce | JDK/Maven instalados, `JAVA_HOME` y `PATH`; abrir otra terminal |
| Error al compilar por versión de Java | `mvn -version` debe mostrar JDK 21 o superior |
| Aparece modo simulado aunque recibí el archivo | Nombre y ubicación de `config.properties`, proveedor `api`, variable de entorno en la misma terminal y acceso a Internet |
| La API no genera | Clave válida, acceso a los modelos, facturación y conexión; el mensaje de error no debe incluir la clave |
| No aparece el cliente | Guardarlo primero en Clientes y volver al probador o abrir de nuevo el selector |
| No se encuentra la cámara | Conectar la webcam, elegir la fuente correcta, revisar permisos y cerrar otras aplicaciones que la estén usando; Subir foto sigue disponible |
| La cámara abre sin imagen | Evitar una cámara virtual sin fuente; seleccionar otra cámara o cerrar y reabrir la ventana |
| Error de base de datos al iniciar | Comprobar permiso de escritura en la carpeta del proyecto y que no haya otra herramienta bloqueando SQLite; conservar el respaldo antes de cambiar datos |
| La ventana sigue mostrando código anterior | Cerrar la instancia anterior y volver a ejecutar `mvn javafx:run` |

En Windows 11, revisar **Configuración → Privacidad y seguridad → Cámara** y el acceso de las aplicaciones de escritorio. Los controles pueden variar según la versión de Windows. Ver la [guía de permisos de Microsoft](https://support.microsoft.com/en-us/windows/privacy/manage-app-permissions-for-a-camera-in-windows).

## 11. Pruebas y consola

```powershell
mvn clean verify
mvn "-Dtest=CameraCaptureDialogTest,TryOnViewTest,SqliteIntegrationTest" test
mvn -q exec:java "-Dexec.args=demo"
mvn -q exec:java "-Dexec.args=menu"
```

La demo automática trabaja sobre una carpeta temporal. El menú de consola utiliza la misma carpeta de sesión que JavaFX. Las capturas de revisión visual se generan en `target/visual-qa/`.

## 12. Estructura y documentación

- `src/main/java/com/maisonglow/AppContext.java`: composición de servicios y DAOs SQLite.
- `src/main/java/com/maisonglow/dao/`: contratos, SQLite e importación compatible con archivos antiguos.
- `src/main/java/com/maisonglow/service/`: reglas y operaciones del negocio.
- `src/main/java/com/maisonglow/view/`: navegación, controladores, componentes y cámara.
- `src/main/java/com/maisonglow/ai/`: proveedor simulado, adaptadores API y herramientas autorizadas.
- `src/main/resources/`: vistas FXML, estilos e imágenes.
- `sql/`: esquema, valores predeterminados y semilla de demostración.
- [Pruebas de SQLite y probador](docs/fase3/pruebas-sqlite-probador.md).
- [Revisión visual con Impeccable](docs/fase3/revision-probador-impeccable.md).
- [Decisiones de arquitectura](docs/adr/README.md), [roadmap](docs/roadmap-corte3.md) y [equipo](TEAM.md).

## Desarrollo

Crear ramas con prefijos `feature/`, `fix/`, `chore/` o `docs/`. Usar commits convencionales (`feat`, `fix`, `docs`, `test`, etc.). Los cambios se revisan mediante pull request hacia `develop`; no subir directamente a `main` o `develop`. Antes de enviar, ejecutar `mvn clean verify` y revisar que no haya configuración privada, claves, bases de datos ni fotos de clientes en el commit.

## Licencia

[MIT](LICENSE).
