# 3. Patrones de Diseño (10%)

Este documento detalla la identificación, implementación y beneficios de los patrones de diseño utilizados en la arquitectura de microservicios de **CircleGuard**.

---

## 1. Patrones de Diseño Existentes en la Arquitectura

### 1.1 API Gateway Pattern
* **Propósito**: Proporcionar un único punto de entrada para todos los clientes hacia los microservicios internos.
* **Implementación**: `circleguard-gateway-service`. Autenticación de tokens JWT, ruteo dinámico hacia servicios internos y control de CORS.
* **Beneficios**: Desacopla los clientes de la topología interna y simplifica el control de seguridad perimetral.

### 1.2 Repository Pattern
* **Propósito**: Abstraer los detalles de persistencia y mediar entre la lógica de negocio y las bases de datos.
* **Implementación**: `UserNodeRepository` (Neo4j), `SystemSettingsRepository` (JPA/PostgreSQL) en todos los servicios.
* **Beneficios**: Facilita las pruebas unitarias con mocks y permite cambiar la tecnología de persistencia sin modificar la lógica de negocio.

### 1.3 Service Layer Pattern
* **Propósito**: Encapsular la lógica de negocio en una capa dedicada y mantener los controladores HTTP delgados.
* **Implementación**: Clases `@Service` como `HealthStatusService`, `CircleService`, `FloorService`.
* **Beneficios**: Centraliza las reglas de negocio, coordina transacciones y mejora la testabilidad.

### 1.4 Observer / Publish-Subscribe Pattern
* **Propósito**: Comunicación asíncrona y reactiva entre microservicios sin acoplamiento temporal.
* **Implementación**: **Apache Kafka**. Eventos publicados en tópicos como `promotion.status.changed` y `circle.fenced`, consumidos de forma asíncrona por `circleguard-notification-service`.
* **Beneficios**: Incrementa la resiliencia y escalabilidad procesando tareas pesadas fuera del hilo HTTP principal.

---

## 2. Patrones de Diseño Adicionales Implementados

### 2.1 Patrón de Resiliencia: Bulkhead (Mamparo) ← Implementación Principal

**Servicio**: `circleguard-auth-service` → comunicación con `circleguard-identity-service`

**Problema que resuelve**: Si `circleguard-identity-service` se vuelve lento o está caído, las peticiones al `auth-service` comenzarían a acumularse esperando respuesta, agotando todos los hilos disponibles del servidor web y colapsando el servicio de autenticación por completo. Esto es un *fallo en cascada*.

**Implementación**:
- Ubicada en [`IdentityClient.java`](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-auth-service/src/main/java/com/circleguard/auth/client/IdentityClient.java) del servicio `circleguard-auth-service`.
- Usa un **Bulkhead de tipo Semáforo** de **Resilience4j**, que limita el número máximo de llamadas concurrentes simultáneas hacia `identity-service`.
- Si el número de llamadas concurrentes supera el límite (`max-concurrent-calls: 5`), la solicitud adicional espera como máximo 200ms (`max-wait-duration`). Si no puede entrar, se rechaza inmediatamente y se ejecuta el método de fallback.
- El `RestTemplate` fue migrado de `new RestTemplate()` a un **Bean de Spring** declarado en `RestClientConfig.java`, permitiendo su inyección de dependencias y reemplazo por mocks en pruebas.

**Configuración externa en `application.yml`** (CA 1.3):
```yaml
resilience4j:
  bulkhead:
    instances:
      identityService:
        max-concurrent-calls: 5    # Máximo de llamadas simultáneas
        max-wait-duration: 200ms   # Espera antes de rechazar
```

**Mecanismo de Fallback** (CA 2.1, 2.2, 2.3):
- Si el Bulkhead está lleno o la llamada falla, se invoca `getAnonymousIdFallback`.
- El fallback retorna un UUID determinista generado a partir del nombre del usuario (`UUID.nameUUIDFromBytes`), garantizando que el sistema de autenticación siga funcionando con una identidad segura de contingencia.
- Se registra un log de nivel `WARN` indicando la saturación.

