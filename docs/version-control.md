# Control de versiones · Maison Glow

> Evidencia de uso de Git para la **Entrega 1**: ramas, Pull Requests y commits por integrante.
> Datos tomados de `git log` sobre `develop` el **2026-10-28**. La estrategia está en el [ADR-0004](adr/0004-estrategia-git.md) y el flujo diario en el [roadmap](roadmap.md#5-flujo-de-git-de-cada-integrante).

## 1. Resumen

| Indicador | Valor | Mínimo exigido |
|---|:--:|:--:|
| Commits propios (sin merges) en `develop` | 151 | — |
| Pull Requests fusionados | 25 | 6 |
| Commits por integrante | 69 · 43 · 39 | 12 cada uno |
| Primer y último commit en `develop` | 2026-10-08 → 2026-10-28 | — |
| Commits directos a `main` o `develop` | 0 desde los 2 commits iniciales (todo entra por PR) | 0 |

Los tres integrantes superan el mínimo de 12 commits. `main` aún no recibe la Entrega 1: el PR `develop` → `main` con el tag `v0.1-entrega1` se hace al cierre (ver sección 6).

## 2. Commits por integrante

Commits propios, sin contar los merges, en `develop`.

| Integrante | Rol | Cuenta de Git | Commits | PR abiertos y fusionados |
|---|---|---|:--:|:--:|
| Samuel David Rangel Martínez | Líder técnico | `samueldrangel@unicesar.edu.co` | 69 | 10 |
| Nicoll Gómez (`Nicoll Jhoana Gómez Siosi` en Git) | Dev 1 | `njohanagomez@gmail.com` | 43 | 9 |
| Isabella Celedón (`Isabel Sofía Celedon Martinez` en Git) | Dev 2 | `iceledon` (correo de GitHub) | 39 | 6 |

Notas sobre los datos:

- Los 69 commits de Samuel salen de dos nombres de autor (`Samuel David Rangel Martínez` y `samueldrangel`) con el **mismo correo**; es la misma persona.
- Los PR se asignan al autor de los commits de la rama. Todos los merges aparecen a nombre de `samueldrangel`, porque el líder fusiona en la web de GitHub.
- Hay 4 commits `deska checkpoint` que **no están en `develop`** (son puntos de control de una herramienta local, sin código) y no se cuentan.
- Los 39 merges de `develop` (25 de PR más 14 de sincronización con `develop`) tampoco se cuentan como aporte de nadie.

## 3. Pull Requests fusionados

Orden de fusión. «Commits» son los propios de la rama.

| PR | Rama | Autor | Commits | Fusión | Tarea |
|:--:|---|---|:--:|:--:|---|
| #1 | `feature/project-skeleton` | Samuel | 13 | 2026-10-08 | S-01 |
| #2 | `docs/roadmap` | Samuel | 2 | 2026-10-08 | Roadmap |
| #3 | `docs/database-design` | Samuel | 11 | 2026-10-09 | S-02 |
| #4 | `feature/beauty-service-model` | Isabella | 3 | 2026-10-14 | I-01 |
| #7 | `feature/person-model` | Nicoll | 6 | 2026-10-14 | N-01 |
| #9 | `refactor/rename-beauty-service` | Samuel | 4 | 2026-10-14 | ADR-0013 (`Treatment`) |
| #10 | `feature/text-file-dao` | Samuel | 9 | 2026-10-14 | S-03 |
| #11 | `feature/appointment-model` | Samuel | 7 | 2026-10-14 | S-04 |
| #5 | `feature/product-model` | Isabella | 9 | 2026-10-15 | I-02 |
| #6 | `feature/settings-service` | Isabella | 10 | 2026-10-15 | I-10 |
| #12 | `feature/appointment-service` | Samuel | 7 | 2026-10-15 | S-05 |
| #13 | `feature/sale-model` | Samuel | 9 | 2026-10-16 | S-06 |
| #14 | `feature/product-service` | Isabella | 7 | 2026-10-18 | I-03 |
| #15 | `feature/appointment-rules` | Isabella | 5 | 2026-10-18 | I-11 |
| #16 | `feature/email-notifier` | Isabella | 5 | 2026-10-20 | I-07 |
| #17 | `feature/customer-service` | Nicoll | 7 | 2026-10-20 | N-02 |
| #18 | `feature/treatment-catalog` | Nicoll | 7 | 2026-10-20 | N-03 |
| #19 | `feature/statistics-service` | Nicoll | 4 | 2026-10-20 | N-05 |
| #20 | `docs/roadmap-fix` | Samuel | 1 | 2026-10-20 | Roadmap |
| #21 | `feature/sale-service` | Samuel | 4 | 2026-10-20 | S-07 |
| #22 | `feature/professional-service` | Nicoll | 7 | 2026-10-23 | N-04 |
| #23 | `feature/low-rotation` | Nicoll | 4 | 2026-10-23 | N-06 |
| #24 | `feature/promotion-suggestion` | Nicoll | 4 | 2026-10-23 | N-07 |
| #25 | `feature/integration-tests` | Nicoll | 2 | 2026-10-28 | N-10 |
| #26 | `feature/console-demo` | Nicoll | 2 | 2026-10-28 | N-11 |

- El PR #8 no figura entre los fusionados. El historial de Git no distingue un PR cerrado de uno abierto sin fusionar; se confirma en la pestaña *Pull requests* de GitHub.
- Los PR #5 y #6 se fusionaron después de #7 y #9 aunque tienen número menor: sus ramas se abrieron antes y se sincronizaron con `develop` primero.
- El historial de Git **no guarda quién aprobó** cada PR. Las aprobaciones están en GitHub (*Pull requests → Reviews*) y se adjuntan como captura a la entrega.

## 4. Ramas

| Tipo | Ramas | Estado |
|---|---|---|
| Permanentes | `main` (estable, protegida) y `develop` (integración) | `main` conserva el commit inicial; recibe la Entrega 1 al cierre |
| Funcionalidad | 21 `feature/*` (una por tarea) | Todas fusionadas en `develop` |
| Documentación y refactor | `docs/roadmap`, `docs/roadmap-fix`, `docs/database-design`, `refactor/rename-beauty-service` | Fusionadas en `develop` |

Cada rama se crea desde `develop`, lleva una sola tarea del [roadmap](roadmap.md) y se fusiona con *merge* normal (sin *squash*) para conservar los commits individuales. Las ramas locales que quedan (`feature/sale-model`, `feature/sale-service`, etc.) ya están fusionadas y se pueden borrar.

## 5. Convenciones

| Regla | Detalle |
|---|---|
| Formato de commit | *Conventional Commits*: `feat`, `fix`, `test`, `docs`, `refactor`, `chore`, con alcance: `feat(customer): add duplicate document check` |
| Idioma | Commits y ramas en inglés; documentos en español |
| Un PR = una tarea | Título en formato *Conventional Commits* |
| Sin `push --force` | En ramas compartidas; el `rebase` solo se hace en la rama propia |
| Revisión | Samuel revisa los PR de Nicoll e Isabella; Nicoll o Isabella revisan los de Samuel. Ningún PR se fusiona sin una aprobación ajena al autor |
| Excepción | Las tareas reasignadas a Samuel el 2026-10-28 (I-04, I-05, I-06, N-09, I-08) y sus PR de cierre de la Entrega 1 los revisa y fusiona Samuel (regla 3 del [roadmap](roadmap.md#5-flujo-de-git-de-cada-integrante)) |

## 6. Cierre de la Entrega 1

1. Fusionar en `develop` los PR de documentación del cierre: roadmap y mockups, diagramas finales, GRASP y SOLID, y Fase 1 con `TEAM.md` y este documento.
2. Ejecutar `mvn clean verify` sobre `develop` (debe pasar con ≥ 70 % de cobertura en `service`).
3. Abrir el PR `develop` → `main`, fusionarlo y crear el tag `v0.1-entrega1`.
4. Actualizar las cifras de la sección 1 con los comandos de abajo.

## 7. Cómo regenerar los datos

```bash
# Commits propios por correo (sin merges)
git log develop --no-merges --format='%ae' | sort | uniq -c

# Resumen por nombre
git shortlog -sne --no-merges develop

# Pull Requests fusionados, en orden
git log develop --merges --reverse --format='%h %ad %s' --date=short --grep='^Merge pull request'

# Commits propios y autor de un PR (reemplazar <merge> por el hash del merge)
git log --no-merges --format='%an' <merge>^1..<merge>^2 | sort | uniq -c

# Historial gráfico (para la captura del docente)
git log --graph --oneline --all
```
