# circleguard-auth-service

## Pipeline / CI-CD

Este servicio usa el pipeline reutilizable definido en **ops** (`.github/workflows/_cicd-pipeline.yml`).

### Disparo automático
- Push a `master` en este repo → ejecuta pipeline completo (CI + CD a DEV/STAGE/PROD)
- PR a `master` → ejecuta solo CI (build, test, scan)

### Disparo manual (workflow_dispatch)
Desde la UI de GitHub Actions en **este repo (dev)**:
1. Ir a **Actions** → **circleguard-auth-service** workflow
2. Click **Run workflow**
3. Opcional: ingresar `dev-branch` (default: `master`)
4. **Run workflow**

Desde terminal (GitHub CLI):
```bash
gh workflow run circleguard-auth-service.yml -R ThomasBrueck/finalproject-ingesoftv-dev -f dev-branch=mi-rama
```

### Qué hace el pipeline
1. **CI**: Build, test (JUnit + Testcontainers), JaCoCo ≥70%, SonarCloud, Trivy, Docker build + push a ACR
2. **Deploy DEV**: Deploy automático a namespace `dev` en AKS
3. **Deploy STAGE**: Deploy automático a namespace `stage` en AKS
4. **E2E Tests (gate)**: Port-forward a servicios en STAGE + `./gradlew test` en `app/e2e` — **bloquea PROD si falla**
5. **Deploy PROD**: Requiere aprobación manual + E2E verde
6. **Tag Release**: SemVer tag automático

### Secrets requeridos (configurados en dev repo)
- `AZURE_CREDENTIALS` — Service Principal para Azure login
- `ACR_USERNAME` / `ACR_PASSWORD` — Credenciales ACR
- `SONAR_TOKEN` — Token SonarCloud
- `DEV_REPO_TOKEN` — PAT con acceso a este repo (para checkout en ops)

### Ver logs
- GitHub Actions → run del workflow → job `pipeline` → steps
- Si falla E2E o deploy: se crea/actualiza issue con label `pipeline-failure`

### E2E local (opcional)
```bash
cd e2e
./gradlew test
```
Requiere Docker Compose levantado (`docker compose -f docker-compose-e2e.yml up -d`).
<!-- trigger pipeline test -->
