# ADR-0010: DAO en memoria primero, JDBC después

- **Estado:** Reemplazado por [ADR-0012](0012-persistencia-en-archivos-de-texto.md)
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel, con el equipo

## Contexto
La primera entrega (≤ 2 semanas) pide toda la lógica de negocio y avances de UI (mockups), pero **no** base de datos. Aun así conviene diseñar la BD desde el inicio para que el backend sea coherente con ella.

## Decisión
1. **Diseñar la BD en los primeros días:** MER y `sql/schema.sql` revisados por los tres. El modelo (`modelo`) se escribe respetando ese diseño (mismos identificadores, relaciones y restricciones).
2. **Programar los servicios contra `CrudDao<T>`** y entregar, en la primera entrega, implementaciones **en memoria** (`XxxDaoMemoria`, con `HashMap<Integer, T>` y un contador de ids).
3. En la entrega 2, escribir `XxxDaoJdbc` (ver [ADR-0003](0003-persistencia-sqlite-jdbc-dao.md)) y cambiar la implementación inyectada en un único punto (clase de arranque). Los servicios no se modifican.
4. Los DAO en memoria se conservan para las **pruebas unitarias** de los servicios.

## Alternativas consideradas
- **Esperar a la BD para empezar la lógica:** bloquea al equipo y arriesga la entrega.
- **Conectar SQLite desde el primer día:** añade carga (conexión, SQL, excepciones) que compite con la lógica de negocio en un plazo corto.

## Consecuencias
- (+) Entrega 1 viable y demostrable sin BD.
- (+) Ejemplo claro de DIP y OCP: se cambia la persistencia sin tocar el negocio.
- (+) Las pruebas de servicios son rápidas y no dependen de un archivo.
- (−) Hay dos implementaciones de cada DAO; se mitiga porque la de memoria es de pocas líneas.
- (−) Riesgo de que el modelo diverja del esquema: por eso el diseño de la BD se fija primero.
