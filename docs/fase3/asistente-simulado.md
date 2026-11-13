# S-12 — Asistente simulado con herramientas

Responsable: Samuel David Rangel. Rama: `feature/ai-assistant`.

El asistente funciona sin conexión y usa únicamente servicios del negocio. No necesita claves ni dependencias nuevas. Implementa los contratos `SmartAssistant`, `AssistantReply` y `PendingAction` del roadmap del Corte 3.

## Herramientas

| Nombre | Parámetros | Confirmación |
|---|---|---|
| `find_customer` | `name` | No |
| `list_low_stock` | Ninguno | No |
| `list_appointments_by_date` | `date` (`yyyy-MM-dd`) | No |
| `check_availability` | `professionalId`, `start`, `end` (`yyyy-MM-ddTHH:mm`) | No |
| `book_appointment` | `customerId`, `professionalId`, `treatmentId`, `start`, `deposit` (opcional, 0 por defecto) | Sí |

Los identificadores deben corresponder a registros existentes. Los importes usan punto decimal, sin separadores de miles. La reserva pasa por `AppointmentService.book`: conserva las reglas de anticipo, jornada, tratamientos y cruces de horarios.

## Uso desde Java

El punto que arma la aplicación proporciona los servicios existentes. En S-14 se conectará mediante `AppContext.ai()`; esta tarea no cambia el arranque ni las vistas de Isabella y Nicoll.

```java
SmartAssistant assistant = new SimulatedAssistant(new ToolRegistry(List.of(
    new FindCustomerTool(customers),
    new ListLowStockTool(products),
    new ListAppointmentsByDateTool(appointments),
    new CheckAvailabilityTool(appointments, professionals),
    new BookAppointmentTool(appointments, customers, professionals, treatments)
)));

AssistantReply reply = assistant.reply("stock bajo");
System.out.println(reply.text());

AssistantReply proposal = assistant.reply(
    "book_appointment customerId=1; professionalId=2; treatmentId=3; "
        + "start=2026-12-01T09:00; deposit=10000.50");
// Mostrar proposal.text() y botones Confirmar / Cancelar en la futura vista.
// Solo al pulsar Confirmar:
proposal.pendingConfirmation().ifPresent(action -> {
    AssistantReply result = assistant.confirm(action);
    System.out.println(result.text());
});
// Al pulsar Cancelar, llamar assistant.reject(action) en su lugar.
```

El modo simulado reconoce `hola`, `ayuda`, `stock bajo`, `buscar cliente Ana Ruiz` y `citas 2026-12-01`. También acepta el nombre exacto de cualquier herramienta registrada seguido de `parámetro=valor`, separado por punto y coma. No interpreta lenguaje natural libre; el adaptador por API corresponde a S-15.

## Confirmación y errores

`ToolRegistry` rechaza herramientas desconocidas y parámetros no declarados. Las operaciones que requieren confirmación devuelven una propuesta con parámetros inmutables y un identificador de sesión. Confirmar consume esa propuesta una sola vez; cancelar la descarta. Una propuesta modificada o emitida por otro registro se rechaza. Si una operación falla, hay que solicitar una propuesta nueva: no se reintenta automáticamente una escritura.

La disponibilidad se comprueba otra vez al reservar, después de confirmar. Una consulta previa de disponibilidad no garantiza que el horario siga libre. Los errores de validación y negocio se devuelven como texto; los errores de almacenamiento se convierten en un mensaje controlado, sin revelar rutas internas.

## Verificación

```bash
mvn test -Dtest=ToolRegistryTest,SimulatedAssistantTest
mvn clean verify
```

Las pruebas con Mockito verifican lista blanca, consultas, parámetros inválidos, confirmación y cancelación, rechazo de propuestas alteradas o ajenas, ejecución única, errores de almacenamiento y reservas con el `AppointmentService` real, incluidas sus reglas y conflictos.

En la verificación local con JDK 24 se precargó el agente de Mockito para evitar que su autoacoplamiento detuviera las pruebas. En PowerShell, después de descargar las dependencias con Maven:

```powershell
$env:JAVA_TOOL_OPTIONS = "-javaagent:$env:USERPROFILE\.m2\repository\org\mockito\mockito-core\5.18.0\mockito-core-5.18.0.jar"
mvn clean verify
```

La opción se aplica solo a esa terminal. No cambia el `pom.xml` ni el mínimo Java 21 del proyecto.

S-12 queda pendiente de revisión y fusión a `develop`. S-13 (probador), S-14 (configuración), adaptadores por API, persistencia y entrega final conservan su alcance en el roadmap.
