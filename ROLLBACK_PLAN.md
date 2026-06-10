# Plan de Rollback — CircleGuard

## 1. Propósito

Documentar los procedimientos para restaurar la estabilidad del sistema rápidamente en caso de un fallo tras un despliegue. Este plan cubre rollback a nivel de aplicación, base de datos, código e infraestructura.

## 2. Criterios de Activación

Se debe ejecutar un rollback cuando se cumple **cualquiera** de estas condiciones después de un despliegue:

| Síntoma | Acción |
|---|---|
| Health check del servicio responde `DOWN` | Rollback inmediato |
| Smoke tests fallan en el pipeline | El pipeline bloquea la promoción; no requiere rollback (no llegó a prod) |
| Tasa de error > 5% en producción (monitoreado por gateway) | Rollback inmediato |
| Vulnerabilidad CRITICAL descubierta post-deploy | Rollback o hotfix, lo que sea más rápido |
| Degradación de performance > 30% en latency p99 | Rollback y evaluar |
| Bug funcional reportado por usuarios que bloquea operación crítica | Rollback si no hay hotfix en < 1 hora |

## 3. Rollback de Aplicación (Kubernetes)

### 3.1 Rollback a la versión anterior del deployment

```bash
# Ver historial de revisiones del deployment
kubectl rollout history deployment/<service-name> -n <namespace>

# Rollback a la revisión anterior
kubectl rollout undo deployment/<service-name> -n <namespace>

# Rollback a una revisión específica
kubectl rollout undo deployment/<service-name> -n <namespace> --to-revision=<N>

# Verificar el estado del rollback
kubectl rollout status deployment/<service-name> -n <namespace> --timeout=300s
```

### 3.2 Post-rollback verification

```bash
# Verificar que los pods estén ready
kubectl get pods -l app=<service-name> -n <namespace>

# Verificar health check
kubectl exec <pod-name> -n <namespace> -- wget -qO- http://localhost:<port>/actuator/health

# Verificar que la imagen sea la anterior
kubectl describe deployment/<service-name> -n <namespace> | grep Image
```

### 3.3 Ambientes

| Ambiente | Procedimiento |
|---|---|
| **DEV** | No requiere rollback formal — se redeploya con el fix desde master |
| **STAGE** | `kubectl rollout undo deployment/<service> -n stage` |
| **PROD** | `kubectl rollout undo deployment/<service> -n production` |

## 4. Rollback de Base de Datos

### 4.1 Estrategia

CircleGuard usa **Flyway** para migraciones de base de datos. Flyway no soporta `undo` nativamente para PostgreSQL en el modelo de comunidad. La estrategia de rollback de DB es:

**No se revierten migraciones. Se aplica una nueva migración que deshace el cambio.**

### 4.2 Procedimiento

```sql
-- Ejemplo: Si la migración V2 agregó una columna no nula sin default,
-- la migración de fix sería V3 que la elimina:

-- V3__fix_rollback_V2_remove_column.sql
ALTER TABLE <table_name> DROP COLUMN IF EXISTS <column_name>;
```

```bash
# La nueva migración se aplica automáticamente al reiniciar el servicio
# Flyway detecta que está detrás y ejecuta V3
```

### 4.3 Reglas

- **Nunca** borrar o modificar migraciones ya aplicadas en producción
- **Nunca** ejecutar `flyway repair` sin entender la consecuencia
- La nueva migración debe tener un nombre que indique su propósito: `V3__fix_rollback_V2_add_column.sql`
- Documentar en el commit qué migración está deshaciendo y por qué

## 5. Rollback de Código (git)

### 5.1 Revertir un commit en master

```bash
# Identificar el commit problemático
git log --oneline -10

# Revertir el commit (crea un nuevo commit que deshace los cambios)
git revert <commit-hash>

# Push a master
git push origin master
```

### 5.2 Revertir un merge commit (Squash and Merge)

```bash
# Identificar el merge commit
git log --oneline --merges -5

# Revertir el merge (necesita -m 1 para indicar que nos quedamos en master)
git revert -m 1 <merge-commit-hash>

# Push a master
git push origin master
```

### 5.3 Post-revert

- El pipeline de CI se dispara automáticamente al pushear a master
- La imagen Docker anterior se redeploya en dev
- Se sigue el flujo normal de promoción para stage y prod

## 6. Rollback de Infraestructura (Terraform)

Cubierto en el `ROLLBACK_PLAN.md` del repositorio ops.

## 7. Checklist de Rollback

- [ ] Identificar el servicio afectado y el alcance del fallo
- [ ] Notificar al equipo (compañero vía Jira/comentario)
- [ ] Ejecutar `kubectl rollout undo` para rollback de aplicación
- [ ] Verificar health check post-rollback
- [ ] Verificar que la imagen desplegada es la anterior
- [ ] Si hay cambio de DB, crear migración de fix y desplegar
- [ ] Revertir el commit en master con `git revert`
- [ ] Documentar la incidencia en Jira con causa raíz y acción preventiva
- [ ] Actualizar este documento si el procedimiento reveló mejoras

## 8. Post-Mortem

Dentro de las 24h siguientes al rollback:

1. Documentar causa raíz del fallo
2. Evaluar por qué no fue detectado en stage
3. Definir acción preventiva (mejorar pruebas, monitoreo, etc.)
4. Agregar ticket en Jira para la acción preventiva
