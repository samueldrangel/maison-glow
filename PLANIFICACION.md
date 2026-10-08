# Maison Glow — Planificación del Proyecto

> **Asignatura:** Programación de Computadores III (SS462) · **Docente:** Ing. Esp. Alfredo Bautista
> **Programa:** Ingeniería de Sistemas, Universidad Popular del Cesar · **Valledupar, Cesar — 2026**
> **Lenguaje / IDE:** Java 17 + NetBeans + JavaFX ([ADR-0009](docs/adr/0009-ui-con-javafx.md)) · **Modalidad:** grupal (3 personas)
>
> **Primera entrega (≤ 2 semanas):** Fases 1 y 2 + **toda la lógica de negocio** funcionando (sin BD) + **mockups** de la UI + **BD diseñada** (MER y `schema.sql`, aún sin conectar).

Este documento es el plan maestro. Las decisiones técnicas están justificadas en [`docs/adr/`](docs/adr/README.md).
Las fechas se expresan en **semanas relativas**; ajústalas a las fechas de entrega que fije el docente.

---

## 1. Equipo y roles

| Integrante | Rol | Responsabilidad principal |
|---|---|---|
| **Samuel David Rangel Martínez** | **Líder del proyecto** (Tech Lead / Scrum) | Arquitectura, repositorio y ramas, revisión y merge de Pull Requests, núcleo transversal (BD, excepciones, DAO base), módulos de **Citas** y **Ventas**, consolidación de documentos de cada fase |
| **Nicoll Gómez** | **Dev 1** | Módulos de **Clientes, Profesionales y Servicios**, **Estadísticas / Inteligencia de Negocios** y **Asistente Inteligente** (herramientas) |
| **Isabella Celedón** | **Dev 2** | Módulos de **Productos e Inventario**, **ventana principal y navegación (GUI)**, mockups y **Probador Virtual** |

**Regla de equipo:** cada dev es dueño de su módulo "de punta a punta" (modelo → DAO → servicio → ventana). Así todos practican las 3 capas y se reducen los conflictos de merge. El líder revisa **todos** los PR.

---

## 2. Alcance: qué entra y qué es opcional

Para un curso de Programación III el proyecto es amplio; se prioriza en tres niveles:

| Nivel | Contenido | RF relacionados |
|---|---|---|
| **MVP (obligatorio)** | CRUD de clientes, profesionales, servicios y productos; citas con control de conflicto de horario; ventas con descuento de stock; stock bajo; validaciones y excepciones; GUI con navegación | RF-01 a RF-20, RF-24, RF-25 |
| **Deseable** | Reglas de negocio de citas (anticipo, confirmación, recordatorio); estadísticas cliente/admin; baja rotación (BI); factura por correo | RF-18, RF-26 a RF-31 |
| **Experimental (si hay tiempo)** | Asistente inteligente con herramientas controladas; Probador virtual. **Se implementan detrás de interfaces con una versión simulada (stub)** para que el sistema funcione sin API ni internet | RF-21 a RF-23 |

> Principio: el MVP debe estar **cerrado y estable antes** de tocar IA. La IA nunca bloquea la entrega (RNF-13).

---

## 3. Arquitectura propuesta (simple, en 3 capas)

```
┌──────────────────────────────────────────────┐
│ PRESENTACIÓN  (paquete vista)   JavaFX + FXML │
└───────────────┬──────────────────────────────┘
                │ llama a
┌───────────────▼──────────────────────────────┐
│ NEGOCIO  (servicio + modelo + reglas)         │──► ia (interfaces) ◄── adaptadores IA
└───────────────┬──────────────────────────────┘
                │ usa interfaces
┌───────────────▼──────────────────────────────┐
│ DATOS  (dao)  archivos .txt → JDBC + SQLite   │
└──────────────────────────────────────────────┘
```

**Paquetes** (`com.maisonglow`):

