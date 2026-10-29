# Fase 2 — Diseño de clases y arquitectura · Maison Glow

> Documento base de la **Entrega 1**. Sigue el orden de la rúbrica de la Fase 2 y deja listo lo que la Fase 3 implementa
> (clases básicas, constructores, getters/setters, herencia, polimorfismo, interfaces, colecciones).
> Contexto y decisiones: [`../../PLANIFICACION.md`](../../PLANIFICACION.md) · [`../adr/`](../adr/README.md)

> **Nomenclatura en el código.** Los identificadores, el Javadoc y los commits están en inglés; los mensajes al usuario, en español.
> Este documento usa los nombres del dominio en español; en el código se corresponden así:
>
> | Diseño (español) | Código (inglés) |
> |---|---|
> | `Persona`, `Cliente`, `Profesional` | `Person`, `Customer`, `Professional` |
> | `Producto`, `Tratamiento`, `Vendible` | `Product`, `Treatment`, `Sellable` |
> | `Cita`, `EstadoCita`, `ReglaCita` | `Appointment`, `AppointmentStatus`, `AppointmentRule` |
> | `Venta`, `DetalleVenta` | `Sale`, `SaleLine` |
> | `CitaServicio`, `VentaServicio`, `TratamientoServicio` | `AppointmentService`, `SaleService`, `TreatmentService` |
> | `NotificadorCorreo`, `Validador` | `EmailNotifier`, `Validator` |
> | Paquetes `modelo`, `servicio`, `servicio.regla`, `vista`, `excepcion`, `ia` | `model`, `service`, `service.rule`, `ui`, `exception`, `ai` |
> | Métodos (`descontarStock`, `calcularTotal`, `agendar`) | `deductStock`, `calculateTotal`, `book` |

