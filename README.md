# Secure Microservices with OAuth2 & OIDC

This project implements a secure microservices architecture as per the assignment requirements.

## 🏗 Architecture

- **Frontend**: React (Port 3000) - Secured with Keycloak (OIDC).
- **Gateway**: Spring Cloud Gateway (Port 8085) - Token Relay & Routing.
- **Product Service**: Spring Boot (Port 8081) - Secured Resource Server.
- **Order Service**: Spring Boot (Port 8082) - Secured Resource Server + Feign Client.
- **Identity Provider**: Keycloak (Port 9080).

## ✅ Implemented Features

- [x] **Frontend**: React App with Product Catalog, Cart, and Order History.
- [x] **Security**: OAuth2/OIDC with Keycloak.
- [x] **Product Service**: Full CRUD (Entities, Repository, Controller) with H2 Database.
- [x] **Order Service**: Order placement with stock validation via Feign Client (JWT Propagated).
- [x] **Gateway**: Centralized entry point.
- [x] **Containerization**: Dockerfiles for all services + Docker Compose.
- [x] **DevSecOps**: SonarQube & OWASP Dependency Check plugins configured (see product-service).

## 🚀 Getting Started

### 1. Start Infrastructure (Keycloak & Databases)
```bash
docker-compose up -d
```

### 2. Configure Keycloak
1. Open [http://localhost:9080](http://localhost:9080) (admin/admin).
2. Create Realm: `microservices-realm`.
3. Create Client: `react-client` (Public, Redirect URI: `http://localhost:3000/*`, Web Origins: `*`).
4. Create Roles: `ADMIN`, `USER`.
5. Create Users:
   - `admin` (Role: ADMIN, USER)
   - `user1` (Role: USER)

### 3. Build & Run Services (Locally)
If you don't want to run everything in Docker yet:

**Gateway:**
```bash
cd gateway && ./mvnw spring-boot:run
```

**Product Service:**
```bash
cd product-service && ./mvnw spring-boot:run
```

**Order Service:**
```bash
cd order-service && ./mvnw spring-boot:run
```

### 4. Run Frontend
```bash
cd react-app
npm install
npm start
```

### 5. Run with Docker (Full Stack)
To run everything in containers:

```bash
# Build JARs first
cd gateway && ./mvnw clean package -DskipTests
cd ../product-service && ./mvnw clean package -DskipTests
cd ../order-service && ./mvnw clean package -DskipTests

# Build and Start Containers
cd ..
docker-compose up -d --build
```

## 🛡 DevSecOps
To run security checks (Example on Product Service):

**Vulnerability Scan:**
```bash
cd product-service
./mvnw dependency-check:check
```

**Code Quality:**
```bash
./mvnw sonar:sonar
```
