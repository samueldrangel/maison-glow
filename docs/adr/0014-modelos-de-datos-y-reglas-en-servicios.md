# ADR-0014: Modelos de datos y reglas en servicios

- **Estado:** Aceptado
- **Fecha:** 2026-11-11
- **Contexto:** observación del docente sobre lógica de negocio dentro de las entidades durante la sustentación.

## Decisión

`model` representa datos y relaciones. Puede proteger su estructura (referencias obligatorias, colecciones de solo lectura), definir identidad y mostrar una descripción. Las decisiones de negocio, cálculos, cambios coordinados y validaciones de entrada pertenecen a `service` y `service.rule`.

| Responsabilidad | Propietario actual |
|---|---|
| Crear y reprogramar citas; calcular fin, cruces y disponibilidad | `AppointmentService` |
| Validar transiciones, anticipo e inasistencias | `AppointmentService` y estrategias de `service.rule` |
| Consultar y modificar existencias y detectar stock bajo | `ProductService` |
| Agrupar líneas, fijar precio histórico, sumar subtotales y total, enlazar cita | `SaleService` |
| Asignar y retirar tratamientos de profesionales | `ProfessionalService` |
| Leer y escribir archivos y reconstruir relaciones | DAO |

Las entidades ya no ofrecen `deductStock`, `calculateTotal`, `markNoShow`, `worksDuring` ni reglas de transición. `Sellable` contiene solamente identidad, nombre y precio; no impone inventario a los tratamientos. La presentación invoca los servicios. Los DAO reconstruyen datos sin llamar a reglas ni servicios.

## Correcciones complementarias

- Los importes (precios, anticipos, totales y estadísticas monetarias) usan `BigDecimal`, también al leer y escribir los archivos. Para entradas exactas se utiliza `new BigDecimal("0.10")`. Las sobrecargas con `double` conservan la comodidad de las llamadas existentes y convierten inmediatamente con `BigDecimal.valueOf`; rechazan números no finitos. Anticipos porcentuales y promociones se redondean a dos decimales con `HALF_UP`.
- Los totales se calculan en el servicio y se conservan en `Sale` como datos. El DAO guarda y recupera el total histórico.
- Las entidades con id generado solo comparan ids positivos. Dos objetos nuevos distintos con id 0 son diferentes. Su hash permanece estable cuando el DAO asigna el id.
- Los DAO devuelven objetos reconstruidos a partir de instantáneas serializadas. Modificar un objeto no equivale a guardarlo.
- El servicio de ventas consulta el stock persistido antes de descontarlo; un objeto desactualizado no puede sobrescribirlo.
- Los errores de escritura provocan recuperación del estado previo. Los límites de las compensaciones entre archivos se explican en [ADR-0012](0012-persistencia-en-archivos-de-texto.md).

## Validación

`ModelBoundaryTest` protege la ausencia de operaciones de negocio en las entidades. `BusinessPracticeRegressionTest` cubre identidad temporal, decimales, aislamiento del DAO, errores de escritura, recuperación de inasistencias y ventas con stock desactualizado. Las pruebas anteriores de cálculos y reglas ahora invocan servicios.

Ejecutar `mvn clean verify` para compilar, probar y comprobar el umbral de cobertura. Mockito y Byte Buddy están alineados para ejecutar también con Java 24; el código fuente conserva Java 21 como versión mínima.
