# ADR-0015: Selección segura del proveedor de IA

- **Estado:** Aceptado
- **Fecha:** 2026-11-13
- **Decide:** Samuel David Rangel (S-14)

## Contexto

S-12 y S-13 aportan implementaciones simuladas. Los adaptadores HTTP se desarrollan en S-15 y S-16. La aplicación debe arrancar sin claves ni red; la interfaz necesita saber si está mostrando resultados simulados.

## Decisión

`AiConfig` lee `config.properties` opcional. `ai.provider=api` solicita el proveedor externo y `ai.api_key_env` nombra la variable de entorno de la clave (por defecto `MAISON_GLOW_AI_API_KEY`). La clave no se escribe en el archivo ni en el repositorio. Sin archivo, clave o adaptadores, `AiProviderFactory` crea el asistente y el probador simulados. Una prueba de disponibilidad acotada, proporcionada por los futuros adaptadores, decide si se puede activar el par API. Si falla, se usa el par simulado. `AiModule.isSimulated()` permite mostrar el aviso en la UI.

La fábrica acepta proveedores de ambos servicios juntos: evita presentar una función como real y la otra como simulada bajo un único indicador. El proveedor comercial y sus endpoints se elegirán en S-15/S-16, cuando se implementen las llamadas reales. Hasta entonces `ai.provider=api` cae en modo simulado.

S-15 adopta la [Responses API de OpenAI](https://developers.openai.com/api/reference/resources/responses/methods/create) para el asistente. Las herramientas siguen siendo locales y pasan por `ToolRegistry`; la [guía oficial de llamadas a funciones](https://developers.openai.com/api/docs/guides/function-calling) describe el intercambio de `function_call` y `function_call_output`. El adaptador exige `api.openai.com` por HTTPS. El probador conserva el modo simulado hasta S-16.

`AppContext.ai()` se conectará cuando I-12 agregue `AppContext` a `develop`. Este PR define el contrato y la fábrica sin crear una segunda raíz de composición que compita con Isabella.

## Consecuencias

- La aplicación puede construir el módulo de IA sin configuración externa.
- Las pruebas cubren simulación, configuración con clave, selección del par API y caída por falta de red o adaptadores.
- El indicador `isSimulated()` corresponde a las dos funciones en conjunto.
