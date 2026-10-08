# ADR-0001: Arquitectura en tres capas

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel (líder), con Nicoll Gómez e Isabella Celedón

## Contexto
La asignatura exige arquitectura en 3 capas, patrón DAO, GRASP y SOLID (RNF-01, RNF-08). El equipo es de tres estudiantes, por lo que la arquitectura debe ser comprensible y no añadir ceremonia innecesaria.

## Decisión
Tres capas con dependencia en una sola dirección:

1. **Presentación** (`vista`): ventanas Swing. Solo captura datos y muestra resultados.
2. **Negocio** (`servicio`, `modelo`, `servicio.regla`): reglas, validaciones y coordinación. Las clases `*Servicio` hacen de *Controller* GRASP.
3. **Datos** (`dao`): acceso JDBC a la base de datos.

Regla: `vista → servicio → dao`. La vista no conoce SQL ni DAO. Las entidades (`modelo`) son compartidas por todas las capas. Se evita una capa extra de "controlador" separada del servicio para no duplicar clases.

## Alternativas consideradas
- **MVC clásico con controlador propio:** añade una clase por módulo sin aportar claridad en este tamaño de proyecto.
- **Arquitectura hexagonal / limpia:** correcta, pero demasiado compleja para el nivel del curso.
- **Monolito sin capas:** incumple los requisitos.

## Consecuencias
- (+) Fácil de explicar, dibujar y evaluar; permite repartir trabajo por módulo.
- (+) Cada capa se puede probar de forma aislada.
- (−) Las entidades de dominio se usan también en la UI (sin DTOs); aceptable a esta escala.