| Paquete | Contenido |
|---|---|
| `modelo` | Entidades del dominio: `Persona`*, `Cliente`, `Profesional`, `Producto`, `Servicio`, `Cita`, `Venta`, `DetalleVenta`, interfaz `Vendible`, enums de estado |
| `dao` | Interfaz genérica `CrudDao<T>` con dos implementaciones por entidad: **archivo de texto** (`ClienteDaoTexto`, entrega 1) y **JDBC** (`ClienteDaoJdbc`, entrega 2). Ver [ADR-0012](docs/adr/0012-persistencia-en-archivos-de-texto.md) |
| `servicio` | Lógica de negocio y coordinación (`CitaServicio`, `VentaServicio`, ...). Actúa como *Controller* GRASP |
| `servicio.regla` | `ReglaCita` (interfaz) y sus implementaciones: anticipo, confirmación, recordatorio |
| `vista` | Vistas `.fxml` (JavaFX), `estilos.css` y sus controladores en `vista.controlador` |
| `ia` | Interfaces `ProbadorVirtual`, `AsistenteInteligente`, `HerramientaAsistente` + implementaciones simuladas y reales |
| `util` | `ConexionBD`, validadores, formateadores |
| `excepcion` | `ValidacionException`, `AccesoDatosException`, `ReglaNegocioException` |

\* = clase abstracta.

**Reglas de dependencia:** `vista → servicio → dao`. Nunca al revés, y `vista` jamás usa `dao` ni SQL directamente.

### 3.1 Clases núcleo (≥ 8, requisito Fase 2)

| # | Clase | Tipo | Rol |
|---|---|---|---|
| 1 | `Persona` | Abstracta | Datos comunes (id, nombre, teléfono, correo) |
| 2 | `Cliente` | Concreta (extiende `Persona`) | Cliente del negocio; historial de citas y compras |
| 3 | `Profesional` | Concreta (extiende `Persona`) | Especialista; servicios que ofrece; agenda |
| 4 | `Vendible` | Interfaz | Contrato: `getNombre()`, `getPrecio()` |
| 5 | `Producto` | Concreta (implementa `Vendible`) | Artículo con stock y stock mínimo |
| 6 | `Servicio` | Concreta (implementa `Vendible`) | Servicio con precio y duración |
| 7 | `Cita` | Concreta | Cliente + profesional + servicio + fecha/hora + estado |
| 8 | `Venta` / `DetalleVenta` | Concretas | Venta con lista polimórfica de ítems `Vendible` |
| 9 | `ReglaCita` | Interfaz | Regla de negocio de citas (extensible) |
| 10 | `CrudDao<T>` | Interfaz | Contrato CRUD de la capa de datos |

Esto cubre **herencia** (`Persona`), **interfaces** (`Vendible`, `ReglaCita`, `CrudDao`), **clase abstracta**, **polimorfismo** (`Venta` recorre `Vendible`) y **colecciones** (`ArrayList`, `HashMap`).

### 3.2 Modelo de base de datos (borrador)

Tablas: `cliente`, `profesional`, `servicio`, `profesional_servicio` (N:M), `producto`, `cita`, `venta`, `detalle_venta`. El MER y el diccionario de datos se entregan en la Fase 4 (ver [ADR-0003](docs/adr/0003-persistencia-sqlite-jdbc-dao.md)).

### 3.3 Principios aplicados (previsión)

| Principio | Dónde se verá |
|---|---|
| **SRP** | Cada DAO solo accede a datos; cada servicio solo reglas; las ventanas solo UI |
| **OCP** | Nuevas `ReglaCita` o nuevas `HerramientaAsistente` sin modificar el código existente |
| **DIP** | Servicios dependen de `CrudDao<T>` e interfaces de IA, no de clases concretas |
| **Creator** | `Venta.agregarDetalle()` crea `DetalleVenta` porque lo compone |
| **Controller** | Clases `*Servicio` reciben las operaciones de la UI |
| **Expert** | `Producto.descontarStock()`, `Venta.calcularTotal()` viven donde está la información |
| **Low Coupling / High Cohesion** | Capa `ia` aislada por interfaces; paquetes con una sola responsabilidad |

---

## 4. Plan por fases y cronograma

El curso entrega 4 fases. Duración sugerida (ajustar con el docente):

