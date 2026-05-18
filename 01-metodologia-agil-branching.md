# 1. Metodología Ágil y Estrategia de Branching

## 1. Metodología Ágil: Scrum

### 1.1 Marco de Trabajo

El proyecto CircleGuard se desarrolla con **Scrum adaptado para equipos pequeños** (2 integrantes). Scrum fue elegido sobre Kanban porque el alcance del proyecto está acotado en el tiempo y se beneficia de ciclos de entrega fijos que permiten revisar y re-priorizar entre cada iteración.


### 1.2 Herramienta: Jira

Se usa **Jira Software** con un tablero **Scrum** para gestionar el backlog, los sprints, las historias de usuario y los criterios de aceptación. Toda la gestión de historias, tareas y evidencia de avance vive en Jira.


### 1.4 Ceremonias (adaptadas a equipo de 2)

| Ceremonia | Frecuencia | Duración | Artefacto |
|---|---|---|---|
| Sprint Planning | Inicio de cada sprint | 1 hora | Sprint backlog en Jira |
| Daily Standup | Diario (async vía comentarios Jira) | 15 min | Actualización de tarjetas |
| Sprint Review | Final de cada sprint | 30 min | Demo del incremento |
| Sprint Retrospective | Final de cada sprint | 30 min | Action items en Jira |

### 1.5 Definition of Done (DoD)

Un ítem del backlog se considera **Done** cuando:

- [ ] El código fue revisado mediante Pull Request (mínimo 1 aprobación)
- [ ] Los tests unitarios del servicio pasan en el pipeline de CI (`./gradlew test`)
- [ ] La cobertura de código no decrece respecto al sprint anterior
- [ ] SonarQube no reporta nuevos issues de nivel `BLOCKER`
- [ ] La imagen Docker del servicio fue construida y publicada en el registry
- [ ] La historia de Jira está en estado `Done` con comentario de evidencia

---

## 2. Estrategia de Branching: GitFlow Simplificado

### 2.1 Arquitectura GitOps — Repositorios Separados

Se adopta el patrón **GitOps con repositorios separados**, manteniendo una separación clara entre el código de la aplicación (este repo) y la configuración de infraestructura/despliegue (ops repo).

```
┌─────────────────────────────────┐    ┌─────────────────────────────────┐
│   Repo: circle-guard-public     │    │   Repo: finalproject-ingesoftv  │
│   (Código de la Aplicación)     │    │   -ops  (Infraestructura/Ops)   │
│   ← ESTE REPOSITORIO            │    │                                 │
│                                 │    │  - Pipelines Jenkins            │
│  - Microservicios Spring Boot   │    │  - Manifiestos Kubernetes       │
│  - Frontend Expo/React Native   │    │  - Terraform (IaC)              │
│  - Tests unitarios/integración  │    │  - docker-compose.*             │
│  - Dockerfiles de cada servicio │    │  - Scripts de operaciones       │
│                                 │    │                                 │
│  Branch principal: main         │    │  Branch principal: master       │
└─────────────────────────────────┘    └─────────────────────────────────┘
           │                                         │
           └─────────────┬───────────────────────────┘
                         │
              Jenkins observa ambos repos
              y dispara los pipelines correspondientes
```

**Rol de este repositorio en el flujo GitOps:**

Cada merge a `main` produce una nueva imagen Docker tagueada que el ops-repo toma para desplegar en el cluster. Este repo **nunca modifica manifiestos de Kubernetes ni configuración de Terraform**.

### 2.2 GitFlow Simplificado

Se adopta **GitFlow simplificado**, adaptado a un equipo de 2 personas con sprints de 1 semana. La simplificación elimina las ramas `hotfix/*` y `support/*` de GitFlow clásico dado el tamaño del equipo, conservando las ramas que aportan valor real al flujo Scrum.

#### Ramas permanentes

| Rama | Propósito |
|---|---|
| `main` | Código en producción. Siempre estable. Solo recibe merges desde `release/sprint-N`. Cada merge genera un tag de versión. |
| `develop` | Rama de integración. Refleja el estado actual del sprint. Las features se integran aquí durante el sprint. |

#### Ramas de corta duración

| Rama | Origen | Destino | Cuándo |
|---|---|---|---|
| `feature/INGESOFTV-XX-descripcion` | `develop` | `develop` (vía PR) | Durante el sprint, para cada historia de usuario |
| `release/sprint-N` | `develop` | `main` + `develop` | Al final de cada sprint, para preparar el release |
| `hotfix/INGESOFTV-XX-descripcion` | `main` | `main` + `develop` | Solo para bugs críticos en producción que no pueden esperar al siguiente sprint |

### 2.3 Estructura Visual de Ramas