**Contenido**
1. [Arquitectura y diagrama de paquetes](#1-arquitectura-y-diagrama-de-paquetes)
2. [Identificación de clases](#2-identificación-de-clases)
3. [Definición de clases (atributos, métodos, responsabilidades)](#3-definición-de-clases)
4. [Diagramas de clases y relaciones](#4-diagramas-de-clases-y-relaciones)
5. [Herencia, interfaces y clases abstractas](#5-herencia-interfaces-y-clases-abstractas)
6. [Diseño preliminar de la interfaz gráfica](#6-diseño-preliminar-de-la-interfaz-gráfica)
7. [Patrones GRASP](#7-patrones-grasp)
8. [Principios SOLID](#8-principios-solid)
9. [Guía de implementación para la Fase 3](#9-guía-de-implementación-para-la-fase-3)

> **Convenciones de este documento**
> - Paquete raíz: `com.maisonglow`. Dinero como `double` (simplicidad del curso); fechas con `java.time`.
> - Los `id` son `int`; los asigna el DAO al crear (0 = aún no guardado). Se diseñaron para coincidir con las tablas de `sql/schema.sql`.
> - Las clases de negocio se nombran `XxxServicio`, sin excepciones. La entidad que se agenda y se vende se llama `Tratamiento` (no `Servicio`) para no chocar con ese sufijo ([ADR-0013](../adr/0013-entidad-tratamiento.md)); en la interfaz puede seguir mostrándose como "servicio".

---

## 1. Arquitectura y diagrama de paquetes

Arquitectura en **3 capas** ([ADR-0001](../adr/0001-arquitectura-en-tres-capas.md)). Regla: `vista → servicio → dao`; la IA se conecta solo a través de interfaces ([ADR-0005](../adr/0005-ia-desacoplada-por-interfaces.md)).

```mermaid
flowchart TB
    subgraph PRES["Capa de presentación"]
        VISTA["vista<br/>(FXML + CSS)"]
        VCTRL["vista.controlador<br/>(controladores JavaFX)"]
    end
    subgraph NEG["Capa de negocio"]
        SERV["servicio<br/>(ClienteServicio, CitaServicio, VentaServicio...)"]
        REGLA["servicio.regla<br/>(ReglaCita y estrategias)"]
        MODELO["modelo<br/>(entidades del dominio)"]
    end
    subgraph DATOS["Capa de datos"]
        DAO["dao<br/>(CrudDao + implementaciones)"]
    end
    subgraph IA["Módulos de IA"]
        IAPKG["ia<br/>(interfaces + simulado/API)"]
    end
    subgraph TRANS["Transversales"]
        UTIL["util<br/>(Validador, ConexionBD)"]
        EXC["excepcion"]
    end

    VISTA --> VCTRL
    VCTRL --> SERV
    SERV --> REGLA
    SERV --> DAO
    SERV --> MODELO
    DAO --> MODELO
    IAPKG --> SERV
    SERV --> UTIL
    DAO --> UTIL
    SERV --> EXC
    DAO --> EXC
```

| Capa | Paquetes | Responsabilidad |
|---|---|---|
| Presentación | `vista`, `vista.controlador` | Mostrar datos, capturar eventos y presentar errores. No contiene reglas de negocio ni SQL |
| Negocio | `servicio`, `servicio.regla`, `modelo` | Reglas, validaciones, coordinación de operaciones; entidades con su comportamiento propio |
| Datos | `dao` | Persistir y recuperar entidades. En la Entrega 1: archivos de texto; luego JDBC ([ADR-0012](../adr/0012-persistencia-en-archivos-de-texto.md)) |
| IA | `ia` | Probador virtual y asistente, detrás de interfaces; las herramientas del asistente llaman a `servicio` |
| Transversal | `util`, `excepcion` | Validaciones, conexión, jerarquía de excepciones ([ADR-0008](../adr/0008-manejo-de-excepciones.md)) |

---

## 2. Identificación de clases

| # | Nombre de la clase | Tipo | Descripción y rol en el sistema |
|---|---|---|---|
| 1 | `Persona` | Abstracta | Datos comunes de toda persona (documento, nombre, teléfono, correo). Raíz de la jerarquía |
| 2 | `Cliente` | Concreta | Persona que compra y agenda citas; correo para la factura |
| 3 | `Profesional` | Concreta | Persona que presta servicios; define su horario y los servicios que ofrece |
| 4 | `Vendible` | Interfaz | Contrato de todo lo que puede venderse (nombre y precio). Permite ventas polimórficas |
| 5 | `Producto` | Concreta | Artículo físico con stock y stock mínimo |
| 6 | `Tratamiento` | Concreta | Tratamiento de belleza (corte, manicura...) con precio y duración |
| 7 | `Cita` | Concreta | Reserva de un servicio con un profesional en una fecha/hora; conoce su estado |
| 8 | `Venta` | Concreta | Transacción comercial de un cliente; agrupa detalles y calcula el total |
| 9 | `DetalleVenta` | Concreta | Línea de una venta: un `Vendible`, cantidad y precio al momento de vender |
| 10 | `EstadoCita` | Enumeración | Estados del ciclo de vida de una cita |
| 11 | `CrudDao<T>` | Interfaz | Contrato CRUD de la capa de datos |
| 12 | `ClienteDaoTexto`, `ProductoDaoTexto`, ... | Concreta | Implementaciones en archivo de texto de los DAO (Entrega 1), sobre la base `TextFileDao<T>` |
| 13 | `ReglaCita` | Interfaz | Contrato de una regla de negocio de citas |
| 14 | `ReglaAnticipo`, `ReglaConfirmacion`, `ReglaRecordatorio` | Concretas | Reglas concretas de RF-26 |
| 15 | `ClienteServicio`, `ProfesionalServicio`, `TratamientoServicio`, `ProductoServicio` | Concretas | Lógica de negocio de cada módulo (CRUD con validación) |
| 16 | `CitaServicio` | Concreta | Agendamiento, disponibilidad, conflicto de horario y aplicación de reglas |
| 17 | `VentaServicio` | Concreta | Registro de ventas, descuento de stock y envío de factura |
| 18 | `EstadisticaServicio` | Concreta | Estadísticas de cliente y administrador; baja rotación |
| 19 | `NotificadorCorreo` / `NotificadorCorreoSimulado` | Interfaz / Concreta | Envío de facturas por correo, desacoplado del proveedor |
| 20 | `Validador` | Concreta (utilidad) | Validaciones reutilizables de datos de entrada |
| 21 | `ValidacionException`, `ReglaNegocioException`, `AccesoDatosException` | Concretas | Jerarquía de excepciones del sistema |

> Las clases del paquete `ia` (`ProbadorVirtual`, `AsistenteInteligente`, `HerramientaAsistente`, `RegistroHerramientas`) se diseñan en la Entrega 2 ([ADR-0005](../adr/0005-ia-desacoplada-por-interfaces.md), [ADR-0006](../adr/0006-herramientas-del-asistente-tipo-mcp.md)).

---

## 3. Definición de clases

### 3.1 Modelo de dominio

#### Clase: `Persona`
**Tipo:** Abstracta · **Responsabilidad principal:** Concentrar los datos y comportamiento comunes de las personas del negocio para evitar duplicarlos en `Cliente` y `Profesional`.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `id` | `int` | Identificador (asignado al guardar) |
| `documento` | `String` | Cédula o documento único |
| `nombre` | `String` | Nombre completo |
| `telefono` | `String` | Teléfono de contacto |
| `correo` | `String` | Correo electrónico |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Persona(int, String, String, String, String)` | — | Constructor completo (protegido) |
| `getId()`, `getDocumento()`, `getNombre()`, `getTelefono()`, `getCorreo()` | tipo del atributo | Getters |
| `setId(int)`, `setNombre(String)`, `setTelefono(String)`, `setCorreo(String)` | `void` | Setters (el documento no cambia) |
| `getDescripcionRol()` | `String` | **Abstracto**: rol de la persona ("Cliente", "Profesional") |
| `toString()` / `equals(Object)` / `hashCode()` | `String` / `boolean` / `int` | Sobrescritos; igualdad por `documento` |

#### Clase: `Cliente`
**Tipo:** Concreta (extiende `Persona`) · **Responsabilidad principal:** Representar al cliente del negocio, con su fecha de registro, para asociarlo a citas y compras.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `fechaRegistro` | `LocalDate` | Día en que se registró |
| `inasistencias` | `int` | Citas a las que no asistió (insumo para penalización) |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Cliente(...)` | — | Constructor que llama a `super(...)` |
| `getFechaRegistro()`, `getInasistencias()` | tipo del atributo | Getters |
| `registrarInasistencia()` | `void` | Incrementa `inasistencias` |
| `getDescripcionRol()` | `String` | Sobrescrito: devuelve `"Cliente"` |

#### Clase: `Profesional`
**Tipo:** Concreta (extiende `Persona`) · **Responsabilidad principal:** Representar al especialista, los servicios que presta y su horario laboral, para decidir si puede atender una cita.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `especialidad` | `String` | Área principal (peluquería, maquillaje...) |
| `horaInicio` | `LocalTime` | Inicio de su jornada |
| `horaFin` | `LocalTime` | Fin de su jornada |
| `tratamientos` | `List<Tratamiento>` | Tratamientos que ofrece (`ArrayList`) |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Profesional(...)` | — | Constructor; inicializa `servicios` vacío |
| `agregarTratamiento(Tratamiento)` / `quitarTratamiento(Tratamiento)` | `void` | Mantiene la lista (sin duplicados) |
| `ofreceTratamiento(Tratamiento)` | `boolean` | ¿Presta este servicio? |
| `trabajaEn(LocalDateTime inicio, LocalDateTime fin)` | `boolean` | ¿El intervalo cae dentro de su jornada? |
| `getTratamientos()` | `List<Tratamiento>` | Copia inmodificable de la lista |
| `getDescripcionRol()` | `String` | Sobrescrito: devuelve `"Profesional"` |

#### Interfaz: `Vendible`
**Tipo:** Interfaz · **Responsabilidad principal:** Definir lo mínimo que cualquier ítem vendible debe exponer, para que `Venta` los trate de forma uniforme.

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `getId()` | `int` | Identificador del ítem |
| `getNombre()` | `String` | Nombre mostrado en la factura |
| `getPrecio()` | `double` | Precio unitario actual |

#### Clase: `Producto`
**Tipo:** Concreta (implementa `Vendible`) · **Responsabilidad principal:** Modelar un artículo y proteger la coherencia de su inventario.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `id` | `int` | Identificador |
| `nombre` | `String` | Nombre comercial |
| `categoria` | `String` | Categoría (cabello, piel, uñas...) |
| `precio` | `double` | Precio de venta |
| `stock` | `int` | Unidades disponibles |
| `stockMinimo` | `int` | Umbral de alerta de inventario bajo |
| `activo` | `boolean` | Si está disponible para venta |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Producto(...)` | — | Constructor completo |
| getters / setters | — | Encapsulamiento de todos los atributos |
| `hayStock(int cantidad)` | `boolean` | ¿Alcanza el inventario? |
| `descontarStock(int cantidad)` | `void` | Resta unidades; lanza `ReglaNegocioException` si no alcanzan |
| `aumentarStock(int cantidad)` | `void` | Suma unidades (reposición) |
| `estaBajoStockMinimo()` | `boolean` | `stock <= stockMinimo` (RF-19) |
| `toString()` / `equals` / `hashCode` | — | Sobrescritos; igualdad por `id` |

#### Clase: `Tratamiento`
**Tipo:** Concreta (implementa `Vendible`) · **Responsabilidad principal:** Describir un tratamiento de belleza con su precio y duración, que determina cuánto tiempo bloquea la agenda.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `id` | `int` | Identificador |
| `nombre` | `String` | Nombre del servicio |
| `descripcion` | `String` | Detalle |
| `precio` | `double` | Precio |
| `duracionMinutos` | `int` | Duración (RF-07) |
| `activo` | `boolean` | Si se ofrece actualmente |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Tratamiento(...)` | — | Constructor completo |
| getters / setters | — | Encapsulamiento |
| `calcularFin(LocalDateTime inicio)` | `LocalDateTime` | `inicio + duracionMinutos` |
| `toString()` / `equals` / `hashCode` | — | Sobrescritos; igualdad por `id` |

#### Clase: `Cita`
**Tipo:** Concreta · **Responsabilidad principal:** Representar una reserva y gobernar sus cambios de estado y su cruce con otras citas.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `id` | `int` | Identificador |
| `cliente` | `Cliente` | Quien reserva |
| `profesional` | `Profesional` | Quien atiende |
| `tratamiento` | `Tratamiento` | Tratamiento reservado |
| `fechaHora` | `LocalDateTime` | Inicio de la cita |
| `estado` | `EstadoCita` | `PENDIENTE`, `CONFIRMADA`, `COMPLETADA`, `CANCELADA`, `NO_ASISTIO` |
| `anticipo` | `double` | Depósito pagado |
| `recordatorio` | `LocalDateTime` | Momento programado del recordatorio |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Cita(...)` | — | Constructor; estado inicial `PENDIENTE` |
| getters / setters | — | Encapsulamiento |
| `getFechaHoraFin()` | `LocalDateTime` | `tratamiento.calcularFin(fechaHora)` |
| `seCruzaCon(Cita otra)` | `boolean` | Intervalos solapados (inicio < fin ajeno y fin > inicio ajeno) |
| `confirmar()` / `cancelar()` / `completar()` | `void` | Transiciones de estado; `ReglaNegocioException` si la transición es inválida |
| `marcarNoAsistio()` | `void` | Cambia a `NO_ASISTIO` e incrementa inasistencias del cliente |
| `estaActiva()` | `boolean` | `PENDIENTE` o `CONFIRMADA` (las únicas que bloquean la agenda) |

#### Clase: `Venta`
**Tipo:** Concreta · **Responsabilidad principal:** Agrupar los detalles de una venta y calcular su total.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `id` | `int` | Identificador |
| `cliente` | `Cliente` | Comprador |
| `fecha` | `LocalDateTime` | Momento de la venta |
| `detalles` | `List<DetalleVenta>` | Líneas de la venta (`ArrayList`) |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `Venta(Cliente)` | — | Constructor; fecha = ahora, detalles vacíos |
| `agregarDetalle(Vendible, int cantidad)` | `DetalleVenta` | **Crea** el detalle (Creator) y lo añade; si el ítem ya está, suma cantidad |
| `calcularTotal()` | `double` | Suma de subtotales |
| `getDetalles()` | `List<DetalleVenta>` | Vista inmodificable |
| `getCliente()`, `getFecha()`, `getId()` / `setId(int)` | — | Acceso |

#### Clase: `DetalleVenta`
**Tipo:** Concreta · **Responsabilidad principal:** Registrar una línea de la venta conservando el precio del momento (el precio del catálogo puede cambiar después).

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `item` | `Vendible` | Producto o servicio vendido (polimorfismo) |
| `cantidad` | `int` | Unidades |
| `precioUnitario` | `double` | Precio al vender |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `DetalleVenta(Vendible, int)` | — | Constructor; copia `item.getPrecio()` |
| `calcularSubtotal()` | `double` | `cantidad × precioUnitario` |
| `esProducto()` | `boolean` | `item instanceof Producto` (para descontar stock) |
| getters / `aumentarCantidad(int)` | — | Acceso y ajuste |

### 3.2 Capa de datos y reglas

#### Interfaz: `CrudDao<T>`
**Tipo:** Interfaz · **Responsabilidad principal:** Definir el contrato de persistencia que usan los servicios, sin revelar si hay archivos de texto, SQLite u otra tecnología.

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `crear(T)` | `T` | Guarda y devuelve la entidad con `id` asignado |
| `buscarPorId(int)` | `Optional<T>` | Busca una entidad |
| `listar()` | `List<T>` | Todas las entidades |
| `actualizar(T)` | `void` | Modifica una existente |
| `eliminar(int)` | `void` | Elimina por id |

Todos lanzan `AccesoDatosException`. Interfaces específicas (`CitaDao extends CrudDao<Cita>`) añaden consultas propias, p. ej. `listarPorProfesional(int)` y `listarPorFecha(LocalDate)`; `VentaDao` añade `listarPorRango(...)`.
**Implementaciones:** `XxxDaoTexto` (Entrega 1, archivo `.txt` + `Map` en memoria) y `XxxDaoJdbc` (Entrega 2).

#### Interfaz: `ReglaCita`
**Tipo:** Interfaz · **Responsabilidad principal:** Permitir añadir reglas de negocio de citas sin modificar `CitaServicio` ([ADR-0007](../adr/0007-reglas-de-cita-como-estrategias.md)).

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `aplicar(Cita cita)` | `void` | Valida o completa la cita; lanza `ReglaNegocioException` si incumple |
| `getNombre()` | `String` | Nombre para mensajes |

| Implementación | Qué hace |
|---|---|
| `ReglaAnticipo` | Exige un anticipo mínimo (`porcentaje` del precio del servicio, configurable) |
| `ReglaConfirmacion` | Exige antelación mínima (`horasMinimas`) para agendar y deja la cita `PENDIENTE` hasta confirmar |
| `ReglaRecordatorio` | Calcula `cita.recordatorio` (`horasAntes` antes del inicio) |

### 3.3 Capa de negocio

#### Clase: `CitaServicio`
**Tipo:** Concreta · **Responsabilidad principal:** Coordinar el agendamiento de citas garantizando que el profesional esté disponible y que se cumplan las reglas.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `citaDao` | `CitaDao` | Acceso a datos (inyectado) |
| `reglas` | `List<ReglaCita>` | Reglas configurables (inyectadas) |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `CitaServicio(CitaDao, List<ReglaCita>)` | — | Constructor con inyección de dependencias |
| `agendar(Cliente, Profesional, Tratamiento, LocalDateTime, double anticipo)` | `Cita` | Valida datos, que el profesional ofrezca el servicio y trabaje en ese horario, **sin conflicto** (RF-11/12), aplica reglas y guarda |
| `verificarDisponibilidad(Profesional, LocalDateTime inicio, LocalDateTime fin)` | `boolean` | Sin cruce con citas activas (reutilizado por el asistente) |
| `confirmar(int)` / `cancelar(int)` / `completar(int)` | `void` | Cambia el estado y persiste |
| `reprogramar(int, LocalDateTime)` | `Cita` | Modifica fecha (RF-13) revalidando disponibilidad |
| `listarPorFecha(LocalDate)` | `List<Cita>` | Agenda del día |

#### Clase: `VentaServicio`
**Tipo:** Concreta · **Responsabilidad principal:** Registrar ventas completas manteniendo coherente el inventario y notificando la factura.

| Atributo | Tipo de dato | Descripción |
|---|---|---|
| `ventaDao` | `VentaDao` | Persistencia de ventas |
| `productoDao` | `CrudDao<Producto>` | Para actualizar stock |
| `notificador` | `NotificadorCorreo` | Envío de factura |

| Método | Tipo de retorno | Descripción |
|---|---|---|
| `VentaServicio(VentaDao, CrudDao<Producto>, NotificadorCorreo)` | — | Constructor con inyección |
| `registrarVenta(Venta)` | `Venta` | Verifica que haya detalles y stock suficiente, descuenta stock de los productos, guarda y envía la factura (un fallo de correo no anula la venta) |
| `listarPorCliente(int)` | `List<Venta>` | Historial de compras |

#### Otras clases de negocio (resumen)

| Clase | Responsabilidad | Métodos principales |
|---|---|---|
| `ClienteServicio` | CRUD con validación de clientes (documento único, correo válido) | `registrar`, `actualizar`, `buscarPorId`, `buscarPorNombre`, `listar`, `eliminar` |
| `ProfesionalServicio` | CRUD de profesionales y vínculo con servicios | `registrar`, `asociarServicio(idProf, idServ)`, `listarPorServicio(idServ)` |
| `TratamientoServicio` | CRUD de tratamientos (precio > 0, duración > 0) | `registrar`, `actualizar`, `activar/desactivar`, `listar` |
| `ProductoServicio` | CRUD de productos y control de existencias | `registrar`, `actualizar`, `ajustarStock(id, delta)`, `listarStockBajo()` |
| `EstadisticaServicio` | Estadísticas (RF-18, 28–31) | `estadisticasCliente(id)`, `productosMasVendidos(n, desde, hasta)`, `productosBajaRotacion(n, desde, hasta)`, `serviciosMasSolicitados(n)` , `clientesFrecuentes(n)` — devuelven `Map<String,Integer>` (HashMap) |
| `NotificadorCorreo` | Contrato de envío de facturas | `enviarFactura(Cliente, Venta)` |
| `Validador` | Validaciones estáticas reutilizables | `requerirTexto`, `requerirCorreo`, `requerirPositivo`, `requerirTelefono` |

---

## 4. Diagramas de clases y relaciones

### 4.1 Dominio

```mermaid
classDiagram
    class Persona {
        <<abstract>>
        -int id
        -String documento
        -String nombre
        -String telefono
        -String correo
        +getDescripcionRol() String*
    }
    class Cliente {
        -LocalDate fechaRegistro
        -int inasistencias
        +registrarInasistencia() void
    }
    class Profesional {
        -String especialidad
        -LocalTime horaInicio
        -LocalTime horaFin
        +ofreceTratamiento(Tratamiento) boolean
        +trabajaEn(LocalDateTime, LocalDateTime) boolean
    }
    class Vendible {
        <<interface>>
        +getId() int
        +getNombre() String
        +getPrecio() double
    }
    class Producto {
        -int stock
        -int stockMinimo
        +descontarStock(int) void
        +estaBajoStockMinimo() boolean
    }
    class Tratamiento {
        -int duracionMinutos
        +calcularFin(LocalDateTime) LocalDateTime
    }
    class Cita {
        -LocalDateTime fechaHora
        -EstadoCita estado
        -double anticipo
        +seCruzaCon(Cita) boolean
        +confirmar() void
        +cancelar() void
    }
    class EstadoCita {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        COMPLETADA
        CANCELADA
        NO_ASISTIO
    }
    class Venta {
        -LocalDateTime fecha
        +agregarDetalle(Vendible, int) DetalleVenta
        +calcularTotal() double
    }
    class DetalleVenta {
        -int cantidad
        -double precioUnitario
        +calcularSubtotal() double
    }

    Persona <|-- Cliente
    Persona <|-- Profesional
    Vendible <|.. Producto
    Vendible <|.. Tratamiento
    Profesional "0..*" o-- "0..*" Tratamiento : ofrece
    Cita "*" --> "1" Cliente
    Cita "*" --> "1" Profesional
    Cita "*" --> "1" Tratamiento
    Cita --> EstadoCita
    Venta "1" *-- "1..*" DetalleVenta : compuesta por
    DetalleVenta "*" --> "1" Vendible
    Venta "*" --> "1" Cliente
```

### 4.2 Servicios, reglas y acceso a datos

```mermaid
classDiagram
    class CrudDao~T~ {
        <<interface>>
        +crear(T) T
        +buscarPorId(int) Optional~T~
        +listar() List~T~
        +actualizar(T) void
        +eliminar(int) void
    }
    class CitaDao {
        <<interface>>
        +listarPorProfesional(int) List~Cita~
        +listarPorFecha(LocalDate) List~Cita~
    }
    class CitaDaoTexto
    class CitaDaoJdbc
    class ReglaCita {
        <<interface>>
        +aplicar(Cita) void
        +getNombre() String
    }
    class ReglaAnticipo
    class ReglaConfirmacion
    class ReglaRecordatorio
    class NotificadorCorreo {
        <<interface>>
        +enviarFactura(Cliente, Venta) void
    }
    class NotificadorCorreoSimulado
    class CitaServicio {
        +agendar(...) Cita
        +verificarDisponibilidad(...) boolean
    }
    class VentaServicio {
        +registrarVenta(Venta) Venta
    }

    CrudDao <|-- CitaDao
    CitaDao <|.. CitaDaoTexto
    CitaDao <|.. CitaDaoJdbc
    ReglaCita <|.. ReglaAnticipo
    ReglaCita <|.. ReglaConfirmacion
    ReglaCita <|.. ReglaRecordatorio
    NotificadorCorreo <|.. NotificadorCorreoSimulado
    CitaServicio --> CitaDao : usa
    CitaServicio o-- "0..*" ReglaCita : aplica
    VentaServicio --> NotificadorCorreo : usa
    VentaServicio --> CrudDao : usa (Producto)
```

### 4.3 Tabla de relaciones

| Relación | Tipo | Justificación |
|---|---|---|
| `Cliente`, `Profesional` → `Persona` | **Herencia** | Comparten datos y comportamiento; "es una" persona |
| `Producto`, `Tratamiento` → `Vendible` | **Realización** (interfaz) | No comparten estado, solo el contrato de ser vendibles |
| `Venta` ◆— `DetalleVenta` | **Composición** | Un detalle no existe sin su venta; se crea y se elimina con ella |
| `Profesional` ◇— `Tratamiento` | **Agregación** (N:M) | El servicio existe sin el profesional (tabla `profesional_tratamiento`) |
| `Cita` → `Cliente`/`Profesional`/`Tratamiento` | **Asociación** | La cita referencia entidades independientes |
| `DetalleVenta` → `Vendible` | **Asociación** polimórfica | Apunta a un `Producto` o a un `Tratamiento` indistintamente |
| `CitaServicio` ◇— `ReglaCita` | **Agregación** | Las reglas se inyectan y existen fuera del servicio |
| `XxxServicio` ⇢ `CrudDao`, `NotificadorCorreo` | **Dependencia** (inyectada) | Dependen de abstracciones, no de implementaciones |

---

## 5. Herencia, interfaces y clases abstractas

| Mecanismo | Dónde | Beneficio |
|---|---|---|
| **Clase abstracta** | `Persona` (método abstracto `getDescripcionRol()`) | Reutiliza atributos y getters; obliga a cada subclase a definir su rol |
| **Herencia** | `Persona` → `Cliente`, `Profesional` | Elimina duplicación |
| **Interfaz** | `Vendible`, `CrudDao<T>`, `ReglaCita`, `NotificadorCorreo` | Contratos desacoplados e intercambiables |
| **Polimorfismo por interfaz** | `Venta` recorre `List<DetalleVenta>` que apuntan a `Vendible`; `CitaServicio` itera `List<ReglaCita>` | Código sin `if (tipo == ...)` |
| **Polimorfismo por herencia** | `persona.getDescripcionRol()` | Cada subclase responde a su manera |
| **Métodos sobrescritos** | `toString()`, `equals()`, `hashCode()`, `getDescripcionRol()`, `getPrecio()` | Representación y comparación coherentes |
| **Colecciones** | `ArrayList` (`tratamientos`, `detalles`, resultados de `listar`), `HashMap` (caché de los DAO; estadísticas), `Optional` | Requisito de la Fase 3 |

---

## 6. Diseño preliminar de la interfaz gráfica

Tecnología: **JavaFX + FXML + CSS** ([ADR-0009](../adr/0009-ui-con-javafx.md)). Los mockups finales (Figma o draw.io) se guardan en `docs/fase2/mockups/` y se enlazan aquí. Responsable: Isabella, con apoyo de Nicoll.

| # | Pantalla | Componentes principales | Eventos → servicio | RF |
|---|---|---|---|---|
| M1 | Principal / navegación | Menú lateral, barra superior, panel de contenido | Navegar entre módulos | RF-20 |
| M2 | Clientes | Tabla, formulario, buscador | Guardar/Buscar → `ClienteServicio` | RF-01, 02 |
| M3 | Profesionales y servicios | Tablas, lista de servicios por profesional | Asociar → `ProfesionalServicio`, `TratamientoServicio` | RF-06 a 09 |
| M4 | Productos e inventario | Tabla con alerta de stock bajo, formulario | Guardar/Ajustar → `ProductoServicio` | RF-03 a 05, 19 |
| M5 | Agenda de citas | Selector de fecha, lista del día, formulario de cita | Agendar/Cancelar → `CitaServicio` | RF-10 a 13 |
| M6 | Nueva venta | Cliente, productos/servicios, carrito, total | Agregar/Cobrar → `VentaServicio` | RF-14 a 17 |
| M7 | Estadísticas | Filtros de fecha, tablas/gráficos, baja rotación | Consultar → `EstadisticaServicio` | RF-18, 28 a 31 |
| M8 | Probador virtual *(Entrega 2)* | Foto, selector de transformación, resultado | Generar → `ProbadorVirtual` | RF-21 |
| M9 | Asistente *(Entrega 2)* | Chat, confirmación de acciones | Enviar → `AsistenteInteligente` | RF-22, 23 |

Wireframes iniciales de referencia:

```
M1 Principal                           M5 Agenda de citas
┌────────────────────────────────┐     ┌────────────────────────────────────────┐
│ Maison Glow             [Admin]│     │ Citas            Fecha: [08/10/2026 ▾] │
├─────────┬──────────────────────┤     ├──────────────────────┬─────────────────┤
│ Inicio  │                      │     │ 09:00 Ana · Tinte    │ Cliente  [____▾]│
│ Clientes│  Resumen del día     │     │ 11:00 Luz · Manicura │ Profes.  [____▾]│
│ Citas   │  · Citas hoy: 6      │     │ 14:00 (libre)        │ Servicio [____▾]│
│ Ventas  │  · Stock bajo: 3     │     │ ...                  │ Hora     [__:__]│
│ Product.│                      │     │                      │ Anticipo [_____]│
│ Estadíst│                      │     │ [Cancelar cita]      │ [Agendar]       │
└─────────┴──────────────────────┘     └──────────────────────┴─────────────────┘
```

Los errores (`ValidacionException`, `ReglaNegocioException`) se muestran como alertas con mensaje claro; la vista nunca muestra trazas técnicas.

---

## 7. Patrones GRASP

Clases y métodos **reales** de `src/main/java/com/maisonglow` (nombres del código, ver la tabla de nomenclatura al inicio). Diagramas: [`diagramas.md`](diagramas.md).

| Patrón | Clase y método | Razón de diseño |
|---|---|---|
| **Creator** | `Sale.addLine(Sellable, int)` crea el `SaleLine` | `Sale` contiene (compone) sus líneas y tiene el ítem y la cantidad para inicializarlas; suma cantidad si el ítem ya está |
| **Creator** | `AppointmentService.book(...)` crea el `Appointment` | El servicio tiene cliente, profesional, tratamiento y hora, y aplica las reglas antes de guardar |
| **Creator** | `CustomerService.register`, `ProductService.register`, `TreatmentService.register`, `ProfessionalService.register` | Cada servicio valida el DTO y crea su entidad ya válida |
| **Controller** | `AppointmentService`, `SaleService`, `CustomerService`, `ProductService`, `ProfessionalService`, `TreatmentService`, `StatisticsService` | Reciben las operaciones del sistema sin ser parte de la UI; la interfaz de la Entrega 2 solo delegará en ellos |
| **Information Expert** | `Product.deductStock(int)`, `Product.isBelowMinimum()` | El producto conoce su stock y su mínimo; nadie más lo modifica |
| **Information Expert** | `Appointment.overlapsWith(Appointment)`, `Appointment.rescheduledTo(LocalDateTime)` | La cita conoce su inicio y su fin (la duración viene de `Treatment.calculateEnd`) |
| **Information Expert** | `Sale.calculateTotal()`, `SaleLine.calculateSubtotal()` | Cada una tiene los datos para calcular su parte |
| **Information Expert** | `Professional.worksDuring(LocalDateTime, LocalDateTime)`, `Professional.offers(Treatment)` | Conoce su jornada y los tratamientos que presta |
| **Information Expert** | `AppointmentStatus.canChangeTo(AppointmentStatus)` | El enum conoce qué transiciones son válidas; `Appointment.confirm/cancel/complete/markNoShow` solo le preguntan |
| **Information Expert** | `Appointment.markNoShow()` → `Customer.registerNoShow()` | La cita sabe que el cliente faltó; el cliente conoce su contador |
| **Low Coupling** | `AppointmentService` depende de `AppointmentDao`, `CrudDao<Customer>` y `AppointmentRule` (interfaces) | Cambiar archivos de texto por JDBC o agregar una regla no afecta al servicio |
| **High Cohesion** | `DepositRule`, `ConfirmationRule`, `ReminderRule`; `Validator`; `InvoiceFormatter` | Cada clase hace una sola cosa bien definida |
| **High Cohesion** | `StatisticsService`, `LowRotationService`, `PromotionService` | La estadística, la baja rotación y las promociones se separaron en tres servicios con un propósito cada uno |
| **Polymorphism** | `Sellable.deductStock/addStock/hasStock` (`Product` sobrescribe; `Treatment` usa el método por defecto) | `SaleService.register` descuenta el stock de cualquier línea sin preguntar su tipo |
| **Polymorphism** | `Person.getRoleDescription()`; `AppointmentRule.apply(Appointment)` | Cada subclase o regla responde a su manera |
| **Pure Fabrication** | `Validator`, `TextFileDao`, `TextFileStore`, `TextLineCodec`, `InvoiceFormatter`, `SimulatedEmailNotifier`, `SimulatedAdminNotifier` | Clases que no existen en el dominio, creadas para lograr bajo acoplamiento y reutilización |
| **Indirection** | `CrudDao<T>` entre servicios y almacenamiento; `EmailNotifier` entre el llamador (`Main`) y el envío del correo | Aísla el negocio de la tecnología de persistencia y del proveedor de correo |
| **Protected Variations** | `AppointmentRule`, `EmailNotifier`, `AdminNotifier`, `CrudDao<T>`, `SettingsService` | Protegen al sistema de cambios en reglas, proveedor de correo, persistencia y parámetros del negocio |

---

## 8. Principios SOLID

| Principio | Clase y lugar | Argumento |
|---|---|---|
| **SRP** — Responsabilidad única | `Product` (solo inventario y precio), `CustomerDaoText` (solo persistencia), `AppointmentService` (solo agendamiento), `Validator` (solo validación), `SettingsService` (solo parámetros), `InvoiceFormatter` (solo el texto de la factura) | Cada clase tiene **un motivo de cambio**: cambiar el formato del archivo no toca el negocio; cambiar la factura no toca ventas |
| **SRP** | `TextFileDao` (plantilla), `TextFileStore` (lectura y escritura del archivo), `TextLineCodec` (separador `\|` y escape) | La persistencia se dividió en tres piezas; un DAO concreto solo define `toFields` y `fromFields` |
| **OCP** — Abierto/cerrado | `AppointmentService` → lista de `AppointmentRule`; `Sale.addLine(Sellable, int)` | Una regla nueva (p. ej. una penalización) o un nuevo `Sellable` se **agrega como clase nueva**; `AppointmentService` y `Sale` no se modifican |
| **LSP** — Sustitución de Liskov | `Customer`/`Professional` por `Person`; `Product`/`Treatment` por `Sellable`; las `XxxDaoText` por `CrudDao<T>` | Se usan donde se espera la abstracción sin romper el comportamiento; todos los DAO lanzan `DataAccessException` |
| **ISP** — Segregación de interfaces | `EmailNotifier` (1 método), `AdminNotifier` (1 método), `AppointmentRule` (2), `SettingDao` (2, sin `CrudDao`), `AppointmentDao` agrega solo `findByProfessional` y `findByDate` | Ninguna clase implementa métodos que no usa; `SettingDao` no hereda de `CrudDao` porque su clave es texto |
| **DIP** — Inversión de dependencias | Constructores de `AppointmentService`, `SaleService`, `StatisticsService`, `LowRotationService`, `PromotionService` y de las tres reglas | Reciben interfaces o servicios por constructor; ningún servicio hace `new` de un DAO ni de un notificador. **Solo `Main` arma las piezas** |

### 8.1 Limitaciones conocidas

Para no afirmar más de lo que hace el código:

| Dónde | Qué pasa | Principio afectado | Plan |
|---|---|---|---|
| `SaleService.saveProducts` y `giveBackStock` | Usan `instanceof Product` para decidir qué filas guardar en `ProductDao` tras descontar o devolver stock | OCP, Polymorphism | El descuento de stock **sí** es polimórfico (`Sellable.deductStock`); solo la persistencia del producto pregunta el tipo. Un `Combo` nuevo no se guardaría sin tocar el servicio |
| `LowRotationService.unitsSoldSince`, `PromotionService` | Usan `instanceof Product` para contar solo ventas de productos | OCP | Aceptable: la baja rotación aplica solo a productos (RF-29) |
| `SaleLineDaoText.columnOf` | `instanceof Product` y `instanceof Treatment` para elegir la columna `product_id` o `treatment_id` | OCP | Refleja el `CHECK` de `sale_line` (una línea es de producto **o** de servicio); está en la capa de datos, no en el negocio |
| Ciclo `service` ⇄ `service.rule` | `SettingsService` vive en `service` y lo usan las reglas | Low Coupling | Ver [`diagramas.md`](diagramas.md#1-diagrama-de-paquetes); se resuelve en la Entrega 2 |
| `AppointmentService.book` con 5 parámetros | Supera el máximo de 3 de Clean Code | Clean Code | Se mantiene por claridad del contrato (RF-10); los demás servicios usan DTO (`ContactData`, `ProductData`, `TreatmentData`, `WorkSchedule`) |

---

## 9. Guía de implementación para la Fase 3

Checklist por clase antes de abrir un Pull Request:

- [ ] Atributos `private`; acceso por getters/setters (setters solo donde el atributo puede cambiar).
- [ ] Constructor completo que deja el objeto en estado válido; colecciones inicializadas.
- [ ] `@Override` en `toString`, `equals`, `hashCode` y métodos de interfaces/abstractos.
- [ ] Las listas se exponen como copias o vistas inmodificables (`List.copyOf`, `Collections.unmodifiableList`).
- [ ] Validación de entrada en los `*Servicio` con `Validador`; no en el modelo ni solo en la UI.
- [ ] Excepciones propias según [ADR-0008](../adr/0008-manejo-de-excepciones.md); sin `catch` vacíos.
- [ ] Javadoc en la clase y en métodos públicos; `PascalCase` en clases, `camelCase` en métodos y atributos.
- [ ] Prueba JUnit (o `Main` de consola) del comportamiento principal.

### Reparto de las clases de esta fase

| Responsable | Clases |
|---|---|
| **Samuel** | `CrudDao`, `TextFileDao`, excepciones, `Validador`, `Cita`, `EstadoCita`, `CitaDao(Texto)`, `CitaServicio`, interfaz `ReglaCita`, `Venta`, `DetalleVenta`, `VentaDao(Texto)`, `VentaServicio`; **paquete `ia`** (opcional en la Entrega 1, sin interfaz): `AsistenteInteligente`, `HerramientaAsistente`, `RegistroHerramientas` y las herramientas, `ProbadorVirtual` y sus versiones simuladas |
| **Nicoll** | `Persona`, `Cliente`, `Profesional`, sus DAO de archivo de texto y servicios, `TratamientoServicio`, `EstadisticaServicio`, `NotificadorAdministrador`, pruebas de integración y `Main` |
| **Isabella** | `Vendible`, `Producto`, `CategoriaProducto`, `Tratamiento`, sus DAO de texto, `ProductoServicio`, `Configuracion`/`ConfiguracionServicio` (tabla `app_setting`), `ReglaAnticipo`, `ReglaConfirmacion`, `ReglaRecordatorio`, `NotificadorCorreo(Simulado)`, mockups M1–M9 |

> Esta distribución sigue [`../roadmap.md`](../roadmap.md). Ojo: la entidad `Tratamiento` es de Isabella pero su lógica (`TratamientoServicio`) es de Nicoll; coordinen la firma de `Tratamiento` desde el día 1. `ReglaCita` la define Samuel (contrato) y sus implementaciones, que leen `ConfiguracionServicio`, las escribe Isabella.
>
> **Coherencia con `schema.sql`:** `Producto` referencia una `CategoriaProducto` (tabla `product_category`); `Cliente` guarda `fechaRegistro` e `inasistencias`; `Cita` guarda `anticipo` y `recordatorio`; `Venta` puede enlazar su `Cita`. Los parámetros del negocio (porcentaje de anticipo, aviso mínimo, ventana de baja rotación) se leen de `ConfiguracionServicio`, no se escriben en el código.