| Semana | Entrega | Foco |
|---|---|---|
| 1–2 | **ENTREGA 1** | Fases 1 y 2 completas; **lógica de negocio completa** (modelo, servicios, reglas) con persistencia en archivos de texto; **mockups** de la UI; **BD diseñada** (MER + `schema.sql`, sin conectar) |
| 3–4 | Entrega 2 (a) | Conexión SQLite + DAO JDBC (CRUD, excepciones, transacciones); reemplazo de los DAO de archivo de texto |
| 5–7 | Entrega 2 (b) | Interfaz JavaFX real conectada a los servicios; estadísticas y BI |
| 8–9 | Entrega 2 (c) | Módulos de IA (asistente, probador), factura por correo, pulido y Javadoc |
| 10 | **Fase 4** | Documentación final, MER definitivo, diccionario de datos, trazabilidad, reporte Git |

> Las semanas 3 en adelante son tentativas hasta conocer las fechas del docente.

### 4.1 Fase 1 — Identificación y especificación del problema

| Entregable | Responsable | Apoyo | Estado |
|---|---|---|---|
| Portada, resumen, introducción | Samuel | — | Redactado (base del enunciado) |
| Planteamiento, justificación, estado del arte | Nicoll | Isabella | Redactado; falta completar matriz con proyectos reales y URLs |
| Objetivos | Samuel | — | Redactado |
| Descripción de la solución, RF, RNF | Isabella | Nicoll | Redactado |
| **Repositorio GitHub (nombre, URL, descripción)** | Samuel | — | Pendiente |
| Roles, estrategia de ramas, tabla de tareas | Samuel | — | En este documento |
| Conclusiones y bibliografía (formato APA) | Isabella | Todos | Pendiente |

### 4.2 Fase 2 — Arquitectura

| Entregable | Responsable | Apoyo |
|---|---|---|
| Diagrama de paquetes por capas | Samuel | — |
| Diagrama de clases + tabla de identificación (8+ clases) | Samuel | Nicoll |
| Atributos, métodos y responsabilidades por clase (plantilla del docente) | Cada dev documenta **sus** clases | Samuel revisa |
| Relaciones (herencia, composición, agregación) | Samuel | — |
| Mockups / wireframes (Figma, Draw.io o NetBeans) | Isabella | Nicoll |
| Justificación GRASP y SOLID (mín. SRP y OCP) | Nicoll | Samuel |

Herramientas sugeridas: **draw.io / PlantUML** para UML; **Figma** o **draw.io** para mockups.

### 4.3 Fase 3 — Desarrollo

#### Entrega 1 (semanas 1–2): lógica de negocio sin BD

Se programa contra la interfaz `CrudDao<T>` usando DAO de **archivo de texto** (`data/*.txt`, ver ADR-0012), y se prueba con JUnit o una clase `Main` de consola. Así, al llegar la BD, solo se cambia la implementación del DAO. Mientras tanto la BD **se diseña ya** (MER y `sql/schema.sql`) y el modelo se escribe de acuerdo con ella.

| Bloque | Samuel (Líder) | Nicoll (Dev 1) | Isabella (Dev 2) |
|---|---|---|---|
| **Días 1–2** | Repo, ramas, proyecto Maven, `CrudDao<T>`, excepciones, `Validador`; **MER y `schema.sql` (con Nicoll e Isabella)** | `Persona`, `Cliente`, `Profesional` | `Vendible`, `Producto`, `Servicio`; mockups: pantalla principal y navegación |
| **Días 3–6** | `Cita` + `CitaDaoTexto`; `CitaServicio` con conflicto de horario (RF-10 a RF-13) | `ClienteServicio`, `ProfesionalServicio`, `ServicioServicio` + DAO de archivo de texto (RF-01, 02, 06 a 09) | `ProductoServicio`, control de stock y alerta de stock bajo (RF-03 a 05, 19); mockups de Productos y Clientes |
| **Días 7–10** | Reglas `ReglaCita` (RF-26); `Venta`, `DetalleVenta` y `VentaServicio` con descuento de stock (RF-14 a 17); interfaz `NotificadorCorreo` con implementación simulada (RF-27) | Estadísticas de cliente/admin y baja rotación (RF-18, 28 a 31) | Mockups de Citas, Ventas, Estadísticas y Probador/Asistente; validaciones de entrada |
| **Días 11–14** | Integración, `Main` de demostración, pruebas cruzadas, documento Fase 1 y 2, diagramas de paquetes y clases | Pruebas JUnit de sus servicios; Javadoc; justificación GRASP/SOLID | Pruebas JUnit de sus servicios; Javadoc; mockups finales; tabla de tareas |