```
main         ─────────────────────────────●─────────────────●──────────
                                         ↑ v1.0.0           ↑ v1.1.0
                                    release/sprint-1    release/sprint-2
                                         ↑                   ↑
develop      ──────●────●────●───────────●────●────●─────────●──────────
                   ↑    ↑    ↑                ↑    ↑
              feat/      feat/            feat/    feat/
              JIRA-10    JIRA-15          JIRA-22  JIRA-27

 ◄─────── Sprint 1 (1 semana) ──────────► ◄──── Sprint 2 (1 semana) ────►
```

### 2.4 Flujo por Sprint

#### Durante el sprint (features)

```
1. Tomar la historia de Jira (estado "In Progress")

2. Crear feature branch desde develop:
   git checkout develop
   git pull origin develop
   git checkout -b feature/INGESOFTV-42-auth-jwt-refresh

3. Desarrollar con commits atómicos (Conventional Commits)

4. Abrir PR hacia develop cuando la historia esté lista

5. CI pasa en verde → compañero revisa → Squash and Merge → rama eliminada
```

#### Al cierre del sprint (release)

```
1. Crear rama de release desde develop:
   git checkout develop
   git pull origin develop
   git checkout -b release/sprint-1

2. Solo correcciones menores sobre esta rama (no features nuevas)
   - Ajustes de versión en build files
   - Fixes de última hora

3. Abrir PR de release/sprint-1 hacia main
   CI pasa → revisión → Merge commit (NO squash, para preservar historial del sprint)

4. Tagear en main:
   git tag -a v1.0.0 -m "Sprint 1 release"
   git push origin v1.0.0

5. Merge de release/sprint-1 de vuelta a develop:
   (para que develop tenga los fixes aplicados en la release branch)
   git checkout develop
   git merge release/sprint-1
   git push origin develop

6. Eliminar rama release/sprint-1
```

#### Hotfix (solo para bugs críticos en producción)

```
1. git checkout main && git checkout -b hotfix/INGESOFTV-XX-descripcion

2. Fix + commit

3. PR hacia main → Merge → tag patch (v1.0.1)

4. Merge del hotfix también hacia develop:
   git checkout develop && git merge hotfix/INGESOFTV-XX-descripcion
```

### 2.5 Reglas de Protección de Ramas

Configuradas en GitHub:

**Rama `main`:**
- [ ] Solo acepta merges desde `release/sprint-N` o `hotfix/*`
- [ ] Require pull request before merging
- [ ] Require status checks to pass (CI verde)
- [ ] Require at least 1 approving review
- [ ] Do not allow direct pushes — ni los admins

**Rama `develop`:**
- [ ] Solo acepta merges desde `feature/*` o `release/*`
- [ ] Require pull request before merging
- [ ] Require status checks to pass (CI verde)
- [ ] Require at least 1 approving review

### 2.6 Ambientes y su relación con las ramas

| Rama | Ambiente que alimenta | Cómo |
|---|---|---|
| `develop` | dev | Cada merge a develop dispara el pipeline de dev en el ops-repo |
| `release/sprint-N` | stage | La rama de release dispara el pipeline de stage para validación previa al merge |
| `main` | prod | Cada merge a main (con tag) dispara el pipeline de producción en el ops-repo |

### 2.7 Convención de Commits: Conventional Commits

Cada mensaje de commit sigue la estructura:

```
<tipo>(<servicio>): <descripción imperativa en minúsculas>

[cuerpo opcional — explica el POR QUÉ]

[footer opcional — ej: Closes INGESOFTV-42]
```

El `scope` hace referencia al servicio o módulo afectado:

```
feat(auth): add JWT refresh token rotation
fix(gateway): resolve null pointer on expired token
test(identity): add integration test for hash collision edge case
chore(deps): upgrade Testcontainers to 1.20.0
refactor(promotion): extract graph traversal to dedicated query service
feat(mobile): add QR scanner screen to campus entry flow
```

#### Tipos permitidos

| Tipo | Cuándo usarlo |
|---|---|
| `feat` | Nueva funcionalidad |
| `fix` | Corrección de bug |
| `perf` | Mejora de rendimiento sin cambio funcional |
| `chore` | Mantenimiento, dependencias, config |
| `docs` | Solo documentación |
| `test` | Agregar o corregir pruebas |
| `refactor` | Reestructuración sin cambio de comportamiento |
| `ci` | Cambios en Dockerfiles o scripts de build local |
| `revert` | Revertir un commit anterior |

#### Reglas de redacción

- **Imperativo presente**: "add", "fix", "remove" — no "added", "fixes", "removing"
- **Minúsculas** en el tipo, scope y descripción
- **Sin punto final** en la primera línea
- **Máximo 72 caracteres** en la primera línea
- El cuerpo explica el *por qué*, no el *qué*
- Cambios que rompen el contrato de API: `feat!: rename /api/v1/status to /api/v2/status`

#### Relación con SemVer

| Tipos en commits desde el último tag | Bump de versión |
|---|---|
| Al menos un `feat!` o `BREAKING CHANGE` en footer | `MAJOR` (v1.0.0 → v2.0.0) |
| Al menos un `feat` | `MINOR` (v1.0.0 → v1.1.0) |
| Solo `fix`, `perf`, `chore`, `test`, etc. | `PATCH` (v1.0.0 → v1.0.1) |

