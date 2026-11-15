# S-16 — Probador virtual por API

`ApiTryOnService` implementa `TryOnService` con `HttpClient` y la [API de edición de imágenes de OpenAI](https://developers.openai.com/api/reference/resources/images/methods/edit). Envía una petición multipart a `https://api.openai.com/v1/images/edits`, con `gpt-image-1.5`, una foto PNG y la transformación elegida. Pide una imagen PNG de 1024 × 1024 y lee `data[0].b64_json`. El transporte y el modelo se inyectan en el constructor para las pruebas.

Antes de enviar la foto, `TryOnImages` aplica las mismas validaciones que el simulador: cliente registrado, transformación admitida, PNG/JPEG de hasta 10 MB y 4096 píxeles por lado. La foto se recodifica a PNG desde los píxeles validados; no se envían el nombre original, metadatos ni datos del cliente. La imagen original permanece intacta.

El resultado se valida antes de decodificar sus píxeles y se guarda en un PNG temporal con el rótulo **«simulación, no garantía»** añadido localmente. El archivo se elimina al salir de la JVM, igual que el resultado simulado; su persistencia corresponde a S-17.

El adaptador limita la petición a 120 segundos. HTTP no exitoso, timeout, JSON/base64 inválido o una imagen no admitida producen `DataAccessException` con un mensaje controlado; no se muestran el cuerpo del proveedor ni la clave. Una interrupción conserva el indicador del hilo. No hay reintentos automáticos ni sustitución silenciosa por una imagen simulada durante la generación.

## Activación

En el `config.properties` local, ignorado por Git:

```properties
ai.provider=api
ai.api_key_env=MAISON_GLOW_AI_API_KEY
```

La variable de entorno indicada debe contener la clave antes de iniciar la aplicación. `AppContext` usa `AiProviderFactory`, que comprueba disponibilidad mediante `GET /v1/models/gpt-image-1.5`, con límite de tres segundos. Si responde 200, construye `ApiAssistant` (`gpt-5.4`) y `ApiTryOnService` (`gpt-image-1.5`). Sin configuración, sin clave o si falla esa comprobación, conserva ambos simulados. La comprobación no garantiza saldo, permisos de generación ni disponibilidad en una petición posterior.

En modo API, cada solicitud válida de generación envía la foto a OpenAI y puede generar un cargo del proveedor. No se realizaron llamadas reales durante esta implementación.

La vista del probador sigue asignada a Nicoll en N-16: debe invocar el servicio fuera del hilo de JavaFX, mostrar el resultado y su aviso, y presentar los errores controlados. S-16 deja listo el backend; no agrega esa pantalla.

## Verificación

```shell
mvn test -Dtest=ApiTryOnServiceTest,SimulatedTryOnServiceTest,AiProviderFactoryTest,AppContextTest
mvn clean verify
```

Las pruebas usan HTTP falso: multipart, imagen con rótulo, foto original intacta, validación previa al envío, límites de imagen, respuestas inválidas, HTTP 401/429/500/503, recuperación en la solicitud siguiente, timeout, interrupción, selección del par API y fallback sin red. El funcionamiento con una cuenta real queda pendiente de una prueba manual con clave y foto autorizada.
