# S-13 — Probador virtual simulado

Responsable: Samuel David Rangel. Rama: `feature/ai-try-on`.

`TryOnService.generate(TryOnRequest)` recibe un identificador de cliente, una foto local y una transformación. El servicio simulado consulta `CustomerService.findById`, acepta una foto PNG o JPEG de hasta 10 MB y 4096 píxeles por lado, y genera un PNG nuevo en el directorio temporal del sistema. La foto original no se modifica. El resultado lleva un rótulo visible y `TryOnResult.disclaimer()` devuelve `simulación, no garantía`.

Transformaciones disponibles: `rojo cobrizo`, `rubio dorado` y `castaño oscuro`. La UI puede obtenerlas mediante `SimulatedTryOnService.supportedTransformations()` mientras no exista el proveedor configurable de S-14. El modo simulado aplica un tinte a la foto completa: sirve para demostrar el flujo, pero no aísla el cabello ni predice un resultado estético real.

```java
TryOnService tryOn = new SimulatedTryOnService(customers);
TryOnResult result = tryOn.generate(new TryOnRequest(1, Path.of("foto.png"), "rojo cobrizo"));
// Mostrar result.image() y result.disclaimer() en la futura vista JavaFX.
```

Una solicitud sin cliente, con cliente no registrado, foto ausente/ilegible o transformación no admitida lanza `ValidationException`. Los errores de lectura del almacenamiento del cliente conservan `DataAccessException`. El PNG temporal se marca para eliminación al terminar la JVM; S-17 definirá la persistencia de sesiones.

Verificación: `mvn test -Dtest=SimulatedTryOnServiceTest` y `mvn clean verify`. En JDK 24 se precarga el agente de Mockito según [S-12](asistente-simulado.md#verificación).

S-13 queda pendiente de revisión y fusión a `develop`. S-14 conectará este contrato con `AppContext.ai()`.
