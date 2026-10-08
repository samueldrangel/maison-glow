# ADR-0008: Manejo de excepciones y validaciones

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel

## Contexto
Los requisitos RF-24, RF-25 y RNF-09 piden validar datos y manejar errores. Sin una política común cada integrante lo hará distinto.

## Decisión
Jerarquía propia (todas `extends Exception`, es decir *checked*, para forzar su manejo):

| Excepción | Se lanza en | Significado |
|---|---|---|
| `ValidacionException` | `servicio` / `util.Validador` | Dato de entrada inválido (correo mal formado, precio negativo) |
| `ReglaNegocioException` | `servicio` | Regla incumplida (horario ocupado, stock insuficiente) |
| `AccesoDatosException` | `dao` | Fallo de BD; envuelve la `SQLException` original como causa |

Normas:
1. La **validación ocurre en la capa de servicio**, no solo en la UI (el asistente y otros clientes también la necesitan).
2. Los DAO nunca muestran mensajes; solo lanzan `AccesoDatosException`.
3. La **vista** es la única que captura y muestra al usuario (`JOptionPane`) mensajes legibles, sin trazas técnicas.
4. Prohibido el `catch` vacío y `printStackTrace()` como manejo.
5. Operaciones multi-tabla (venta + detalle + stock) usan **transacción** (`commit`/`rollback`).

## Alternativas consideradas
- **Excepciones *unchecked*:** más cómodas, pero es más fácil olvidar manejarlas.
- **Devolver códigos de error / `boolean`:** pierde el motivo del fallo.

## Consecuencias
- (+) Mensajes consistentes y fáciles de trazar a un tipo de error.
- (+) Cumple la rúbrica de manejo de excepciones en acceso a datos.
- (−) Más `throws` en las firmas; se acepta por claridad.
