# ADR-0015: Selección segura del proveedor de IA

- **Estado:** Aceptado
- **Fecha:** 2026-11-13
- **Decide:** Samuel David Rangel (S-14)

## Contexto

S-12 y S-13 aportan implementaciones simuladas. Los adaptadores HTTP se desarrollan en S-15 y S-16. La aplicación debe arrancar sin claves ni red; la interfaz necesita saber si está mostrando resultados simulados.

## Decisión

`AiConfig` lee `config.properties` opcional. `ai.provider=api` solicita el proveedor externo y `ai.api_key` permite guardar la clave en ese archivo local ignorado por Git. `ai.api_key_env` conserva la alternativa de variable de entorno (por defecto `MAISON_GLOW_AI_API_KEY`); la clave local tiene prioridad. Sin archivo, clave o adaptadores, `AiProviderFactory` crea el asistente y el probador simulados. Una prueba de disponibilidad acotada decide si se puede activar el par API. Si falla, se usa el par simulado. `AiModule.isSimulated()` permite mostrar el aviso en la UI.

La fábrica acepta proveedores de ambos servicios juntos: evita presentar una función como real y la otra como simulada bajo un único indicador. S-15/S-16 implementan los adaptadores de OpenAI. La fábrica de producción comprueba `GET /v1/models/gpt-image-1.5` con timeout de tres segundos; sin disponibilidad usa el par simulado. Un fallo posterior se comunica mediante el contrato del servicio, sin cambiar silenciosamente el proveedor ni el indicador del módulo.

S-15 adopta la [Responses API de OpenAI](https://developers.openai.com/api/reference/resources/responses/methods/create) para el asistente. Las herramientas siguen siendo locales y pasan por `ToolRegistry`; la [guía oficial de llamadas a funciones](https://developers.openai.com/api/docs/guides/function-calling) describe el intercambio de `function_call` y `function_call_output`. El adaptador exige `api.openai.com` por HTTPS. S-16 adopta la [API de edición de imágenes](https://developers.openai.com/api/reference/resources/images/methods/edit), con `gpt-image-1.5`, multipart y salida PNG en base64. La fábrica conecta ambos adaptadores y usa `gpt-5.4` para el asistente. Los clientes HTTP no siguen redirecciones. Las validaciones y el rótulo permanente del probador se comparten entre ambos proveedores mediante `TryOnImages`.

`AppContext.ai()` ya utiliza esta fábrica desde I-12; S-16 completa la selección de los adaptadores sin modificar esa raíz de composición.

## Consecuencias

- La aplicación puede construir el módulo de IA sin configuración externa.
- Las pruebas cubren simulación, configuración con clave, selección del par API y caída por falta de red o adaptadores.
- El indicador `isSimulated()` corresponde a las dos funciones en conjunto.
