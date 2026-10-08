# ADR-0009: Interfaz gráfica con JavaFX

- **Estado:** Aceptado (reemplaza la parte de UI del [ADR-0002](0002-java-swing-netbeans-maven.md))
- **Fecha:** 2026-10-08
- **Decide:** Equipo completo, con visto bueno del docente (admite JavaFX, o web si se sustenta la arquitectura)

## Contexto
El equipo quiere una interfaz atractiva sin añadir complejidad. El docente permite HTML/CSS/JS, React o JavaFX, siempre que se sustente cómo funciona la arquitectura. El equipo eligió JavaFX.

## Decisión
- **JavaFX** con vistas declaradas en **FXML**, diseñadas con **Scene Builder** (integrable con NetBeans) y estilizadas con **CSS** propio (`estilos.css`, paleta Maison Glow).
- Cada vista `.fxml` tiene un controlador JavaFX en el paquete `vista.controlador`. Estos controladores **solo llaman a `servicio`**; la regla `vista → servicio → dao` se mantiene.
- Maven con `javafx-controls`, `javafx-fxml` y `javafx-maven-plugin`. Java 17 y Maven del ADR-0002 siguen vigentes.
- **Una vista `.fxml` por desarrollador** para evitar conflictos de merge.

## Alternativas consideradas
- **Swing (ADR-0002):** el diseñador de NetBeans es directo, pero el aspecto es anticuado y es difícil lograr una interfaz "linda".
- **HTML/CSS/JS o React:** mejor estética, pero exige una API REST en Java y un segundo lenguaje, duplica validaciones y complica el reparto del trabajo y la sustentación de la arquitectura.

## Consecuencias
- (+) Todo el proyecto queda en Java y en 3 capas; CSS permite un diseño moderno.
- (+) Separa vista (FXML) de lógica de UI (controlador), fácil de explicar.
- (−) JavaFX no viene incluido desde Java 11: hay que configurarlo en Maven y en NetBeans (probar en S0).
- (−) Scene Builder es una herramienta externa a instalar.
