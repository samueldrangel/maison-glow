# Fase 1 — Identificación y especificación del problema

## 1. Portada

| | |
|---|---|
| **Proyecto** | Maison Glow — Sistema de gestión con herramientas de inteligencia artificial para un negocio de belleza |
| **Institución** | Universidad Popular del Cesar — Programa de Ingeniería de Sistemas |
| **Asignatura** | Programación de Computadores III (SS462) |
| **Docente** | Ing. Esp. Alfredo Bautista |
| **Integrantes** | Samuel David Rangel Martínez (líder del proyecto) · Nicoll Gómez · Isabella Celedón |
| **Ciudad y año** | Valledupar, Cesar — 2026 |
| **Repositorio** | <https://github.com/samueldrangel/maison-glow> |

**Contenido**

1. [Portada](#1-portada)
2. [Resumen](#2-resumen)
3. [Introducción](#3-introducción)
4. [Planteamiento del problema](#4-planteamiento-del-problema)
5. [Justificación](#5-justificación)
6. [Estado del arte y vigilancia tecnológica](#6-estado-del-arte-y-vigilancia-tecnológica)
7. [Objetivos](#7-objetivos)
8. [Descripción de la solución](#8-descripción-de-la-solución)
9. [Funcionalidades de inteligencia artificial](#9-funcionalidades-de-inteligencia-artificial)
10. [Requerimientos funcionales (RF)](#10-requerimientos-funcionales-rf)
11. [Requerimientos no funcionales (RNF)](#11-requerimientos-no-funcionales-rnf)
12. [Repositorio y control de versiones](#12-repositorio-y-control-de-versiones)
13. [Organización del equipo y alcance de la Entrega 1](#13-organización-del-equipo-y-alcance-de-la-entrega-1)
14. [Conclusiones](#14-conclusiones)
15. [Bibliografía](#15-bibliografía)

---

## 2. Resumen

El presente proyecto propone el diseño y desarrollo de un sistema de gestión orientado a la administración integral de un negocio de belleza ubicado en Valledupar, Cesar. La solución busca centralizar en una misma aplicación los principales procesos relacionados con la gestión de clientes, profesionales, servicios, productos, inventario, citas y ventas.

Como componente diferenciador, el sistema incorporará herramientas basadas en inteligencia artificial orientadas a complementar la operación del establecimiento y mejorar la experiencia del cliente. Entre estas funcionalidades se contempla un probador virtual para la simulación de cambios de apariencia y un asistente inteligente capaz de consultar información y ejecutar determinadas operaciones mediante herramientas previamente definidas y controladas.

El proyecto será desarrollado inicialmente para un único establecimiento y tendrá como propósito integrar los conocimientos de programación orientada a objetos, diseño de software, arquitectura en capas, persistencia de datos, patrones GRASP, principios SOLID, manejo de excepciones y control de versiones (Git). La solución será concebida de manera modular, permitiendo que las funcionalidades de inteligencia artificial permanezcan desacopladas de la lógica principal del negocio.

---

## 3. Introducción

Los negocios dedicados a la belleza y al cuidado personal manejan diferentes procesos relacionados con la atención de clientes, programación de citas, prestación de servicios, venta de productos y control de inventario. Cuando estos procesos se realizan de manera manual o utilizando herramientas independientes, la información puede quedar distribuida y dificultar el seguimiento de las operaciones del establecimiento.

Además de la administración tradicional, las tecnologías de inteligencia artificial han abierto nuevas posibilidades para mejorar la interacción entre los clientes y los negocios. En el sector de la belleza, por ejemplo, las herramientas de generación y edición de imágenes permiten explorar virtualmente diferentes alternativas de apariencia antes de realizar un procedimiento.

A partir de esta situación se plantea el desarrollo de un sistema local orientado a un negocio de belleza, que integre las principales operaciones administrativas y comerciales en una sola aplicación y que incorpore herramientas de inteligencia artificial como elemento diferenciador.

El sistema permitirá administrar clientes, productos, servicios, profesionales, inventario, ventas y citas. Adicionalmente, se plantea incorporar un probador virtual para visualizar posibles cambios de apariencia y un asistente inteligente que pueda consultar información y ejecutar operaciones determinadas dentro del sistema.

El desarrollo del proyecto permitirá aplicar los conocimientos adquiridos en Programación de Computadores III, particularmente en programación orientada a objetos, diseño de clases, arquitectura en capas, persistencia, interfaces gráficas, patrones de diseño y principios de diseño de software.

---

## 4. Planteamiento del problema

### 4.1 Descripción del problema

Un negocio de belleza puede desarrollar simultáneamente actividades relacionadas con la prestación de servicios, atención de clientes, programación de citas, administración de profesionales, comercialización de productos y control de inventario. Cada una de estas actividades genera información que debe ser registrada, consultada y relacionada para facilitar la administración del establecimiento.

Cuando la información de estos procesos se encuentra distribuida en diferentes herramientas, registros manuales o mecanismos independientes, pueden presentarse dificultades para consultar el historial de los clientes, conocer la disponibilidad de los profesionales, controlar las existencias de productos, realizar seguimiento de las citas y relacionar las operaciones comerciales.

A esta problemática se suma la necesidad de ofrecer experiencias más interactivas a los clientes. En particular, antes de realizar determinados procedimientos estéticos, un cliente puede requerir una referencia visual sobre el posible resultado. Las tecnologías de inteligencia artificial para generación y edición de imágenes ofrecen una alternativa para implementar simulaciones visuales que sirvan como apoyo durante este proceso.

Por lo anterior, surge la necesidad de analizar los procesos principales de un negocio de belleza y diseñar una solución tecnológica que permita centralizar su gestión e incorporar herramientas de inteligencia artificial de manera controlada y complementaria.

### 4.2 Formulación del problema

¿Cómo diseñar y desarrollar un sistema de gestión con herramientas de inteligencia artificial que permita centralizar la administración de los principales procesos de un negocio de belleza en Valledupar, Cesar?

---

## 5. Justificación

El desarrollo del sistema se justifica desde cuatro perspectivas complementarias:

- **Perspectiva operativa.** Permite centralizar información relacionada con clientes, productos, servicios, citas, ventas e inventario. Esto facilita la consulta y administración de la información del establecimiento y permite mantener una relación coherente entre las diferentes operaciones (por ejemplo, la asociación automática entre cita, profesional y cliente, y el descuento de inventario tras una venta).
- **Perspectiva tecnológica.** Incorpora inteligencia artificial como una funcionalidad complementaria al sistema tradicional de gestión. El probador virtual permite explorar visualmente posibles cambios de apariencia, mientras que el asistente inteligente interactúa mediante lenguaje natural con herramientas explícitas y controladas dentro del dominio del negocio.
- **Perspectiva académica.** Sirve como vehículo práctico para aplicar conceptos avanzados de Programación III, tales como programación orientada a objetos (encapsulamiento, herencia, polimorfismo, interfaces, clases abstractas, colecciones), arquitectura en capas, persistencia relacional con patrón DAO, principios SOLID, patrones GRASP, manejo adecuado de excepciones, diseño de interfaces gráficas y control de versiones con Git.
- **Perspectiva económica.** La implementación del sistema reduce costos operativos asociados al manejo manual de citas, inventario y ventas (cuadernos, hojas de cálculo o herramientas desarticuladas), y disminuye pérdidas económicas derivadas de errores de inventario, citas duplicadas o falta de control de caja. Adicionalmente, el componente de inteligencia artificial agrega valor diferencial frente a las alternativas existentes en el mercado sin incurrir en costos de licenciamiento recurrente, dado que se desarrolla como proyecto propio con herramientas de código abierto.

---

## 6. Estado del arte y vigilancia tecnológica

Para establecer el contexto tecnológico del proyecto se revisaron diferentes sistemas existentes relacionados con la administración de salones de belleza.

### 6.1 Beauty Salon Management System — Java / Spring Boot

Desarrollado en Java y Spring Boot, implementa funcionalidades como gestión de citas, inventario, ventas y punto de venta (POS), clientes y seguridad, con arquitectura de tres capas, JPA/Hibernate y MySQL. Demuestra que la combinación de agenda, inventario, clientes y ventas constituye el dominio estándar para estos sistemas.

### 6.2 Glamour Beauty Salon Management System

Implementa funcionalidades para administrar clientes, empleados, servicios, citas, pagos, inventario y retroalimentación de clientes, además de control para evitar reservas simultáneas.

### 6.3 Hair Salon Management System

Contempla entidades centrales (`Customer`, `Stylist`, `SalonService`, `Appointment`, `Payment`, `ProductInventory`, `User`, `Role`) y reglas de negocio rigurosas para evitar conflictos de traslape horario.

### 6.4 Matriz comparativa del estado del arte

| Característica / funcionalidad | Proyecto 6.1 | Proyecto 6.2 | Proyecto 6.3 | Maison Glow |
|---|:--:|:--:|:--:|:--:|
| Gestión de clientes | ✓ | ✓ | ✓ | ✓ |
| Gestión de servicios | ✓ | ✓ | ✓ | ✓ |
| Gestión de citas | ✓ | ✓ | ✓ | ✓ |
| Empleados / profesionales | ✓ | ✓ | ✓ | ✓ |
| Inventario de productos | ✓ | ✓ | ✓ | ✓ |
| Ventas / pagos (POS) | ✓ | ✓ | ✓ | ✓ |
| Estadísticas | ✓ | ✓ | ✓ | ✓ |
| IA para visualización (probador virtual) | — | — | — | ✓ |
| Asistente inteligente (NLP / herramientas) | — | — | — | ✓ |
| Interacción mediante herramientas (MCP) | — | — | — | ✓ |

> **Fuentes de los proyectos 6.1 a 6.3.** Las URLs de los tres repositorios o sitios revisados se agregan aquí antes de la entrega: _[URL del proyecto 6.1]_, _[URL del proyecto 6.2]_ y _[URL del proyecto 6.3]_. Cada una debe aparecer también en la [bibliografía](#15-bibliografía).

**Conclusión del estado del arte.** Las soluciones tradicionales de administración de salones ya están ampliamente extendidas. Por ello, el factor diferenciador del proyecto radica en la integración coordinada de herramientas de IA (probador virtual y asistente con herramientas controladas, al estilo del Model Context Protocol, MCP) con la lógica de negocio y la base de datos relacional del establecimiento.

### 6.5 Análisis de la competencia

En el contexto de Valledupar, Cesar, la mayoría de los salones de belleza y centros de estética gestionan sus operaciones mediante métodos manuales (cuadernos, agendas físicas) o herramientas genéricas como hojas de cálculo y aplicaciones de mensajería para la programación de citas, sin un sistema integrado que centralice clientes, inventario, ventas y agenda.

A nivel de software comercial, existen plataformas genéricas de gestión para salones de belleza (como las revisadas en las secciones 6.1 a 6.3) que ofrecen funcionalidades estándar de agenda, inventario y ventas, pero no incorporan funcionalidades de inteligencia artificial como el probador virtual o el asistente conversacional propuestos en este proyecto. Esto representa una oportunidad de diferenciación, ya que ningún competidor identificado combina la gestión integral del negocio con herramientas de IA aplicadas directamente a la experiencia del cliente y a la operación del negocio.

Por tanto, la competencia directa del proyecto no proviene de otro sistema con IA equivalente, sino de la inercia de los métodos manuales y de las soluciones genéricas sin diferenciación tecnológica, lo cual favorece la adopción del sistema propuesto en el mercado local.

---

## 7. Objetivos

### Objetivo general

Desarrollar un sistema de gestión con herramientas de inteligencia artificial para centralizar la administración de los principales procesos de un negocio de belleza en Valledupar, Cesar.

### Objetivos específicos

1. Analizar los procesos de gestión de clientes, profesionales, servicios, productos, inventario, citas y ventas de un negocio de belleza, así como las soluciones tecnológicas existentes en este ámbito, para identificar los requerimientos del sistema.
2. Diseñar la arquitectura, el modelo orientado a objetos y la estructura de la solución, definiendo los componentes necesarios para integrar los procesos de gestión y las herramientas de inteligencia artificial.
3. Desarrollar un sistema funcional que permita gestionar los principales procesos del negocio e integrar herramientas de inteligencia artificial orientadas a la simulación visual de cambios de apariencia y a la asistencia en determinadas consultas y operaciones.

---

## 8. Descripción de la solución

La aplicación se concibe como un software local para la administración integral de un único establecimiento. Sus módulos principales abarcan:

- **Gestión de productos.** Registro, modificación, consulta, control de precios, stock y alertas de bajo inventario.
- **Gestión de servicios.** Registro, precio, duración, asociación a profesionales y estado activo/inactivo.
- **Gestión de clientes.** Registro, consulta, actualización de datos e historial de citas y compras.
- **Gestión de citas.** Agendamiento por cliente, servicio, profesional y fecha y hora, con control estricto de disponibilidad y eliminación de conflictos de horario. Incluye un conjunto de reglas de negocio configurables (anticipo o depósito, confirmación previa, recordatorio y política de penalización por inasistencia) orientadas a evitar la pérdida del horario reservado.
- **Gestión de ventas y facturación.** Registro de ventas de productos, servicios o combinadas (con actualización automática de stock) y envío automático de la factura al correo electrónico del cliente.
- **Gestión de estadísticas.** Reemplaza el historial de operaciones por reportes estadísticos (ventas, servicios más solicitados, clientes frecuentes, rotación de productos), disponibles tanto para el cliente (su propio historial) como para el administrador (visión global del negocio).
- **Inteligencia de negocios.** Identifica los productos con menor rotación de ventas, notifica al administrador y los ofrece al cliente mediante recomendaciones y promociones generadas dentro del sistema.

---

## 9. Funcionalidades de inteligencia artificial

### 9.1 Probador virtual

Módulo experimental que simula transformaciones de estilo:

1. El cliente selecciona una fotografía personal.
2. Selecciona la transformación deseada (por ejemplo, cambio de color de cabello a «rojo cobrizo»).
3. El servicio de IA genera la imagen editada aproximada.
4. El resultado se visualiza en la interfaz, presentado como simulación y no como garantía definitiva del procedimiento real.

### 9.2 Asistente inteligente (integración con herramientas tipo MCP)

El asistente procesa peticiones en texto o voz ejecutando funciones controladas dentro del sistema:

- **Consulta de stock:** «¿Qué productos tienen poco inventario?» → ejecuta `getLowStockProducts()`.
- **Consulta de agenda:** «¿Qué citas hay mañana?» → ejecuta `getAppointmentsByDate()`.
- **Agendamiento asistido:** «Agenda a María mañana a las tres para maquillaje.» → coordina `searchCustomer()`, `findService()`, `checkAvailability()` y `createAppointment()`.

> **Nota de implementación.** Estos nombres son ilustrativos. Para el curso se adopta el *concepto* de MCP (herramientas con nombre, descripción y parámetros, en lista blanca, que llaman a los servicios y nunca a los DAO) con una implementación propia, sin servidor MCP completo. La decisión y sus alternativas están en el [ADR-0006](../adr/0006-herramientas-del-asistente-tipo-mcp.md).

### 9.3 Inteligencia de negocios (Business Intelligence)

Módulo analítico orientado a apoyar decisiones comerciales mediante el seguimiento de ventas:

- **Detección de productos de baja rotación:** el sistema identifica periódicamente los productos menos vendidos dentro de un rango de tiempo.
- **Notificación al administrador:** se genera una alerta indicando qué productos presentan baja rotación, para apoyar decisiones de precio, promoción o descontinuación.
- **Recomendación al cliente:** los productos de baja rotación pueden ofrecerse al cliente mediante el asistente inteligente o la interfaz de ventas, incluyendo nuevas promociones asociadas.

---

## 10. Requerimientos funcionales (RF)

| ID | Descripción del requerimiento funcional |
|---|---|
| RF-01 | El sistema deberá permitir registrar clientes. |
| RF-02 | El sistema deberá permitir consultar y actualizar clientes. |
| RF-03 | El sistema deberá permitir registrar productos. |
| RF-04 | El sistema deberá permitir modificar y consultar productos. |
| RF-05 | El sistema deberá permitir controlar las existencias de productos. |
| RF-06 | El sistema deberá permitir registrar servicios. |
| RF-07 | El sistema deberá permitir definir precio y duración de un servicio. |
| RF-08 | El sistema deberá permitir registrar profesionales. |
| RF-09 | El sistema deberá permitir asociar profesionales con servicios. |
| RF-10 | El sistema deberá permitir registrar citas. |
| RF-11 | El sistema deberá verificar la disponibilidad del profesional antes de registrar una cita. |
| RF-12 | El sistema deberá evitar conflictos de horario entre citas. |
| RF-13 | El sistema deberá permitir modificar y cancelar citas. |
| RF-14 | El sistema deberá permitir registrar ventas. |
| RF-15 | El sistema deberá permitir incluir productos en una venta. |
| RF-16 | El sistema deberá permitir incluir servicios en una venta. |
| RF-17 | El sistema deberá actualizar el inventario después de una venta de productos. |
| RF-18 | El sistema deberá permitir consultar estadísticas del cliente (compras y citas), disponibles tanto para el cliente como para el administrador. |
| RF-19 | El sistema deberá permitir consultar productos con inventario bajo. |
| RF-20 | El sistema deberá proporcionar una interfaz para administrar la información del negocio. |
| RF-21 | El sistema deberá permitir generar una simulación visual mediante el módulo de probador virtual cuando el servicio de IA esté disponible. |
| RF-22 | El sistema deberá permitir al asistente inteligente consultar información del negocio mediante herramientas controladas. |
| RF-23 | El sistema deberá permitir al asistente ejecutar operaciones autorizadas mediante herramientas definidas. |
| RF-24 | El sistema deberá validar los datos antes de almacenarlos. |
| RF-25 | El sistema deberá manejar los errores producidos durante las operaciones del sistema. |
| RF-26 | El sistema deberá aplicar un conjunto de reglas de negocio para la gestión de citas (anticipo o depósito, confirmación previa y recordatorio) orientadas a evitar la pérdida del horario reservado. |
| RF-27 | El sistema deberá enviar automáticamente la factura de venta al correo electrónico registrado del cliente. |
| RF-28 | El sistema deberá permitir al administrador consultar estadísticas generales del negocio (productos más y menos vendidos, servicios más solicitados). |
| RF-29 | El sistema deberá identificar los productos con menor rotación de ventas dentro de un periodo determinado. |
| RF-30 | El sistema deberá notificar al administrador los productos con menor rotación de ventas. |
| RF-31 | El sistema deberá permitir ofrecer al cliente productos de baja rotación y promociones mediante el asistente inteligente o la interfaz de ventas. |

---

## 11. Requerimientos no funcionales (RNF)

| ID | Descripción del requerimiento no funcional |
|---|---|
| RNF-01 | El sistema deberá utilizar una arquitectura organizada por capas. |
| RNF-02 | El código deberá seguir convenciones de nomenclatura establecidas para Java. |
| RNF-03 | Las clases deberán mantener responsabilidades claramente definidas. |
| RNF-04 | El sistema deberá aplicar principios de diseño orientado a objetos. |
| RNF-05 | El sistema deberá aplicar principios SOLID cuando sean pertinentes. |
| RNF-06 | El diseño deberá considerar patrones GRASP. |
| RNF-07 | El sistema deberá utilizar una base de datos relacional para la persistencia. |
| RNF-08 | El acceso a datos deberá estar separado de la lógica de negocio. |
| RNF-09 | El sistema deberá manejar adecuadamente las excepciones. |
| RNF-10 | La interfaz gráfica deberá proporcionar navegación clara entre los módulos. |
| RNF-11 | La aplicación deberá ejecutarse en un entorno local. |
| RNF-12 | La información deberá mantenerse almacenada de forma persistente. |
| RNF-13 | Las funcionalidades de IA deberán estar desacopladas de la lógica principal del negocio. |
| RNF-14 | Las operaciones realizadas por el asistente deberán estar restringidas a herramientas previamente definidas. |
| RNF-15 | El código fuente deberá mantenerse bajo control de versiones mediante Git. |

---

## 12. Repositorio y control de versiones

| Campo | Valor |
|---|---|
| **Nombre** | `maison-glow` |
| **URL** | <https://github.com/samueldrangel/maison-glow> |
| **Descripción** | Sistema de gestión con herramientas de inteligencia artificial para un negocio de belleza en Valledupar, Cesar |
| **Ramas permanentes** | `main` (estable, protegida) y `develop` (integración) |
| **Ramas de trabajo** | `feature/<tarea>`, `docs/<tema>` y `refactor/<tema>`, una por tarea, creadas desde `develop` |
| **Flujo** | Rama → Pull Request a `develop` → revisión → *merge* normal → rama borrada |
| **Commits** | *Conventional Commits* (`feat`, `fix`, `test`, `docs`, `refactor`, `chore`) |

La estrategia y sus motivos están en el [ADR-0004](../adr/0004-estrategia-git.md); los commits y Pull Requests de cada integrante, en [`../version-control.md`](../version-control.md). Esto cubre el RNF-15.

---

## 13. Organización del equipo y alcance de la Entrega 1

### 13.1 Roles

| Integrante | Rol | Responsabilidad principal |
|---|---|---|
| Samuel David Rangel Martínez | Líder del proyecto | Arquitectura, repositorio y ramas, núcleo transversal (DAO base, excepciones, validación), citas, ventas, inteligencia artificial, mockups, diagramas y consolidación de documentos |
| Nicoll Gómez | Desarrollador 1 | Clientes, profesionales, tratamientos, estadísticas, baja rotación y promociones, pruebas de integración y demo de consola |
| Isabella Celedón | Desarrolladora 2 | Productos e inventario, configuración del negocio, reglas de cita y factura simulada |

Distribución de clases y reasignaciones: [`../../TEAM.md`](../../TEAM.md). Tabla de tareas con dependencias, ramas y estados: [`../roadmap.md`](../roadmap.md).

### 13.2 Alcance de la Entrega 1

La Entrega 1 implementa **toda la lógica de negocio** con persistencia en **archivos de texto**, deja la **base de datos diseñada** (MER y `sql/schema.sql`, sin conectar) y presenta los **mockups** de la interfaz. Los motivos están en el [ADR-0012](../adr/0012-persistencia-en-archivos-de-texto.md).

| Requisito | Estado en la Entrega 1 |
|---|---|
| RF-01 a RF-19, RF-24 a RF-31 | Lógica de negocio implementada y probada con JUnit, sin interfaz. En RF-18 y RF-28 las estadísticas existen; la separación por rol (cliente ve solo las suyas) llega con `app_user` en la Entrega 2 |
| RF-20, RNF-10 | Mockups M1 a M9; la interfaz JavaFX es de la Entrega 2 |
| RF-21 a RF-23, RNF-13, RNF-14 | Backend de IA opcional (S-12 y S-13) detrás de interfaces; adaptadores reales en la Entrega 2 |
| RNF-07, RNF-12 | Base de datos relacional **diseñada**; la persistencia efectiva es en archivos de texto detrás de `CrudDao<T>` y pasa a SQLite (JDBC) en la Entrega 2 |
| RNF-01, 03, 04, 05, 06, 08, 09 | Arquitectura en capas, DAO, GRASP y SOLID aplicados; ver [`../fase2/diseno-clases.md`](../fase2/diseno-clases.md) y [`../fase2/diagramas.md`](../fase2/diagramas.md) |
| RNF-15 | Todo el código se integra por Pull Request |

---

## 14. Conclusiones

1. El análisis de los procesos de un negocio de belleza y de los sistemas existentes confirmó que la agenda, el inventario, las ventas y los clientes forman el núcleo de este tipo de software. La integración coordinada de herramientas de IA con esa lógica es lo que diferencia a Maison Glow.
2. La arquitectura en tres capas, con acceso a datos detrás de interfaces (`CrudDao<T>`), permitió construir y probar toda la lógica de negocio sin base de datos y planear el cambio a SQLite sin modificar los servicios.
3. Las reglas de cita como estrategias (`AppointmentRule`), los ítems vendibles como interfaz (`Sellable`) y las herramientas del asistente en lista blanca muestran la aplicación de SOLID y GRASP: se agrega una regla, un tipo vendible o una herramienta como clase nueva, sin modificar las existentes.
4. Mantener la IA desacoplada del negocio y con una versión simulada garantiza que el sistema funcione sin internet ni servicios de pago, y que ninguna falla de IA bloquee la operación.
5. El trabajo por ramas y Pull Requests dejó evidencia verificable de la contribución de cada integrante en el historial de Git.

---

## 15. Bibliografía

Referencias en formato APA 7.ª edición. Las fuentes en línea se consultaron el 28 de octubre de 2026.

Bloch, J. (2018). *Effective Java* (3.ª ed.). Addison-Wesley.

Chacon, S., y Straub, B. (2014). *Pro Git* (2.ª ed.). Apress. https://git-scm.com/book

Conventional Commits. (s. f.). *Conventional Commits 1.0.0*. https://www.conventionalcommits.org/

Gamma, E., Helm, R., Johnson, R., y Vlissides, J. (1994). *Design patterns: Elements of reusable object-oriented software*. Addison-Wesley.

Larman, C. (2004). *Applying UML and patterns: An introduction to object-oriented analysis and design and iterative development* (3.ª ed.). Prentice Hall.

Martin, R. C. (2003). *Agile software development: Principles, patterns, and practices*. Prentice Hall.

Martin, R. C. (2008). *Clean code: A handbook of agile software craftsmanship*. Prentice Hall.

Model Context Protocol. (s. f.). *Model Context Protocol*. https://modelcontextprotocol.io/

Nygard, M. (2011, 15 de noviembre). *Documenting architecture decisions*. Cognitect. https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions

OpenJFX. (s. f.). *OpenJFX*. https://openjfx.io/

SQLite. (s. f.). *SQLite documentation*. https://www.sqlite.org/docs.html

The JUnit Team. (s. f.). *JUnit 5 user guide*. https://junit.org/junit5/docs/current/user-guide/

The Apache Software Foundation. (s. f.). *Apache Maven project*. https://maven.apache.org/

> **Pendiente de agregar:** las fuentes de los proyectos 6.1 a 6.3 (sección 6) y, si el docente lo exige, las de los ADR.
