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

## 2. Estrategia de Branching: GitHub Flow

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
│  - Tests unitarios/integración  │    │  - Scripts de operaciones       │
│  - Dockerfiles de cada servicio │    │                                 │
│                                 │    │  Branch principal: master       │
│  Branch principal: master       │    └─────────────────────────────────┘
└─────────────────────────────────┘               │
           │                                      │
           └──────────────┬───────────────────────┘
                          │
             Jenkins observa ambos repos
             y dispara los pipelines correspondientes
```

**Rol de este repositorio en el flujo GitOps:**

Cada merge a `master` produce una nueva imagen Docker tagueada que el ops-repo toma para desplegar en el cluster. Este repo **nunca modifica manifiestos de Kubernetes ni configuración de Terraform**.

### 2.2 GitHub Flow

Se adopta **GitHub Flow**, elegido por su simplicidad para un equipo de 2 personas con sprints de 1 semana. Una única rama permanente (`master`) siempre estable y desplegable; todo cambio llega mediante Pull Request desde una rama de corta duración.

#### Rama permanente

| Rama | Propósito |
|---|---|
| `master` | Única rama permanente. Siempre estable y desplegable. Todo cambio entra vía PR. Cada merge dispara el pipeline de CI/CD. |

#### Ramas de corta duración

| Prefijo | Origen | Destino | Cuándo |
|---|---|---|---|
| `feat/SCRUM-XX-descripcion` | `master` | `master` (vía PR) | Nueva funcionalidad o historia de usuario |
| `fix/SCRUM-XX-descripcion` | `master` | `master` (vía PR) | Corrección de bug |
| `refactor/SCRUM-XX-descripcion` | `master` | `master` (vía PR) | Refactorización sin cambio de comportamiento |
| `docs/SCRUM-XX-descripcion` | `master` | `master` (vía PR) | Documentación |
| `test/SCRUM-XX-descripcion` | `master` | `master` (vía PR) | Agregar o mejorar pruebas |
| `chore/SCRUM-XX-descripcion` | `master` | `master` (vía PR) | Mantenimiento, dependencias, config |

### 2.3 Estructura Visual de Ramas

```
master  ──────●──────────●──────────●──────────●──────────►
              ↑          ↑          ↑          ↑
           PR merge   PR merge   PR merge   PR merge
              │          │          │          │
          feat/        fix/      refactor/   feat/
          SCRUM-10    SCRUM-15   SCRUM-18   SCRUM-22

 ◄──────────────── Sprint 1 (1 semana) ─────────────────►
```

### 2.4 Flujo de Trabajo

```
1. Tomar la historia de Jira (estado "In Progress")

2. Crear rama desde master:
   git checkout master
   git pull origin master
   git checkout -b feat/SCRUM-42-auth-jwt-refresh

3. Desarrollar con commits atómicos siguiendo Conventional Commits

4. Abrir PR hacia master cuando la historia esté lista

5. CI pasa en verde → compañero revisa → Squash and Merge → rama eliminada

6. Al cierre del sprint: tagear master con la versión del sprint
   git tag -a v1.0.0 -m "Sprint 1 release"
   git push origin v1.0.0
```

### 2.5 Reglas de Protección de la Rama master

Configuradas en GitHub:

- [ ] Require pull request before merging — no se permite push directo
- [ ] Require status checks to pass — CI debe estar en verde
- [ ] Require at least 1 approving review — el compañero revisa antes del merge
- [ ] Dismiss stale reviews — nueva aprobación requerida si hay commits nuevos
- [ ] Do not allow bypass — ni los admins pueden saltarse las reglas

### 2.6 Ambientes y su relación con las ramas

Los tres ambientes **no son ramas**; son etapas del pipeline que se activan desde `master`:

| Evento en master | Ambiente | Cómo se activa |
|---|---|---|
| Merge a `master` | **dev** | Automático — Jenkins despliega en el namespace `dev` del AKS |
| Aprobación manual en Jenkins | **stage** | Manual — desde el pipeline de dev tras smoke tests |
| Aprobación manual + tag `vX.Y.Z` | **prod** | Manual — desde el pipeline de stage tras pruebas de integración |

```
master
  │
  └── merge ──► Pipeline DEV (automático)
                    │
                    └── Manual Approval ──► Pipeline STAGE
                                               │
                                               └── Manual Approval + tag ──► Pipeline PROD
