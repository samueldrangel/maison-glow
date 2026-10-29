# Maison Glow

Sistema de gestión con herramientas de inteligencia artificial para la administración integral de un negocio de belleza en Valledupar, Cesar.

Proyecto de aula — **Programación de Computadores III (SS462)**, Ingeniería de Sistemas, Universidad Popular del Cesar.
Docente: Ing. Esp. Alfredo Bautista.

## Equipo

| Integrante | Rol |
|---|---|
| Samuel David Rangel Martínez | Líder del proyecto |
| Nicoll Gómez | Desarrollo (Dev 1) |
| Isabella Celedón | Desarrollo (Dev 2) |

## Tecnología

Java 17 · JavaFX (FXML + CSS) · Maven · SQLite (JDBC, patrón DAO) · arquitectura en 3 capas.

## Módulos

Clientes, profesionales, servicios, productos e inventario, citas, ventas y facturación, estadísticas y BI. Módulos experimentales de IA: probador virtual y asistente inteligente con herramientas controladas.

## Documentación

- [Planificación del proyecto](PLANIFICACION.md)
- [Diseño de clases (Fase 2)](docs/fase2/diseno-clases.md)
- [Decisiones de arquitectura (ADR)](docs/adr/README.md)
- [Índice de documentación](docs/README.md)

## Estructura

```
maison-glow/
├── docs/            # ADR y documentos por fase
├── sql/             # Esquema de la base de datos
└── src/
    ├── main/java/com/maisonglow/
    │   ├── modelo/  dao/  servicio/  vista/  ia/  util/  excepcion/
    └── test/java/
```

## Flujo de trabajo en Git

Ramas: `main` (estable, protegida) · `develop` (integración) · `feature/<modulo>`. Cambios por Pull Request con revisión del líder. Commits en formato *Conventional Commits* (`feat(citas): ...`). Detalles en el [ADR-0004](docs/adr/0004-estrategia-git.md).

## Ejecución

_Pendiente: se documentará cuando exista el proyecto Maven (Entrega 1)._
