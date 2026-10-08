# ADR-0002: Java 17, Swing con NetBeans y Maven

- **Estado:** Reemplazado parcialmente por [ADR-0009](0009-ui-con-javafx.md) (la UI pasa de Swing a JavaFX; Java 17 y Maven siguen vigentes)
- **Fecha:** 2026-10-08
- **Decide:** Equipo completo

## Contexto
El docente pide interfaz gráfica diseñada con NetBeans. El equipo necesita gestionar dependencias (driver JDBC, correo, JSON) sin descargar JAR a mano.

## Decisión
- **Java 17 (LTS)**, soportado por NetBeans reciente.
- **Swing** con el diseñador visual de NetBeans (archivos `.form`).
- **Maven** como sistema de construcción (NetBeans lo soporta nativamente). Dependencias previstas: `sqlite-jdbc`, `jakarta.mail`, `gson` (o `org.json`), JUnit 5.

## Alternativas consideradas
- **JavaFX:** más moderno, pero NetBeans no ofrece diseñador integrado equivalente y el docente pide NetBeans.
- **Ant (proyecto por defecto de NetBeans):** gestión manual de JAR, más propenso a errores entre integrantes.
- **Spring Boot:** fuera del alcance del curso; oculta conceptos que se deben demostrar.

## Consecuencias
- (+) Un `git clone` + abrir en NetBeans basta para compilar.
- (−) Los `.form` de NetBeans generan código y pueden causar conflictos de merge: una ventana por desarrollador (ver ADR-0004).
