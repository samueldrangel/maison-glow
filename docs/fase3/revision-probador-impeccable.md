# Revisión del probador virtual

Se aplicaron las guías de contexto, calidad, refinamiento y revisión nativa de [Impeccable](https://github.com/pbakaus/impeccable), versión 4.9.2, revisión `d41d986`. Referencia visual: `docs/fase2/mockups/maison_glow_probador_virtual_ui_mockup.jpg`. La revisión se limita al probador y su acceso desde el menú; conserva la identidad y navegación del resto de la aplicación.

Es una interfaz JavaFX/FXML. Se revisó su código y sus capturas JavaFX; el detector web/DOM de Impeccable no verifica este tipo de aplicación. La automatización de escritorio leyó el arranque, pero no pudo activar la ventana para completar el recorrido manual. Las interacciones se comprobaron con pruebas sobre los controles JavaFX reales y servicios locales simulados.

| Hallazgo | Corrección y comprobación |
|---|---|
| El cliente recién creado no aparece en la pantalla conservada | Consulta actualizada al navegar y al abrir el selector; prueba que crea otro cliente con la vista ya cargada. |
| Lista de tonos muy distinta del mockup | Quince muestras circulares con texturas extraídas de la referencia; selección con borde, nombre visible, tooltip y nombre accesible. Un catálogo compartido valida los tonos en ambos proveedores. |
| Encabezado central cortado y fotos que invaden otra tarjeta | Títulos con ajuste de línea, columnas proporcionadas, mínimos de imagen corregidos y desplazamiento vertical cuando falta altura. Capturas y límites de los controles comprobados en cuatro tamaños de contenido, incluido el espacio disponible en una ventana de 960 px. |
| Falta intensidad y comparación | Intensidad conectada a la generación. Comparador deslizante para proporciones compatibles; si la API devuelve otra proporción, se compara con la referencia del panel izquierdo sin superponer imágenes desalineadas. El aviso permanece en el archivo exportado. |
| Texto tenue y botón dorado de poco contraste | Contrastes calculados: blanco/dorado 5,40:1; blanco/rosa 4,75:1; texto secundario/superficie 5,72:1 y texto secundario/crema 5,34:1. Foco visible y controles de teclado conservados. |
| Resultado anterior visible después de cambiar la solicitud | Cambiar cliente, foto, tono o intensidad invalida el resultado y desactiva Guardar. La solicitud en curso bloquea sus entradas para evitar mezclar estados. |

Las capturas se generan en `target/visual-qa/tryon-836.png`, `tryon-884.png`, `tryon-1250.png` y `tryon-1844.png`. La foto de revisión puede pasarse con `-Dmaison.visualPhoto=ruta.png`; las pruebas normales usan una imagen sintética temporal.

Se verifican estados vacío, selección, errores por campo, generación, resultado y exportación. El modo simulado identifica expresamente que tiñe la foto completa. Las llamadas reales a la API no forman parte de esta verificación; su adaptador se prueba con respuestas HTTP controladas.
