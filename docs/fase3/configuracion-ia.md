# S-14 — Configuración del módulo de IA

La aplicación usa `AiConfig.load()` y `AiProviderFactory`. Sin `config.properties`, el módulo crea `SimulatedAssistant` y `SimulatedTryOnService`; `AiModule.isSimulated()` devuelve `true` para que la UI muestre un aviso.

Para solicitar los adaptadores externos, crea un `config.properties` local (ignorado por Git):

```properties
ai.provider=api
ai.api_key=TU_CLAVE_API
```

El archivo queda solo en el equipo y está ignorado por Git. También se admite `ai.api_key_env=MAISON_GLOW_AI_API_KEY` si se prefiere una variable de entorno; `ai.api_key` tiene prioridad. La fábrica comprueba disponibilidad con un límite de tres segundos y selecciona ambos adaptadores API o ambos simulados. Detalles de activación, límites y pruebas en [S-16](probador-api.md).

La fábrica recibe `CustomerService`, `ProductService`, `AppointmentService`, `ProfessionalService` y `TreatmentService`, y entrega `AiModule`. `AppContext.ai()` usa esta fábrica con sus servicios existentes. La decisión se explica en [ADR-0015](../adr/0015-seleccion-del-proveedor-de-ia.md).

Pruebas: `mvn test -Dtest=AiProviderFactoryTest` y `mvn clean verify`.