**Criterio de aceptación de la Entrega 1:** un `Main` de consola (o los tests) permite registrar cliente, profesional, servicio y producto; agendar una cita rechazando conflictos; vender productos y servicios descontando stock; y consultar stock bajo y estadísticas. Los mockups cubren todos los módulos. El MER y `schema.sql` están revisados por los tres.

#### Entrega 2 (semanas 3 en adelante)

| Bloque | Samuel (Líder) | Nicoll (Dev 1) | Isabella (Dev 2) |
|---|---|---|---|
| **Sem. 3–4** | `ConexionBD`, DAO JDBC de Cita y Venta con transacciones | DAO JDBC de Cliente, Profesional, Servicio | DAO JDBC de Producto; carga inicial de datos de prueba |
| **Sem. 5–7** | Vistas FXML de Citas y Ventas | Vistas FXML de Clientes, Servicios, Estadísticas | Vista principal, `estilos.css`, Productos y navegación |
| **Sem. 8–9** | Factura por correo real, integración, tag `v1.0-mvp` | Asistente con 3–4 herramientas (RF-22/23) | Probador virtual (stub + API opcional) (RF-21) |

**Congelamiento:** el MVP se cierra antes de iniciar la IA.

#### Tabla de tareas (se actualiza cada sprint)

| ID | Tarea | Responsable | Rama | Estado |
|---|---|---|---|---|
| T-01 | Proyecto base, `ConexionBD`, script SQL | Samuel | `feature/nucleo` | ⬜ Pendiente |
| T-02 | Excepciones y validadores | Samuel | `feature/nucleo` | ⬜ Pendiente |
| T-03 | Clientes (modelo, DAO, servicio, GUI) | Nicoll | `feature/clientes` | ⬜ Pendiente |
| T-04 | Profesionales y Servicios | Nicoll | `feature/servicios` | ⬜ Pendiente |
| T-05 | Productos e Inventario | Isabella | `feature/productos` | ⬜ Pendiente |
| T-06 | Ventana principal y navegación | Isabella | `feature/gui-base` | ⬜ Pendiente |
| T-07 | Citas + conflictos de horario | Samuel | `feature/citas` | ⬜ Pendiente |
| T-08 | Reglas de citas (anticipo, confirmación, recordatorio) | Samuel | `feature/citas` | ⬜ Pendiente |
| T-09 | Ventas + descuento de stock | Samuel / Isabella | `feature/ventas` | ⬜ Pendiente |
| T-10 | Factura por correo (interfaz + implementación) | Samuel | `feature/ventas` | ⬜ Pendiente |
| T-11 | Estadísticas y BI (baja rotación) | Nicoll | `feature/estadisticas` | ⬜ Pendiente |
| T-12 | Asistente inteligente (herramientas) | Nicoll | `feature/ia-assistant` | ⬜ Pendiente |
| T-13 | Probador virtual | Isabella | `feature/ia-probador` | ⬜ Pendiente |
| T-14 | Tabla de trazabilidad GRASP/SOLID | Todos (cada uno su código) | `docs/trazabilidad` | ⬜ Pendiente |

Leyenda: ⬜ Pendiente · 🟨 En progreso · ✅ Completada

### 4.4 Fase 4 — Documentación

| Entregable | Responsable |
|---|---|
| Descripción de capas y diagrama de paquetes final | Samuel |
| Diagrama de clases final + descripción de cada clase | Cada dev (sus clases); Samuel consolida |
| MER y diccionario de datos | Nicoll |
| Tabla consolidada GRASP / SOLID / Código Limpio con referencia a clase y método | Isabella |
| Reporte final de Git (commits por integrante, ramas creadas y fusionadas) | Samuel |
| Manual breve de instalación y uso | Isabella |

---

## 5. Estrategia de Git

- **Repositorio:** `maison-glow` (nombre sugerido) · **URL:** _por definir_ · **Descripción:** Sistema de gestión con herramientas de IA para un negocio de belleza en Valledupar.
- **Ramas**
  - `main`: versión estable y entregable. **Protegida**; solo recibe merges desde `develop` al cierre de cada fase (con tag).
  - `develop`: integración continua del equipo.
  - `feature/<modulo>`: una por funcionalidad (clientes, servicios, productos, citas, ventas, estadisticas, ia-assistant, ia-probador, gui-base, nucleo). Se crean desde `develop`.
  - `docs/<tema>`: documentación.
