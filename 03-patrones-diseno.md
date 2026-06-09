# 3. Patrones de Diseño (10%)

Este documento detalla la identificación, implementación y beneficios de los patrones de diseño utilizados en la arquitectura de microservicios de **CircleGuard**.

---

## 1. Patrones de Diseño Existentes en la Arquitectura (Línea Base)

### 1.1 API Gateway / Gatekeeper Pattern
* **Explicación Teórica y Práctica (Justificación):** Actúa como un punto de entrada perimetral y validador de acceso unificado. Centraliza las tareas transversales de seguridad (como verificación de firmas de tokens JWT y validaciones de vigencia de códigos QR en Redis) antes de permitir que una petición acceda a los microservicios de negocio o autorice el paso físico. Esto evita duplicar la lógica de seguridad y el acceso a Redis en cada microservicio, reduciendo el acoplamiento global y optimizando los recursos.
* **Mapeo de Código Explícito:**
  * Controlador del Gateway: [GateController.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-gateway-service/src/main/java/com/circleguard/gateway/controller/GateController.java)
  * Lógica de Validación Perimetral: [QrValidationService.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-gateway-service/src/main/java/com/circleguard/gateway/service/QrValidationService.java)
  * Configuración de Red y Redis: [application.yml (Gateway)](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-gateway-service/src/main/resources/application.yml)

### 1.2 Repository Pattern (Patrón Repositorio)
* **Explicación Teórica y Práctica (Justificación):** Media entre la capa de negocio y la base de datos mapeando colecciones de dominio a consultas físicas. Permite que la capa de servicio permanezca completamente agnóstica a la base de datos subyacente (Neo4j para grafos o PostgreSQL para relacional). Esto facilita el desacoplamiento técnico y la testabilidad, permitiendo mockear la persistencia de datos fácilmente mediante inyecciones en pruebas unitarias.
* **Mapeo de Código Explícito:**
  * Repositorio de Grafos (Neo4j): [UserNodeRepository.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/repository/graph/UserNodeRepository.java) y [CircleNodeRepository.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/repository/graph/CircleNodeRepository.java)
  * Repositorio Relacional (JPA/PostgreSQL): [SystemSettingsRepository.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/repository/jpa/SystemSettingsRepository.java) y [FloorRepository.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/repository/jpa/FloorRepository.java)

### 1.3 Service Layer Pattern (Capa de Servicio)
* **Explicación Teórica y Práctica (Justificación):** Define un límite lógico y encapsula las transacciones y lógica de negocio principales, aislando los controladores HTTP de la complejidad del flujo operativo. Mantiene a los controladores delgados (solo responsables de parsear peticiones y retornar respuestas HTTP) y centraliza las reglas de dominio para facilitar la reutilización de código y mantenimiento.
* **Mapeo de Código Explícito:**
  * Servicio de Promociones y Cercos: [HealthStatusService.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/service/HealthStatusService.java)
  * Ciclos de Vida Automáticos: [StatusLifecycleService.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/service/StatusLifecycleService.java)
  * Lógica de Círculos: [CircleService.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/service/CircleService.java)

### 1.4 Observer / Publish-Subscribe Pattern (Publicador-Suscriptor)
* **Explicación Teórica y Práctica (Justificación):** Facilita la mensajería asíncrona no bloqueante entre microservicios, logrando desacoplamiento temporal y espacial. El emisor publica un evento y puede continuar inmediatamente con su hilo HTTP; los suscriptores escuchan y procesan el evento en background. Esto garantiza resiliencia, ya que si el receptor (servicio de notificaciones) sufre una caída, los mensajes quedan retenidos en Kafka sin causar pérdida de datos.
* **Mapeo de Código Explícito:**
  * Publicador de Eventos (Productor): [HealthStatusService.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-promotion-service/src/main/java/com/circleguard/promotion/service/HealthStatusService.java) (envío de mensajes mediante `kafkaTemplate.send`)
  * Suscriptor de Cercos (Consumidores): [ExposureNotificationListener.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-notification-service/src/main/java/com/circleguard/notification/service/ExposureNotificationListener.java) y [CircleFencedListener.java](file:///c:/Users/Juane/Documents/finalproject-ingesoftv-dev/services/circleguard-notification-service/src/main/java/com/circleguard/notification/service/CircleFencedListener.java)

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
