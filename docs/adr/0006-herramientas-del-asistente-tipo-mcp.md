# ADR-0006: Asistente con herramientas controladas (estilo MCP)

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Equipo completo

## Contexto
El asistente debe consultar y ejecutar operaciones solo mediante herramientas previamente definidas (RF-22, RF-23, RNF-14). El enunciado menciona Model Context Protocol (MCP). Implementar un servidor MCP completo (JSON-RPC, transporte stdio/HTTP) excede el nivel del curso y depende de SDK que podrían no ser estables en Java.

## Decisión
Adoptar **el concepto de MCP** (herramientas con nombre, descripción y parámetros declarados) con una implementación sencilla y propia:

- Interfaz `HerramientaAsistente` con `getNombre()`, `getDescripcion()`, `getParametros()` y `ejecutar(Map<String,String>)`.
- Una herramienta por clase: `ConsultarStockBajo`, `ConsultarCitasPorFecha`, `BuscarCliente`, `VerificarDisponibilidad`, `CrearCita`.
- `RegistroHerramientas` (un `HashMap<String, HerramientaAsistente>`) es la **lista blanca**: lo que no está registrado no se puede ejecutar.
- Las herramientas **llaman a los `*Servicio`**, nunca a los DAO, por lo que heredan todas las validaciones y reglas de negocio.
- Las herramientas que modifican datos (`CrearCita`) piden confirmación del usuario en la UI antes de ejecutarse.
- **Evolución opcional:** si sobra tiempo, exponer el mismo registro mediante un adaptador MCP real sin cambiar las herramientas.

## Alternativas consideradas
- **Servidor MCP completo:** alto riesgo y poco valor académico adicional.
- **El modelo ejecuta SQL libre:** inseguro, viola RNF-14.

## Consecuencias
- (+) Agregar una herramienta no modifica el resto del asistente (OCP).
- (+) Seguridad por construcción: lista blanca + validaciones del servicio.
- (−) No es "MCP estricto"; se documenta como "diseño inspirado en MCP".
