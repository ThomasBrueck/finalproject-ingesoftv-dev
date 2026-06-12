# 🛡️ CircleGuard — Repositorio de Aplicación (Dev)

**Absolute Privacy. High-Speed Containment. Secure Campus.**

CircleGuard es un sistema universitario de rastreo de contactos y "fencing" de salud que identifica grupos de contacto interconectados ("Círculos") y aplica cuarentenas rápidas preservando el anonimato individual.

Este repositorio (**dev**) contiene el **código fuente de la aplicación**: los 8 microservicios Spring Boot, el frontend móvil/web, las pruebas (unitarias, integración, E2E, rendimiento y seguridad) y los workflows disparadores de CI/CD. La infraestructura (Terraform, manifiestos de Kubernetes, pipeline reutilizable) vive en el repo de operaciones `finalproject-ingesoftv-ops`.

> Arquitectura GitOps de repositorios separados: cada merge a `master` genera una imagen versionada que el repo ops despliega en el cluster. Este repo **nunca** modifica manifiestos de Kubernetes ni Terraform.

---

## 📑 Tabla de contenido

1. [Arquitectura de la aplicación](#-arquitectura-de-la-aplicación)
2. [Estructura del repositorio](#-estructura-del-repositorio)
3. [Stack tecnológico](#-stack-tecnológico)
4. [Desarrollo local](#-desarrollo-local)
5. [Pruebas](#-pruebas)
6. [CI/CD, branching y releases](#-cicd-branching-y-releases)
7. [Manual de operaciones básico](#-manual-de-operaciones-básico)
8. [Documentación completa del proyecto](#-documentación-completa-del-proyecto)

---

## 🏗️ Arquitectura de la aplicación

8 microservicios Spring Boot que se comunican de dos formas: **REST** (síncrono) y **eventos Kafka** (asíncrono). Diagrama de detalle: `03-patrones-diseno.md` y el `.drawio` del repo ops (pestaña "Aplicación: servicios, datos y eventos").

| Servicio | Puerto | Responsabilidad | Datos / dependencias |
|---|---|---|---|
| gateway-service | 8087 | Valida QR de entrada al campus | Redis |
| auth-service | 8180 | Login (LDAP + local), emite JWT/QR | PostgreSQL `auth` + LDAP · REST → identity |
| identity-service | 8083 | Bóveda de anonimización de identidad | PostgreSQL `identity` · Kafka |
| promotion-service | 8088 | Rastreo de contactos y cuarentenas | PostgreSQL `promotion` + Neo4j + Redis + Kafka |
| form-service | 8086 | Encuestas de salud y certificados | PostgreSQL `form` + Kafka |
| dashboard-service | 8084 | Analítica anonimizada | PostgreSQL `dashboard` · REST → promotion |
| notification-service | 8082 | Alertas multicanal (push/SMS/email) | Kafka · REST → auth |
| file-service | 8085 | Almacenamiento de archivos | Almacenamiento |

**Eventos Kafka:** `survey.submitted`, `promotion.status.changed`, `certificate.validated`, `audit.identity.accessed`.

---

## 📂 Estructura del repositorio

```
.
├── services/                       # 8 microservicios Spring Boot
│   └── circleguard-<nombre>-service/
│       ├── src/main/               # código + application.yml + logback-spring.xml
│       └── src/test/               # unitarias + integración (Testcontainers)
├── e2e/                            # 31 pruebas end-to-end contra stage
├── tests/
│   ├── performance/                # Locust (locustfile.py + run-locust.sh)
│   └── security/                   # OWASP ZAP (zap-scan.sh)
├── mobile/                         # frontend Expo (React Native)
├── .github/workflows/
│   ├── circleguard-<servicio>.yml  # 8 disparadores que invocan el pipeline de ops
│   ├── sonarcloud.yml              # análisis estático (1× por commit)
│   └── perf-security.yml           # Locust + ZAP contra stage
├── build.gradle.kts                # configuración común (JaCoCo, Sonar, deps)
├── 01-metodologia-agil-branching.md
├── 03-patrones-diseno.md
└── ROLLBACK_PLAN.md
```

---

## 🛠️ Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.4 / Java 21 / Gradle |
| Base relacional | PostgreSQL 17 |
| Base de grafos | Neo4j 5 |
| Caché | Redis 7 |
| Bus de eventos | Apache Kafka 3.7 |
| Resiliencia | Resilience4j (Bulkhead) |
| Observabilidad (libs) | Micrometer (Prometheus) · OpenTelemetry (OTLP) · logstash-logback-encoder |
| Frontend | Expo (React Native) — iOS / Android / Web |
| Pruebas | JUnit 5 · Mockito · Testcontainers · Locust · OWASP ZAP |

---

## 💻 Desarrollo local

### 1. Levantar dependencias (Docker)
```bash
docker-compose -f docker-compose.dev.yml up -d
# Incluye: PostgreSQL, Neo4j, Kafka, Redis y OpenLDAP
```

### 2. Compilar y ejecutar
```bash
./gradlew bootRun --parallel                   # todos los servicios
./gradlew :services:<servicio>:bootRun         # un servicio
```

### 3. Explorar APIs
Cada servicio expone OpenAPI 3.0: `http://localhost:<puerto>/swagger-ui/index.html`

### 4. Frontend
```bash
cd mobile && npm install
npm run start    # menú Expo | npm run android | npm run ios | npm run web
```

---

## 🧪 Pruebas

| Tipo | Cómo se ejecuta | Detalle |
|---|---|---|
| Unitarias | `./gradlew test` | JUnit 5 + Mockito, en los 8 servicios; cubren camino feliz y de error |
| Integración | `./gradlew integrationTest` | Testcontainers levanta PostgreSQL y Kafka reales |
| E2E | módulo `e2e/` (corre contra stage) | 31 escenarios de flujos completos de usuario |
| Rendimiento | `tests/performance/run-locust.sh [light\|medium\|stress]` | Locust con umbrales de p95 y tasa de error |
| Seguridad (DAST) | `tests/security/zap-scan.sh` | OWASP ZAP contra los 8 servicios |
| Cobertura | JaCoCo + SonarCloud | Gate ≥80% (actual ~92%) |

---

## 🚀 CI/CD, branching y releases

### Estrategia de branching — GitHub Flow
- Rama permanente única: `master` (siempre estable y desplegable).
- Ramas de corta duración: `feat/SCRUM-XX-...`, `fix/...`, `refactor/...`, `test/...`, `docs/...`, `chore/...`.
- Todo cambio entra por **Pull Request** con título en **Conventional Commits**, requiere **1 aprobación** y **CI en verde**, y se integra con **Squash & Merge**.
- Detalle completo en `01-metodologia-agil-branching.md`.

### Disparadores de pipeline
- **Push a master** → CI + CD completo (despliegue a dev → stage → E2E → aprobación → prod).
- **Pull Request** → solo CI (build, pruebas, escaneo), sin desplegar.
- Filtros por ruta: el pipeline de un servicio solo se activa si cambian sus archivos.

### Versionado y releases
La versión se calcula automáticamente desde los Conventional Commits. Al desplegar a producción se crea el tag `servicio/vX.Y.Z` y un **GitHub Release** con notas auto-generadas (ver pestaña *Releases*).

---

## 🔧 Manual de operaciones básico

> La operación de infraestructura (cluster, rollback, observabilidad) se detalla en el repo **ops** (`docs/MANUAL_OPERACIONES.md`). Desde la perspectiva de la aplicación:

### Construir e iterar
```bash
./gradlew build                                # compila + pruebas + cobertura
./gradlew :services:<servicio>:test            # pruebas de un servicio
```

### Desplegar un cambio
1. Crear rama desde `master` (`feat/SCRUM-XX-...`).
2. Commits con Conventional Commits.
3. Abrir PR a `master`; esperar CI en verde + aprobación.
4. Squash & Merge → el pipeline despliega a dev y stage automáticamente.
5. Aprobar manualmente el job de producción en GitHub Actions cuando corresponda.

### Verificar el estado de un servicio desplegado
```bash
az aks get-credentials -g circleguard-core-rg -n circleguard-aks
kubectl get pods -n stage
kubectl logs deploy/circleguard-<servicio>-service -n stage
```

### Rollback de la aplicación
```bash
kubectl rollout undo deployment/circleguard-<servicio>-service -n production
```
Procedimiento completo y criterios de activación en `ROLLBACK_PLAN.md`.

### Usuarios de prueba
| Usuario | Password | Rol |
|---|---|---|
| `staff_guard` | `password` | GATE_STAFF |
| `health_user` | `password` | HEALTH_CENTER |
| `super_admin` | `password` | todos |

---

## 📚 Documentación completa del proyecto

| Documento | Contenido |
|---|---|
| `01-metodologia-agil-branching.md` | Scrum, Jira, GitHub Flow, Conventional Commits, Definition of Done |
| `03-patrones-diseno.md` | Patrones de diseño (Bulkhead, Feature Toggle, Cache-Aside, External Config) y su uso en el código |
| `ROLLBACK_PLAN.md` | Plan de rollback desde la perspectiva de la aplicación |
| Repo **ops** | Terraform, manifiestos k8s, pipeline reutilizable, observabilidad, costos y manual de operaciones extendido |

### Cumplimiento del taller (parte de aplicación)

| Sección | Estado | Dónde |
|---|---|---|
| 1. Ágil + Branching | ✅ | `01-metodologia-agil-branching.md` |
| 3. Patrones de diseño | ✅ | `03-patrones-diseno.md`, `services/` |
| 5. Pruebas completas | ✅ | `services/*/src/test`, `e2e/`, `tests/` |
| 6. Change Mgmt + Release Notes | ✅ | PRs, GitHub Releases, `ROLLBACK_PLAN.md` |
| 7. Observabilidad (instrumentación) | ✅ | Micrometer, OTLP, métricas de negocio en `services/` |
| 8. Seguridad (app) | ✅ | JWT + RBAC (`@PreAuthorize`), gestión de secretos |

---

## 🔐 Privacidad y cumplimiento

- **Cumplimiento FERPA:** las identidades reales nunca se almacenan en el grafo de contactos.
- **Derecho al olvido:** purga completa de datos vía el Identity Vault.
- **Privacidad temporal:** las aristas de contacto se purgan automáticamente tras 14 días.