```

### 2.7 Convención de Commits: Conventional Commits

Cada mensaje de commit sigue la estructura:

```
<tipo>(<servicio>): <descripción imperativa en minúsculas>

[cuerpo opcional — explica el POR QUÉ]

[footer opcional — ej: Closes SCRUM-42]
```

El `scope` hace referencia al servicio o módulo afectado:

```
feat(auth): add JWT refresh token rotation
fix(gateway): resolve null pointer on expired token
test(identity): add integration test for hash collision edge case
chore(deps): upgrade Testcontainers to 1.20.0
refactor(dashboard): upgrade fallback to circuit breaker with Resilience4j
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

Debe seguir Conventional Commits (es el mensaje que quedará en `master` tras el squash):

```
feat(auth): add JWT refresh token rotation
fix(gateway): resolve null pointer on expired token validation
refactor(dashboard): upgrade fallback to circuit breaker with Resilience4j
```

#### Cuerpo del PR

```markdown
## Qué hace este PR
<!-- Una o dos oraciones. -->

## Por qué
<!-- Contexto. Ej: Closes SCRUM-42 -->

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

Todos los PRs hacia `master` usan exclusivamente **Squash and Merge**.

Un PR = una historia de Jira = un commit limpio en `master`.

#### Eliminación de ramas tras el merge

Todas las ramas se eliminan automáticamente tras el merge.

Configuración en GitHub: `Settings → General → Pull Requests → Automatically delete head branches` ✓

### 2.9 Flujo Completo — Ejemplo de un Sprint

```
── INICIO SPRINT 1 ─────────────────────────────────────────────

1. Desarrollador A toma SCRUM-10:
   git checkout master && git pull origin master
   git checkout -b feat/SCRUM-10-auth-ldap-integration

2. Desarrollador B toma SCRUM-11:
   git checkout master && git pull origin master
   git checkout -b feat/SCRUM-11-identity-hash-vault

   (ambos trabajan en paralelo sobre su feature branch)

3. Desarrollador A termina → PR feat/SCRUM-10 → master
   CI verde → Desarrollador B revisa → Squash and Merge
   Rama feat/SCRUM-10 eliminada automáticamente
   Jenkins despliega automáticamente en namespace dev

4. Desarrollador B termina → PR feat/SCRUM-11 → master
   CI verde → Desarrollador A revisa → Squash and Merge
   Rama feat/SCRUM-11 eliminada automáticamente

── CIERRE SPRINT 1 ──────────────────────────────────────────────

5. Validación en dev → aprobación manual → deploy a stage
   Pruebas de integración en stage → aprobación manual → deploy a prod

6. Tagear master con la versión del sprint:
   git tag -a v1.0.0 -m "Sprint 1 release"
   git push origin v1.0.0

── INICIO SPRINT 2 ──────────────────────────────────────────────

7. Nuevas features se abren desde master (que ya tiene v1.0.0)
   Mismo flujo → cierra con v1.1.0
```

---

## 3. Change Management Process

### 3.1 Propósito

Establecer un flujo formal y trazable para que todo cambio en el sistema sea evaluado, autorizado y registrado antes de llegar a producción. Este proceso aplica tanto al código de la aplicación (este repo) como a la infraestructura (ops repo).

### 3.2 Tipos de Cambio

| Tipo | Categoría | Ejemplos | ¿Requiere PR? | ¿Requiere CI? |
|---|---|---|---|---|
| **Feature** | Nueva funcionalidad | `feat(auth): add JWT refresh` | Sí | Sí |
| **Fix** | Corrección de bug | `fix(gateway): null pointer on expired token` | Sí | Sí |
| **Hotfix** | Corrección urgente en producción | `fix(auth): patch token validation` | Sí (fast-track) | Sí |
| **Refactor** | Cambio estructural sin cambio funcional | `refactor(dashboard): extract service class` | Sí | Sí |
| **Chore** | Mantenimiento, dependencias, config | `chore(deps): upgrade Spring Boot` | Sí | Sí |
| **Docs** | Solo documentación | `docs: add change management guide` | Sí | No obligatorio |
| **Revert** | Reversión de un cambio anterior | `revert: undo commit abc1234` | Sí | Sí |

### 3.3 Flujo de Aprobación de Cambios

```
┌────────────┐     ┌──────────┐     ┌───────────┐     ┌───────────┐     ┌───────────┐
│ 1. Ticket  │────►│ 2. Rama  │────►│ 3. Pull   │────►│ 4.        │────►│ 5. Merge  │
│ Jira:      │     │ docs/    │     │ Request   │     │ Revisión  │     │ Squash    │
│ "InProgress"│     │ feat/    │     │ (título   │     │ + CI      │     │ + Jira    │
│            │     │ fix/     │     │ Convent.  │     │ pasa      │     │ "Done"    │
└────────────┘     │ chore/   │     │ Commits)  │     │           │     │           │
                   └──────────┘     └───────────┘     └───────────┘     └───────────┘
                                                              │
                                                         ¿Aprueba?
                                                         ┌───┴───┐
                                                         │ Sí    │ No → se itera
                                                         └───┬───┘
                                                             ▼
                                                     ┌───────────────┐
                                                     │ Pipeline CI   │
                                                     │ pasa en verde │
                                                     └───────┬───────┘
                                                             ▼
                                                     ┌───────────────┐
                                                     │ Merge +       │
                                                     │ Deploy a dev  │
                                                     └───────────────┘
