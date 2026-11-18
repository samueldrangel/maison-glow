# SQLite y probador virtual

## Arranque y datos

Ejecutar desde la raíz del proyecto con Java 21 o superior y Maven:

```powershell
mvn javafx:run
```

La aplicación crea `data/session/maison-glow.db` con el esquema incluido en el JAR. No requiere un servidor ni ejecutar SQL manualmente. Clientes, profesionales y sus tratamientos, catálogo, productos, citas, ventas, líneas y configuración se leen y escriben mediante DAOs JDBC. Los servicios y todas las pantallas comparten esa conexión lógica. Cada operación abre una conexión con claves foráneas activadas; una venta y su descuento de existencias se confirman juntos.

Los importes se guardan como decimales de texto y se reconstruyen como `BigDecimal`, para conservar exactamente precios, anticipos y totales. Las estadísticas de la aplicación se calculan en los servicios con esos valores; las vistas SQL que usan `SUM` son consultas orientativas de SQLite.

Al abrir una carpeta por primera vez se importan sus archivos de texto del negocio en una transacción. Conserva identificadores, relaciones, configuración y el contador del siguiente ID. Si un archivo no es válido, se revierte la importación y el arranque indica el error. Después de corregirlo, se puede reintentar. `app_metadata.legacy_import_v1` impide repetir la importación. Los archivos originales no se borran ni se modifican.

`sql/seed.sql` contiene configuración y categorías. `sql/demo-seed.sql` añade tres clientes ficticios identificados con `(demo)`, tres tratamientos, un profesional y tres productos. Se aplica una sola vez a la sesión, con la marca `demo_seed_v1`; no restablece modificaciones ni vuelve a crear datos que el usuario haya borrado. Los scripts usan `INSERT OR IGNORE` y también pueden ejecutarse repetidamente sin duplicar registros.

El historial de IA de la primera entrega (`AiHistory`) conserva su formato de texto. Esta migración conecta la persistencia del negocio usada por las pantallas, el asistente y la validación del cliente del probador.

## Recorrido de prueba

1. Abrir **Clientes** y registrar una persona. Si el probador ya estaba abierto, volver a **Probador virtual**: el cliente debe aparecer sin reiniciar. El desplegable también refresca los datos al abrirse y permite buscar por nombre o documento.
2. Elegir el cliente. Pulsar **Tomar foto**, seleccionar la cámara, capturar, repetir si hace falta y confirmar con **Usar esta foto**; la imagen queda seleccionada directamente. Cancelar conserva la referencia anterior. También se puede cargar un JPG o PNG de hasta 10 MB y 4096 píxeles por lado. Un archivo inválido debe mostrar el motivo bajo la foto.
3. Elegir una de las 15 muestras circulares. El borde y el nombre inferior indican la selección. Ajustar **Intensidad**.
4. Pulsar **Generar simulación**. Durante la petición se bloquean las entradas para evitar mezclar una solicitud con otra. Al terminar aparecen la comparación **Antes / Después**, el control deslizante y **Guardar**.
5. Guardar un PNG y abrirlo. Debe contener el aviso de simulación. Cambiar la foto, el cliente, el tono o la intensidad invalida el resultado anterior hasta generar uno nuevo.
6. Probar sin cliente, sin foto y sin tono: debe mostrar errores y permitir corregirlos. Crear el cliente antes de generarlo sigue siendo obligatorio.
7. Registrar una cita y una venta, cerrar y abrir la aplicación y comprobar clientes, citas, ventas y existencias. Los registros deben seguir disponibles.

Sin proveedor disponible se muestra **Modo simulado**: aplica un tinte de ejemplo a la foto completa; no detecta el cabello. El proveedor API lee la clave de `config.properties` local. Las pruebas automatizadas del adaptador API usan transporte simulado y no consumen la API real.

## Verificación

```powershell
mvn clean verify
```

La suite incluye importación y reintento, claves foráneas, semilla repetible, importes exactos, persistencia al reabrir, rollback de venta y fallo al confirmar la transacción, cliente creado con el probador en caché, generación de cada tono, intensidad, exportación y distribución de la paleta en tamaños distintos. Las capturas JavaFX se generan en `target/visual-qa/`.

## Recuperación

Con la aplicación cerrada, copiar `data/session/` para respaldarla. La versión anterior puede seguir leyendo los archivos originales, pero no verá cambios nuevos guardados en SQLite. No borrar la base para volver atrás: conservar una copia y usar una carpeta de prueba separada. La importación es unidireccional; los archivos antiguos quedan como respaldo, no como una segunda base sincronizada.
