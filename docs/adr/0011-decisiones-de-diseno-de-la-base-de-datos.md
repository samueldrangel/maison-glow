# ADR-0011: Decisiones de diseño de la base de datos

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Equipo completo (propone Samuel Rangel)

## Contexto
Aunque la Entrega 1 no usa base de datos ([ADR-0012](0012-persistencia-en-archivos-de-texto.md)), el modelo debe nacer coherente con ella y cubrir **todo el proyecto** (RF-01 a RF-31, roles, IA). El esquema vive en `sql/schema.sql` como diseño; se aplica en la Entrega 2 ([ADR-0003](0003-persistencia-sqlite-jdbc-dao.md)).

## Decisión
1. **Cobertura completa desde el inicio:** 22 tablas, 4 vistas y 3 triggers, marcados por entrega (D1, D2, AI).
2. **Ítems vendibles en `sale_line`:** dos columnas (`product_id`, `service_id`) con `CHECK` de exclusión mutua, en lugar de una tabla por tipo o un par `(tipo, id)` sin clave foránea. Es la forma relacional de la interfaz `Sellable`: conserva integridad referencial y permite `JOIN` directos para las estadísticas.
3. **Stock solo por movimientos:** `product.stock` se modifica únicamente insertando en `stock_movement`; un trigger lo aplica y `CHECK (stock >= 0)` aborta la venta si no alcanza. Da trazabilidad y hace "todo o nada" la venta dentro de una transacción.
4. **Anti-cruce de citas en la BD:** trigger por profesional sobre citas `PENDING`/`CONFIRMED`. Es una **segunda barrera**; la regla principal vive en `AppointmentService` (RF-11/12), que da el mensaje al usuario.
5. **`end_at` almacenado:** es derivado (inicio + duración), pero se guarda para poder comprobar cruces con una consulta simple. El modelo lo calcula con `BeautyService.calculateEnd()`.
6. **Precio congelado:** `sale_line.unit_price` copia el precio al vender; cambiar el catálogo no altera ventas pasadas.
7. **Fechas y horas como texto ISO-8601:** SQLite no tiene tipo fecha y el formato ISO se ordena y compara correctamente como texto, y coincide con `java.time`.
8. **Reglas configurables en `app_setting`** (clave/valor) en lugar de columnas o código fijo (RF-26).
9. **Roles en `app_user`** (`ADMIN`, `CUSTOMER`) con `CHECK` que obliga a enlazar un cliente solo si el rol es `CUSTOMER`. Contraseñas con hash.
10. **Auditoría de IA:** `assistant_tool_call` registra cada herramienta solicitada, su estado y si exigió confirmación (RNF-14).
11. **Nombres de tablas y columnas en inglés** y en `snake_case`, igual que el código.

## Alternativas consideradas
- **Herencia de tabla única o una tabla por tipo para `Sellable`:** más columnas nulas o más joins, sin ventaja para este alcance.
- **Guardar el stock solo como columna actualizada por la aplicación:** más simple, pero sin historial y propenso a inconsistencias si dos operaciones coinciden.
- **Horario por día de la semana para profesionales:** más realista, pero el modelo actual usa una jornada diaria única (`workStart`/`workEnd`); queda como mejora futura.
- **Facturación electrónica y pasarelas de pago:** fuera del alcance (ver `PLANIFICACION.md`).

## Consecuencias
- (+) Un único diseño guía los archivos de texto de la Entrega 1, el JDBC de la Entrega 2 y los módulos de IA.
- (+) Las reglas críticas tienen doble protección (servicio y BD).
- (−) El esquema es más grande de lo que la Entrega 1 usa; las tablas D2 y AI no se implementan hasta la Entrega 2.
- (−) El esquema no está ejecutado hasta la Entrega 2; hay que validarlo entonces con SQLite y ajustar si surge algún error.
