# ADR-0005: IA desacoplada mediante interfaces y modo simulado

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Equipo completo

## Contexto
El probador virtual y el asistente dependen de servicios externos (APIs de imagen y de lenguaje) que pueden no estar disponibles, requerir pago o cambiar. El requisito RNF-13 exige que la IA esté desacoplada de la lógica de negocio y RF-21 dice "cuando el servicio de IA esté disponible".

## Decisión
- El paquete `ia` define **interfaces**: `ProbadorVirtual` (`generarSimulacion(imagen, transformacion)`) y `AsistenteInteligente` (`responder(mensaje)`).
- Cada interfaz tiene dos implementaciones: **`...Simulado`** (respuesta fija/local, para demo y pruebas) y **`...Api`** (llama al proveedor real con `java.net.http.HttpClient`).
- Se elige la implementación en un único lugar (clase de configuración leyendo `config.properties`); si falta la clave o no hay red, se usa la simulada y la UI muestra un aviso.
- Ninguna clase de `servicio`, `dao` o `modelo` importa nada del proveedor de IA.
- Los resultados del probador se rotulan siempre como "simulación, no garantía".

## Alternativas consideradas
- **Llamar a la API directamente desde la ventana:** acopla la UI al proveedor y la hace inusable sin internet.
- **Omitir la IA:** pierde el factor diferenciador del proyecto.

## Consecuencias
- (+) El sistema funciona y se puede evaluar sin claves ni red.
- (+) Es un ejemplo claro de DIP, OCP y Low Coupling para la sustentación.
- (−) Hay que mantener dos implementaciones por interfaz.