```

**Reglas del flujo:**

1. **Ticket en Jira**: Todo cambio debe tener un ticket Jira en estado `In Progress` antes de escribir código.
2. **Rama desde master**: `git checkout -b <tipo>/SCRUM-XX-descripcion`
3. **Pull Request**: Título sigue Conventional Commits. Cuerpo incluye descripción, motivación y checklist.
4. **Revisión obligatoria**: Mínimo 1 approving review del compañero. No self-approval.
5. **CI debe pasar**: Tests unitarios, SonarQube quality gate, Trivy security scan. Si falla, no se mergea.
6. **Squash & Merge**: Un solo commit limpio en master con referencia al ticket Jira.
7. **Jira a Done**: Al mergear, la historia pasa a `Done` con comentario de evidencia.

### 3.4 Promoción por Ambientes

| Etapa | Gatillo | Verificaciones |
|---|---|---|
| **DEV** | Automático al mergear a `master` | Smoke tests (health check) |
| **STAGE** | Aprobación manual tras dev | Smoke tests + integración |
| **PROD** | Aprobación manual + tag SemVer | Smoke tests + release notes |

### 3.5 Trazabilidad

Cada cambio debe ser rastreable desde el requerimiento hasta el deploy:

```
Ticket Jira ──► Rama ──► Commit ──► PR ──► Merge ──► Tag ──► Release Notes
SCRUM-42        feat/     feat:      feat:    v1.0.0    v1.0.0
                SCRUM-42  add JWT    add JWT
```

### 3.6 Cambios de Emergencia (Hotfix)

Para bugs críticos en producción que no pueden esperar el ciclo normal:

1. Crear rama `fix/SCRUM-XX-descripcion` desde master
2. PR con revisión exprés (1 approve, priorizada)
3. CI pasa → merge → deploy automático a dev
4. Approval manual acelerado a stage y prod
5. Ticket de Jira se actualiza post-facto si es necesario
6. Se documenta la causa raíz y la acción preventiva en un plazo de 24h

---

## Resumen

| Dimensión | Decisión | Justificación |
|---|---|---|
| Metodología ágil | Scrum (1 semana/sprint) | Ciclos cortos con revisión al final de cada sprint |
| Herramienta de gestión | Jira Software | Historias, criterios de aceptación y evidencia viven en Jira |
| Estrategia de branching | GitHub Flow | Una sola rama permanente; sin overhead de develop/release para equipo de 2 |
| Modelo GitOps | Repos separados (app + ops) | Este repo produce imágenes Docker; el ops-repo las despliega en AKS |
| Merge strategy | Squash and Merge (siempre) | master limpio: un commit por historia de Jira |
| Ambientes | Pipeline desde master: dev (auto) → stage (manual) → prod (manual + tag) | Los ambientes son etapas del pipeline, no ramas |
| Convención de commits | Conventional Commits con scope de servicio | Trazabilidad tipo → SemVer, historial legible por servicio |
| Versionado | SemVer; tag en master al cierre de cada sprint | v1.0.0 = Sprint 1, v1.1.0 = Sprint 2 |
