# ADR-0004: Estrategia de ramas y commits en Git

- **Estado:** Aceptado
- **Fecha:** 2026-10-08
- **Decide:** Samuel Rangel (líder)

## Contexto
La asignatura evalúa evidencia de commits por integrante, ramas y fusiones. El enunciado del proyecto ya prevé ramas `feature/*` integradas a `develop` mediante Pull Requests.

## Decisión
- `main`: estable, protegida, solo recibe merges desde `develop` al cierre de fase (con tag `fase1`…`fase4`, `v1.0-mvp`).
- `develop`: integración.
- `feature/<modulo>`: una por funcionalidad, creada desde `develop`, fusionada por Pull Request con al menos una aprobación del líder.
- `docs/<tema>` para documentación.
- Commits en formato *Conventional Commits* en español (`feat(citas): ...`).
- Cada dev es dueño de módulos distintos y de **sus propias ventanas** para evitar conflictos en los `.form` de NetBeans.
- `.gitignore` obligatorio: `target/`, `nbproject/private/`, `*.db`, archivos con claves.

## Alternativas consideradas
- **Trunk-based (todo a main):** peligroso para estudiantes sin pruebas automáticas.
- **Una rama por integrante:** pierde trazabilidad por funcionalidad y genera ramas de larga vida con merges grandes. (El requisito permite "por integrante **o** funcionalidad".)

## Consecuencias
- (+) Historial legible y evidencia clara para el reporte final.
- (+) El líder controla la calidad en cada merge.
- (−) Requiere disciplina de PR; el líder puede convertirse en cuello de botella (mitigación: revisar en ≤ 48 h).