**Observabilidad** (CA 3.1):
- Métricas de Bulkhead expuestas en `/actuator/health` y `/actuator/metrics`.

**Pruebas Unitarias** (CA 4.1): Archivo [`IdentityClientTest.java`](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-auth-service/src/test/java/com/circleguard/auth/client/IdentityClientTest.java):
- ✅ Llamada exitosa cuando el servicio está disponible.
- ✅ Fallback ejecutado al invocar con `BulkheadFullException`.
- ✅ Fallback ejecutado al simular un error de red (`RestClientException`).
- ✅ UUID determinista reproducible para la misma identidad.

**Beneficios**:
- Aísla los hilos del `auth-service` de los fallos de `identity-service`.
- El servicio de autenticación sigue operativo aunque `identity-service` esté caído.
- Respuesta inmediata de contingencia sin bloquear nuevos hilos.

---

### 2.2 Patrón de Configuración Dinámica: Feature Toggle (Bandera de Característica)

**Servicio**: `circleguard-promotion-service`

* **Propósito**: Habilitar o deshabilitar funcionalidades de negocio en tiempo de ejecución sin redesplegar la aplicación.
* **Implementación**: La bandera `unconfirmedFencingEnabled` se persiste en PostgreSQL (`SystemSettings`). El administrador puede activarla/desactivarla via API REST (`/api/v1/admin/settings/toggle-unconfirmed-fencing`). En `HealthStatusService`, si está desactivada, se suspende la propagación epidemiológica en cascada a través de Neo4j.
* **Beneficios**: Control operativo instantáneo sin necesidad de redespliegue.

---

### 2.3 Patrón de Rendimiento: Cache-Aside

**Servicio**: `circleguard-promotion-service`

* **Propósito**: Reducir latencias y carga sobre las bases de datos almacenando temporalmente resultados frecuentes.
* **Implementación**: Spring Cache con Redis. `@Cacheable` en estados de usuario y configuraciones del sistema; `@CacheEvict` al actualizar para mantener consistencia.
* **Beneficios**: Respuestas sub-milisegundo para consultas frecuentes y menor carga sobre Neo4j/PostgreSQL.

---

### 2.4 Patrón de Configuración Externa: External Configuration

**Alcance**: Todos los microservicios

* **Propósito**: Separar completamente la configuración del entorno del código fuente (12-Factor App, regla III).
* **Implementación**: Variables de entorno inyectadas a través de Kubernetes Secrets en los manifiestos YAML del repositorio de operaciones. Valores de respaldo definidos con la sintaxis `${VARIABLE:default}` en `application.yml`.
* **Beneficios**: El mismo artefacto Docker se despliega sin modificaciones en los ambientes `dev`, `stage` y `production`.

---

## 3. Matriz de Resumen de Patrones

| Patrón | Tipo | Servicio | Tecnología | Beneficio Principal |
|---|---|---|---|---|
| **API Gateway** | Estructural | `circleguard-gateway-service` | Spring Cloud Gateway | Punto de entrada único y seguridad perimetral |
| **Bulkhead** | Resiliencia | `circleguard-auth-service` | Resilience4j | Aísla hilos y evita colapsos por sobrecarga de `identity-service` |
| **Feature Toggle** | Configuración | `circleguard-promotion-service` | PostgreSQL + Spring JPA | Encendido/Apagado dinámico de propagación de cercos epidemiológicos |
| **Cache-Aside** | Rendimiento | `circleguard-promotion-service` | Redis + Spring Cache | Baja latencia y ahorro de consultas a Neo4j |
| **External Configuration** | Arquitectónico | Todos los microservicios | Kubernetes Secrets | Portabilidad de imágenes entre entornos |
