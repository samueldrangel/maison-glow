# ADR-0001: Arquitectura en tres capas

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel (líder), con Nicoll Gómez e Isabella Celedón

## Contexto
La asignatura exige arquitectura en 3 capas, patrón DAO, GRASP y SOLID (RNF-01, RNF-08). El equipo es de tres estudiantes, por lo que la arquitectura debe ser comprensible y no añadir ceremonia innecesaria.

## Decisión
Tres capas con dependencia en una sola dirección:

1. **Presentación** (`Main` y `ui.console`; JavaFX previsto): captura datos y muestra resultados.
2. **Negocio** (`service`, `service.rule`): reglas, cálculos, validaciones y coordinación. Las clases `*Service` hacen de *Controller* GRASP.
3. **Datos** (`dao`): archivos de texto en la Entrega 1; JDBC en la Entrega 2.

Regla: `presentación → service → dao`. La presentación no accede directamente a la persistencia. Las entidades (`model`) son estructuras de datos compartidas: atributos, constructores, acceso al estado, identidad y validaciones estructurales básicas. No ejecutan reglas de negocio ni modifican otras entidades. Los DAO no dependen de servicios. Se evita una capa extra de "controlador" separada del servicio para no duplicar clases.

## Alternativas consideradas
- **MVC clásico con controlador propio:** añade una clase por módulo sin aportar claridad en este tamaño de proyecto.
- **Arquitectura hexagonal / limpia:** correcta, pero demasiado compleja para el nivel del curso.
- **Monolito sin capas:** incumple los requisitos.

## Consecuencias
- (+) Fácil de explicar, dibujar y evaluar; permite repartir trabajo por módulo.
- (+) Cada capa se puede probar de forma aislada.
- (−) Las entidades de dominio se usan también en la UI (sin DTOs); aceptable a esta escala.

## Enmienda (2026-11-11)

Se centraliza el comportamiento en los servicios siguiendo la observación de la sustentación. La distribución vigente y sus pruebas se describen en [ADR-0014](0014-modelos-de-datos-y-reglas-en-servicios.md). El modelo queda fuera del paquete que implementa las reglas de negocio.
