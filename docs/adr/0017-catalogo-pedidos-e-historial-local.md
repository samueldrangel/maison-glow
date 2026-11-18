# ADR-0017: Catálogo, pedidos e historial local por cuenta

- Estado: implementado en backend, pendiente de integración visual y revisión.
- Fecha: 2026-11-17.

## Decisión

Conservar SQLite local. Separar publicación de producto de actividad/inventario, y pedido pendiente de venta pagada. Los pedidos reservan unidades durante 30 minutos; clave de reintento por cliente y operación transaccional de cobro impiden duplicación. Una restricción SQLite protege las reservas también frente a la venta anterior y ajustes de stock.

El autoservicio obtiene cliente desde la sesión y reutiliza reglas existentes de citas. La reserva propia declara cero pagado; solo administración registra dinero y confirma una cita con anticipo suficiente. El acceso al historial administrativo exige autorización revocable del cliente. Fotos aceptadas son copias gestionadas con identificadores generados; la retirada de medios se coordina mediante cola persistente de limpieza.

Los servicios anteriores conservan firmas y se construyen sobre DAO decorados que exigen ADMIN en cada operación. El autoservicio usa servicios internos sobre DAO privados del armado; no se exponen a controladores. `AccessDeniedDataException` conserva compatibilidad con las firmas antiguas. Los proveedores IA se crean por login con herramientas limitadas por rol y operaciones locales ligadas a esa sesión.

La migración 2 es aditiva y transaccional. Las conversaciones antiguas sin propietario verificado no se atribuyen por coincidencia de IDs a cuentas nuevas. Copia de seguridad abarca BD y medios; no se habilita nube ni sincronización de archivos.

Contratos completos y límites de integración: [roles-backend.md](../fase3/roles-backend.md).