### 2.8 Proceso de Pull Request

#### Título del PR

Debe seguir Conventional Commits (es el mensaje que quedará en `develop` tras el squash):

```
feat(auth): add JWT refresh token rotation
fix(gateway): resolve null pointer on expired token validation
test(identity): add integration tests for anonymization pipeline
```

#### Cuerpo del PR

```markdown
## Qué hace este PR
<!-- Una o dos oraciones. -->

## Por qué
<!-- Contexto. Ej: Closes INGESOFTV-42 -->

## Cómo probarlo
<!-- Pasos para verificar el cambio. -->

## Checklist
- [ ] Tests pasan localmente (`./gradlew :services:<nombre>:test`)
- [ ] SonarQube sin nuevos issues BLOCKER
- [ ] Cambio cubierto por tests
- [ ] Dockerfile sigue construyendo correctamente
- [ ] OpenAPI/README actualizado si aplica
```

#### Reglas de revisión

- Mínimo **1 aprobación** del compañero antes del merge
- El autor **no puede aprobarse a sí mismo**
- Si se agregan commits nuevos después de la aprobación, la aprobación se invalida (**Dismiss stale reviews**)
- Los comentarios **"Request changes"** deben resolverse antes del merge

#### Estrategia de merge

| Tipo de PR | Estrategia | Por qué |
|---|---|---|
| `feature/*` → `develop` | **Squash and Merge** | Trunk de develop limpio; un commit por historia de Jira |
| `release/sprint-N` → `main` | **Merge commit** | Preserva el historial completo del sprint en main |
| `release/sprint-N` → `develop` | **Merge commit** | Necesario para traer los fixes de release de vuelta |
| `hotfix/*` → `main` | **Merge commit** | Trazabilidad del fix crítico |
| `hotfix/*` → `develop` | **Merge commit** | Sincronizar el fix en develop |

#### Eliminación de ramas tras el merge

- Ramas `feature/*`: eliminadas automáticamente tras el merge.
- Ramas `release/*` y `hotfix/*`: eliminadas manualmente después de confirmar que el merge a `main` y `develop` fue exitoso.

Configuración en GitHub para auto-delete de features: `Settings → General → Pull Requests → Automatically delete head branches` ✓

### 2.9 Flujo Completo — Ejemplo de un Sprint

```
── INICIO SPRINT 1 ─────────────────────────────────────────────

1. Desarrollador A:
   git checkout -b feature/INGESOFTV-10-auth-ldap-integration

2. Desarrollador B:
   git checkout -b feature/INGESOFTV-11-identity-hash-vault

   (ambos trabajan en paralelo sobre su feature branch)

3. Desarrollador A termina → PR feature/INGESOFTV-10 → develop
   CI verde → Desarrollador B revisa → Squash and Merge
   Rama feature/INGESOFTV-10 eliminada automáticamente

4. Desarrollador B termina → PR feature/INGESOFTV-11 → develop
   CI verde → Desarrollador A revisa → Squash and Merge
   Rama feature/INGESOFTV-11 eliminada automáticamente

── CIERRE SPRINT 1 ──────────────────────────────────────────────

5. git checkout -b release/sprint-1 (desde develop)
   Ajustes menores de versión si aplica

6. PR release/sprint-1 → main
   CI verde → revisión → Merge commit

7. git tag -a v1.0.0 -m "Sprint 1 release" && git push origin v1.0.0

8. Merge release/sprint-1 → develop (para sincronizar fixes)

9. Eliminar rama release/sprint-1

── INICIO SPRINT 2 ──────────────────────────────────────────────

10. Nuevas features se abren desde develop (que ya tiene v1.0.0)
    Mismo flujo que Sprint 1 → cierra con v1.1.0
```

---

## Resumen

| Dimensión | Decisión | Justificación |
|---|---|---|
| Metodología ágil | Scrum (1 semana/sprint, 2 sprints) | Ciclos cortos con revisión al final de cada sprint |
| Herramienta de gestión | Jira Software | Historias, criterios de aceptación y evidencia viven en Jira |
| Estrategia de branching | GitFlow simplificado | Separa features / integración / releases; se adapta al ritmo de sprint |
| Modelo GitOps | Repos separados (app + ops) | Este repo produce imágenes Docker; el ops-repo las despliega |
| Merge features → develop | Squash and Merge | Develop limpio: un commit por historia de Jira |
| Merge release → main | Merge commit | Preserva el historial completo del sprint |
| Ambientes | develop → dev / release → stage / main → prod | Cada rama alimenta un ambiente distinto vía el ops-repo |
| Convención de commits | Conventional Commits con scope de servicio | Trazabilidad tipo → SemVer, historial legible por servicio |
| Versionado | SemVer; tag en cada merge a main | v1.0.0 = Sprint 1, v1.1.0 = Sprint 2 |
