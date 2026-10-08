# ADR-0003: Persistencia con SQLite, JDBC y patrón DAO

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel, validado por el equipo

## Contexto
Se requiere base de datos relacional, CRUD desde la capa de acceso a datos, patrón DAO y manejo de excepciones (RNF-07, RNF-08, RNF-12). La aplicación es local y para un único establecimiento (RNF-11).

## Decisión
> **Calendario:** el esquema se diseña desde el inicio, pero la conexión JDBC se implementa en la entrega 2; antes se usan DAO en memoria ([ADR-0010](0010-dao-en-memoria-primero.md)).

- **Motor:** SQLite (archivo `maisonglow.db`), accedido con **JDBC puro** (`PreparedStatement` siempre, para evitar inyección SQL).
- **Patrón DAO:** interfaz genérica `CrudDao<T>` (`crear`, `buscarPorId`, `listar`, `actualizar`, `eliminar`) y una clase `XxxDaoJdbc` por entidad.
- **Esquema** en `sql/schema.sql`, ejecutado automáticamente si la BD no existe.
- **Mapeo objeto→tabla manual** (método privado `mapear(ResultSet)` en cada DAO); sin ORM.
- Una única clase `ConexionBD` entrega la conexión.
- Las `SQLException` se envuelven en `AccesoDatosException` (ver ADR-0008).

## Alternativas consideradas
- **MySQL/MariaDB:** más realista, pero exige instalar y configurar un servidor en cada equipo; riesgo alto de "en mi máquina sí funciona".
- **JPA/Hibernate:** oculta el mapeo y el DAO, que son parte de lo que se debe demostrar.
- **Archivos planos/serialización:** no cumple "relacional".

## Consecuencias
- (+) Cero instalación; la BD viaja con el proyecto (ignorada en Git, regenerable con el script).
- (+) SQL estándar: migrar a MySQL solo requiere cambiar `ConexionBD` y el driver.
- (−) SQLite tiene tipos flexibles: la validación debe hacerse en la capa de servicio.
- (−) Escritura concurrente limitada; irrelevante para una app de escritorio de un solo usuario.
