# S-14 — Configuración del módulo de IA

La aplicación usa `AiConfig.load()` y `AiProviderFactory`. Sin `config.properties`, el módulo crea `SimulatedAssistant` y `SimulatedTryOnService`; `AiModule.isSimulated()` devuelve `true` para que la UI muestre un aviso.

Para solicitar adaptadores externos en S-15/S-16, crea un `config.properties` local (ignorado por Git):

```properties
ai.provider=api
ai.api_key_env=MAISON_GLOW_AI_API_KEY
```

La variable indicada debe contener la clave en el entorno del proceso. Nunca pongas la clave en el archivo de propiedades. Hasta que existan ambos adaptadores, el módulo seguirá en modo simulado incluso con `ai.provider=api`.

La fábrica recibe `CustomerService`, `ProductService`, `AppointmentService`, `ProfessionalService` y `TreatmentService`, y entrega `AiModule`. Cuando Isabella fusione I-12, `AppContext.ai()` llamará a esta fábrica con sus servicios existentes; el contrato para Nicoll ya está definido. La decisión y el límite temporal se explican en [ADR-0015](../adr/0015-seleccion-del-proveedor-de-ia.md).

Pruebas: `mvn test -Dtest=AiProviderFactoryTest` y `mvn clean verify`.
