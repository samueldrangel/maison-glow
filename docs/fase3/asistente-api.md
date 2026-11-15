# S-15 — Adaptador HTTP del asistente

`ApiAssistant` usa `java.net.http.HttpClient` y la Responses API de OpenAI. El constructor recibe cliente HTTP, endpoint HTTPS de `api.openai.com`, modelo, clave y `ToolRegistry`, lo que permite probar el transporte sin red. La clave proviene de `AiConfig` y no se registra en los mensajes de error.

En cada petición envía el mensaje y las descripciones de las herramientas registradas. Solo ejecuta llamadas que `ToolRegistry` permita. Una consulta devuelve su resultado al modelo con `function_call_output`; una operación que modifica datos retorna `pendingConfirmation` para que la vista muestre Confirmar/Cancelar antes de llamar a `confirm` o `reject`.

El tiempo máximo por petición es de 15 segundos y el intercambio se limita a tres rondas. HTTP no exitoso, timeout o respuesta inválida producen un `AssistantReply` claro. S-16 conecta el adaptador a `AiProviderFactory` junto con el probador API; consulta la [activación y el fallback](probador-api.md).

Referencias: [Responses API](https://developers.openai.com/api/reference/resources/responses/methods/create) y [llamadas a funciones](https://developers.openai.com/api/docs/guides/function-calling).
