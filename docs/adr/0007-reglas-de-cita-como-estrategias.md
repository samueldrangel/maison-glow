# ADR-0007: Reglas de citas como estrategias intercambiables

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel

## Contexto
RF-26 exige reglas configurables para citas (anticipo/depósito, confirmación previa, recordatorio, penalización por inasistencia). Estas reglas pueden cambiar o ampliarse según el negocio.

## Decisión
- Interfaz `ReglaCita` con `validar(Cita cita)` (lanza `ReglaNegocioException`) y `getNombre()`.
- Implementaciones: `ReglaAnticipo`, `ReglaConfirmacion`, `ReglaRecordatorio` (y `ReglaPenalizacion` si hay tiempo).
- `CitaServicio` recibe una `List<ReglaCita>` por constructor y las aplica en orden al agendar.
- El **conflicto de horario** NO es una regla configurable: es una invariante, vive en `CitaServicio`.

## Alternativas consideradas
- **`if/else` dentro de `CitaServicio`:** cada regla nueva obliga a modificar la clase (viola OCP) y la vuelve difícil de leer.
- **Reglas en la BD con motor de reglas:** exagerado.

## Consecuencias
- (+) Demuestra OCP y el patrón Strategy de forma natural; fácil de explicar.
- (+) Cada regla se prueba con un test aislado.
- (−) Más clases pequeñas (se acepta: es el objetivo de SRP).
