# S-17 — Persistencia de IA en archivos de texto

`AiHistory` guarda cuatro archivos en la misma carpeta de datos de `AppContext`: `assistant_conversations.txt`, `assistant_messages.txt`, `assistant_tool_calls.txt` y `try_on_sessions.txt`. Cada registro conserva, en orden, los campos de `assistant_conversation`, `assistant_message`, `assistant_tool_call` y `try_on_session` de `src/main/resources/sql/schema.sql`. El encabezado `#nextId` conserva identificadores crecientes entre reinicios y `TextLineCodec` permite mensajes con `|`, barras invertidas y saltos de línea.

`AppContext.aiHistory()` ofrece operaciones para abrir una conversación, añadir mensajes, registrar y finalizar llamadas de herramientas, abrir y finalizar sesiones del probador, y consultar esos datos. Se comprueba que los mensajes tengan una conversación y que las llamadas de herramientas pertenezcan a un mensaje del asistente. Cada archivo se reemplaza de forma atómica al escribir. No hay transacción entre archivos: cada llamada del repositorio guarda un registro de una tabla.

El campo `user_id` reserva el identificador de `app_user`, previsto para una entrega posterior. El historial guarda rutas de imágenes, no copias de las fotos; por eso una imagen temporal que se elimina al salir de la aplicación no estará disponible en otra sesión. El flujo de interfaz de N-16 deberá guardar un resultado definitivo antes de registrar su ruta.

El historial no intercepta las respuestas de `SmartAssistant` ni las generaciones de `TryOnService`: N-16 deberá llamar a `aiHistory()` desde sus controladores cuando el usuario use esas vistas. Esto evita crear registros de una conversación antes de que exista la vista y permite conservar explícitamente confirmaciones y rechazos.

Pruebas: `AiHistoryTest` crea y recarga una conversación con mensajes y una llamada de herramienta, y sesiones exitosas/fallidas; verifica encabezados de ID y errores de referencias ausentes.
