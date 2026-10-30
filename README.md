# Calculator DevOps — CSE 816 SPE Mini-Project

A Spring Boot **Calculator REST API** wrapped in a fully automated CI/CD pipeline:
**GitHub → Jenkins → Maven/JUnit → Docker → Docker Hub → Docker Compose (2 replicas + Nginx)**.

## Tech stack

| Area | Tool |
|---|---|
| App | Java 17, Spring Boot 3.3 (Web + Actuator), Maven |
| Tests | JUnit 5, Spring MockMvc |
| CI/CD | Jenkins declarative pipeline (`Jenkinsfile`) |
| Container | Multi-stage `Dockerfile` (Maven build → JRE-alpine runtime, non-root) |
| Deploy | Docker Compose: `calculator-1`, `calculator-2`, `nginx` load balancer |

## API

Base URL through Nginx: `http://localhost:8090`

| Operation | Endpoint | Example response |
|---|---|---|
| Add | `GET /api/v1/add?a=2&b=3` | `{"operation":"add","a":2.0,"b":3.0,"result":5.0}` |
| Subtract | `GET /api/v1/subtract?a=10&b=4` | `{"operation":"subtract",...,"result":6.0}` |
| Multiply | `GET /api/v1/multiply?a=6&b=7` | `{"operation":"multiply",...,"result":42.0}` |
| Divide | `GET /api/v1/divide?a=7&b=2` | `{"operation":"divide",...,"result":3.5}` |
| Health | `GET /actuator/health` | `{"status":"UP",...}` |

Errors (division by zero, missing or non-numeric parameter) return **HTTP 400**:

```json
{"timestamp":"2026-10-25T16:11:03Z","status":400,"error":"Bad Request",
 "message":"Division by zero is not allowed","path":"/api/v1/divide"}
```

## Build, test and run

```bash
# Build the JAR (needs JDK 17)
mvn clean package -DskipTests

# Run unit + integration tests (reports in target/surefire-reports/)
mvn test

# Run the JAR directly
java -jar target/calculator.jar          # http://localhost:8080
```

### Docker

```bash
docker build -t <dockerhub-user>/calculator:latest .
docker run -d -p 8081:8080 <dockerhub-user>/calculator:latest
curl "http://localhost:8081/api/v1/add?a=2&b=3"
```

### Docker Compose (full stack)

```bash
DOCKERHUB_USER=<dockerhub-user> IMAGE_TAG=latest docker compose up -d
docker compose ps
curl "http://localhost:8090/api/v1/divide?a=7&b=2"
docker compose down
```

Nginx round-robins between the two replicas. The `X-Upstream` response header shows which one answered:
`curl -si "http://localhost:8090/api/v1/add?a=1&b=1" | grep X-Upstream`

## CI/CD pipeline

`Jenkinsfile` stages: **Checkout → Build → Test (JUnit published) → Docker Build (tag = build number) → Push (Docker Hub) → Deploy (`docker compose up -d` + health check)**.
After every run, Jenkins emails the build status (success or failure).

- Trigger: GitHub webhook (`githubPush`), with Poll SCM every 2 minutes as fallback
- Docker Hub login comes from the Jenkins credential `dockerhub-creds`. No secrets are stored in this repo.
- Idempotent: re-running only recreates the containers whose image changed

## Project layout

```
calculator-devops/
├── src/main/java/com/spe/calculator/
│   ├── CalculatorApplication.java
│   ├── controller/CalculatorController.java
│   ├── service/CalculatorService.java
│   ├── dto/{CalculationResponse,ErrorResponse}.java
│   └── exception/GlobalExceptionHandler.java
├── src/main/resources/application.properties
├── src/test/java/com/spe/calculator/
│   ├── service/CalculatorServiceTest.java                 # JUnit 5 unit tests
│   └── controller/CalculatorControllerIntegrationTest.java # MockMvc integration tests
├── nginx/nginx.conf
├── Dockerfile
├── .dockerignore
├── docker-compose.yml
├── Jenkinsfile
├── .gitignore
└── README.md
```