- **Flujo:** `feature/*` → Pull Request a `develop` → revisión del líder (mínimo 1 aprobación) → *merge* → borrar la rama.
- **Commits** (Conventional Commits, en español): `feat(citas): valida conflicto de horario`, `fix(ventas): corrige total`, `docs(adr): agrega ADR-0004`, `refactor`, `test`.
- **Reglas:** commits pequeños y frecuentes **por cada integrante** (esto es evidencia evaluable); nunca commitear directo a `main`; `git pull --rebase` antes de empujar.
- **Archivos a ignorar (`.gitignore`):** `target/`, `build/`, `nbproject/private/`, `*.db`, `.env`, `config.properties` con claves. **Las claves de API nunca se suben.**
- **Evidencia para el docente:** `git shortlog -sn`, `git log --graph --oneline --all` y capturas de PR.

---

## 6. Definición de "Terminado" (DoD)

Una tarea se da por cerrada cuando:

1. Compila sin advertencias importantes y se probó manualmente (o con JUnit en servicios críticos).
2. Entradas validadas; excepciones manejadas y mostradas al usuario con mensaje claro.
3. Nombres en `camelCase` / `PascalCase`, métodos cortos de una sola responsabilidad, sin comentarios innecesarios.
4. Javadoc en las clases públicas.
5. PR aprobado por el líder y fusionado en `develop`.
6. Fila actualizada en la tabla de tareas.

---

## 7. Riesgos y mitigación

| Riesgo | Prob. | Mitigación |
|---|---|---|
| Alcance demasiado grande | Alta | Priorización MVP / deseable / experimental (sección 2) |
| Conflictos de merge en vistas `.fxml` | Media | Una vista por dev; no editar la del compañero sin avisar |
| Entrega 1 en 2 semanas con alcance grande | Alta | Lógica con DAO de archivo de texto y probada por consola/JUnit; UI solo en mockups; IA y JDBC quedan para la entrega 2 |
| API de IA no disponible o de pago | Alta | Interfaces + implementación simulada; la demo funciona sin internet |
| Desigualdad de aporte en commits | Media | Revisión semanal de `git shortlog`; tareas asignadas por dueño |
| Cambio de requisitos del docente | Baja | ADR para registrar cambios de decisión |

---

## 8. Rituales del equipo

- **Reunión semanal de 30 min** (inicio de sprint): revisar la tabla de tareas, asignar, desbloquear.
- **Revisión de PR** dentro de 48 h.
- **Cierre de sprint:** demo corta entre los tres y actualización de la tabla.
- **Canal único** (WhatsApp/Discord) para bloqueos; decisiones importantes se registran como ADR.

---

## 9. Mapa requisito → módulo → responsable

| Módulo | RF | Responsable |
|---|---|---|
| Clientes | 01, 02 | Nicoll |
| Productos / Inventario | 03, 04, 05, 19 | Isabella |
| Servicios / Profesionales | 06, 07, 08, 09 | Nicoll |
| Citas | 10, 11, 12, 13, 26 | Samuel |
| Ventas / Factura | 14, 15, 16, 17, 27 | Samuel (+ Isabella en GUI) |
| Estadísticas / BI | 18, 28, 29, 30, 31 | Nicoll |
| GUI transversal | 20 | Isabella |
| Validación y errores | 24, 25 | Samuel (núcleo) |
| Probador virtual | 21 | Isabella |
| Asistente inteligente | 22, 23 | Nicoll |

---

## 10. Estructura del repositorio

```
maison-glow/
├── PLANIFICACION.md
├── README.md
├── pom.xml
├── docs/
│   ├── README.md
│   ├── adr/                  # Registros de decisiones de arquitectura
│   ├── fase1/                # Documento de Fase 1
│   ├── fase2/                # UML, mockups
│   ├── fase3/                # Evidencia de commits, trazabilidad
│   └── fase4/                # MER, diccionario de datos, reporte final
├── sql/
│   └── schema.sql
└── src/main/java/com/maisonglow/
    ├── modelo/  dao/  servicio/  vista/  ia/  util/  excepcion/
```
