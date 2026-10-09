# ADR-0012: Persistencia en archivos de texto en la Entrega 1

- **Estado:** Aceptado (reemplaza al [ADR-0010](0010-dao-en-memoria-primero.md))
- **Fecha:** 2026-10-08
- **Decide:** Equipo completo, siguiendo la recomendación del docente

## Contexto
La primera entrega pide toda la lógica de negocio sin base de datos. El ADR-0010 proponía DAO en memoria; el docente recomienda **archivos de texto**, que además hacen que los datos sobrevivan entre ejecuciones sin añadir complejidad de SQL.

## Decisión
1. **Un archivo `.txt` por entidad** en la carpeta `data/` (`customers.txt`, `professionals.txt`, `beauty_services.txt`, `products.txt`, `appointments.txt`, `sales.txt`, `settings.txt`), codificación UTF-8.
2. **Una línea por registro**, campos separados por `|`. El carácter `|` y la barra invertida dentro de un texto se escapan con `\|` y `\\`; los saltos de línea no se permiten (el `Validator` los rechaza).
3. **Clase base `TextFileDao<T>`** (tarea S-03 del roadmap) que implementa `CrudDao<T>`: carga el archivo al construirse, mantiene un `Map<Integer,T>` en memoria, **guarda tras cada cambio** y asigna ids. Cada DAO concreto solo define `toFields(T)`, `fromFields(String[])` y cómo leer/asignar el id.
4. **Ruta por constructor** para que las pruebas usen archivos temporales sin tocar `data/`.
5. **Referencias entre entidades por id:** una cita guarda `customerId`, `professionalId` y `serviceId`; al cargar, su DAO recibe por constructor los DAO de esas entidades (interfaces `CrudDao`) para reconstruir los objetos. Es una dependencia entre DAO de la misma capa, nunca hacia los servicios.
6. **Estructura de campos = columnas del esquema** ([`sql/schema.sql`](../../sql/schema.sql)); el orden y los nombres son los mismos, de modo que el paso a SQLite no cambia el modelo.
7. **Datos iniciales:** `data/settings.txt` viene precargado con la configuración de `sql/seed.sql`. Los demás archivos nacen vacíos y están en `.gitignore`.
8. **Excepciones:** los errores de lectura/escritura se envuelven en `DataAccessException` (ADR-0008).

## Enmienda (2026-10-09): archivos para todas las tablas D1 del esquema
Para mantener la coherencia con `sql/schema.sql` ([ADR-0011](0011-decisiones-de-diseno-de-la-base-de-datos.md)) se agregan a la lista de archivos de la decisión 1:
- `categories.txt` (tabla `product_category`), precargado con las categorías de `sql/seed.sql` y versionado como `settings.txt`.
- `professional_services.txt` (tabla `professional_service`, relación N:M), manejado por `ProfessionalDaoText`.
- `sale_lines.txt` (tabla `sale_line`), manejado por `SaleDaoText`, que guarda la venta y sus líneas juntas.
- `settings.txt` (tabla `app_setting`) tiene clave de texto: su `SettingDao` no extiende `CrudDao` y reutiliza el escape de `TextFileDao`.
- El stock se modifica directamente en `Product` durante la Entrega 1; los `stock_movement` llegan con la Entrega 2.

## Alternativas consideradas
- **Solo memoria (ADR-0010):** más simple, pero los datos se pierden al cerrar y no demuestra persistencia.
- **CSV con comas:** el nombre o la descripción suele contener comas; `|` evita la mayoría de choques.
- **Serialización de Java:** no es legible ni versionable y se rompe al cambiar las clases.

## Consecuencias
- (+) Persistencia real y legible a bajo costo, alineada con la recomendación del docente.
- (+) Los servicios no cambian al pasar a SQLite: solo se sustituye el DAO inyectado en el punto de arranque (ADR-0003).
- (−) Cada DAO debe escribir su conversión de/a campos; la clase base reduce el costo.
- (−) Sin transacciones: una venta que descuenta stock debe validar todo **antes** de modificar cualquier archivo.
