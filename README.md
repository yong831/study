# product-service

Spring Boot boilerplate for a Product CRUD **microservice** (Java 21).

## Stack
| Area | Tech |
|---|---|
| Language / Runtime | Java 21 (virtual threads enabled) |
| Framework | Spring Boot 4.1.2 — Web MVC, Validation, Data JPA, Cache, Actuator |
| Database | MariaDB 11.4 (tests: H2 in MariaDB mode) |
| Cache | Redis 7.4 (tests: in-memory cache) |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven wrapper |
| Deploy | Docker (multi-stage, layered jar), Docker Compose, Kubernetes (Kustomize) |

## Architecture (MVC layers)
```
Client ──▶ Controller ──▶ Service ──▶ Repository ──▶ MariaDB
          (DTO, @Valid)   (@Transactional,   (Spring Data JPA)
                           @Cacheable ──▶ Redis)
```

```
src/main/java/com/yong831/study
├── StudyApplication.java
├── common
│   ├── config      JpaConfig (auditing), CacheConfig, OpenApiConfig
│   ├── dto         PageResponse
│   ├── entity      BaseTimeEntity (createdAt / updatedAt)
│   ├── exception   ErrorCode, BusinessException, GlobalExceptionHandler (RFC 9457 ProblemDetail)
│   └── web         RequestIdFilter (X-Request-Id propagation + MDC logging)
└── product
    ├── controller  ProductController       REST endpoints
    ├── service     ProductService          business logic, transactions, cache
    ├── repository  ProductRepository       data access
    ├── domain      Product                 JPA entity
    └── dto         ProductCreateRequest / ProductUpdateRequest / ProductResponse
```

## Microservice concepts applied
- **Single responsibility & own data** — this service owns only the `product` domain and its own database (`product_db`).
- **Stateless** — no server session; any replica can serve any request, so it scales horizontally (HPA).
- **Externalized config** — profiles `local` / `test` / `prod`; prod reads all connection info from environment variables (ConfigMap / Secret).
- **Health checks** — `/actuator/health/liveness` and `/actuator/health/readiness` drive Kubernetes probes.
- **Distributed tracing basics** — `X-Request-Id` is reused from the gateway/upstream or generated, returned in the response and written to every log line.
- **Consistent error contract** — ProblemDetail JSON with a stable `code` field.
- **API versioning** — `/api/v1/...`
- **Caching** — read-through Redis cache with eviction on update/delete.
- **Graceful shutdown** — `server.shutdown=graceful` + `preStop` hook for zero-downtime rolling updates.
- **Container-ready** — non-root image, layered jar, container-aware JVM memory settings.

## REST API
| Method | Path | Description | Success |
|---|---|---|---|
| POST | `/api/v1/products` | Create | 201 + `Location` |
| GET | `/api/v1/products/{id}` | Get one (Redis cached) | 200 |
| GET | `/api/v1/products?name=&page=0&size=20&sort=id,desc` | Search / list (paged) | 200 |
| PUT | `/api/v1/products/{id}` | Update (evicts cache) | 200 |
| DELETE | `/api/v1/products/{id}` | Delete (evicts cache) | 204 |

Request body (POST / PUT):
```json
{ "name": "Keyboard", "description": "Mechanical", "price": 1000.00, "stock": 10 }
```

Error response example:
```json
{ "type": "about:blank", "title": "Product not found", "status": 404,
  "detail": "Product not found: id=99", "code": "PRODUCT_NOT_FOUND" }
```

Swagger UI (local): http://localhost:8080/swagger-ui.html

## Run

### 1) Local (IDE / Maven) — infra in Docker
```bash
docker compose up -d              # MariaDB + Redis
./mvnw spring-boot:run            # profile: local
```

### 2) Everything in Docker
```bash
docker compose --profile app up -d --build
curl http://localhost:8080/actuator/health
docker compose --profile app down
```

### 3) Kubernetes
```bash
# build an image the cluster can pull
docker build -t product-service:latest .
#   kind:     kind load docker-image product-service:latest
#   minikube: minikube image load product-service:latest
#   registry: docker tag product-service:latest <registry>/product-service:1.0.0 && docker push ...
#             cd k8s/app && kustomize edit set image product-service=<registry>/product-service:1.0.0

kubectl apply -k k8s/             # app + in-cluster MariaDB + Redis (namespace: product)
# kubectl apply -k k8s/app        # app only, when using managed DB/Redis (edit configmap.yaml)

kubectl -n product get pods
kubectl -n product port-forward svc/product-service 8080:80
```

| Path | Contents |
|---|---|
| `k8s/app` | Namespace, ConfigMap, Secret (sample), Deployment (probes, resources, non-root), Service, HPA, Ingress (`product.local`, nginx) |
| `k8s/infra` | MariaDB StatefulSet (5Gi PVC), Redis Deployment (cache-only) |

> The sample Secrets contain dev credentials. For real environments, manage secrets externally and switch `SPRING_JPA_HIBERNATE_DDL_AUTO` to `validate` with a migration tool (Flyway/Liquibase).
> The `preStop.sleep` hook requires Kubernetes 1.30+.

## Test
```bash
./mvnw test
```
| Test | Scope |
|---|---|
| `ProductServiceTest` | Service unit test (Mockito) |
| `ProductControllerTest` | Web layer (`@WebMvcTest`): status codes, validation, errors, request id |
| `ProductRepositoryTest` | JPA layer (`@DataJpaTest`, H2): auditing, query methods |
| `ProductApiIntegrationTest` | Full CRUD flow + cache population/eviction + health |

Tests need no Docker: they use H2 and an in-memory cache (`application-test.yml`).
