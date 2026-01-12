# Project Architecture: OAuth2 & OIDC Microservices

This document outlines the architecture of the Microservices E-commerce application, secured with OAuth2 and OpenID Connect (OIDC) using Keycloak.

## 1. System Overview

The system follows a **Microservices Architecture** where the frontend interacts with backend services through a central **API Gateway**. Security is delegated to an **Authorization Server** (Keycloak).

```mermaid
graph TD 
    %% ========================================== 
    %% Definitions and Styling 
    %% ========================================== 
    classDef frontend fill:#e3f2fd,stroke:#1565c0,stroke-width:2px,color:#0d47a1; 
    classDef gateway fill:#fff3e0,stroke:#e65100,stroke-width:2px,color:#e65100; 
    classDef auth fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#4a148c; 
    classDef microservice fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px,color:#1b5e20; 
    classDef database fill:#eceff1,stroke:#546e7a,stroke-width:1px,stroke-dasharray: 5 5; 


    User((End User)) 


    %% ========================================== 
    %% Client Side / External 
    %% ========================================== 
    subgraph ClientSide ["🖥️ Client Side (Browser)"] 
        Frontend[React Frontend App<br/>Port: 3000<br/><i>oidc-client-ts</i>]:::frontend 
    end 


    User -->|Interacts| Frontend 


    %% ========================================== 
    %% Docker Infrastructure Boundary 
    %% ========================================== 
    subgraph DockerInfra ["🐳 Docker Compose Infrastructure"] 
        
        %% Security Zone 
        subgraph SecurityZone ["🔐 Security Zone"] 
            Keycloak[Keycloak Auth Server<br/>Port: 9080<br/>Realm: microservices-realm]:::auth 
        end 


        %% Gateway Zone 
        Gateway[API Gateway<br/>Spring Cloud Gateway<br/>Port: 8085]:::gateway 


        %% Backend Services Zone 
        subgraph BackendServices ["📦 Backend Microservices"] 
            
            subgraph ProdDomain [Product Domain] 
                ProductService[Product Service<br/>Port: 8081]:::microservice 
                ProdDB[(H2 DB)]:::database 
                ProductService --- ProdDB 
            end 


            subgraph OrderDomain [Order Domain] 
                OrderService[Order Service<br/>Port: 8082<br/><i>OpenFeign + RequestInterceptor</i>]:::microservice 
                OrderDB[(H2 DB)]:::database 
                OrderService --- OrderDB 
            end 
        end 
    end 


    %% ========================================== 
    %% Flows and Connections 
    %% ========================================== 


    %% 1. Authentication Flow (OIDC) 
    Frontend -- "1. Login Redirect<br/>(Auth Code Flow + PKCE)" --> Keycloak 
    Keycloak -- "2. Issues Tokens<br/>(Access JWT, ID Token)" --> Frontend 


    %% 2. Gateway Validation Flow 
    Gateway -.-|Fetches JWK Set certs for JWT validation| Keycloak 


    %% 3. API Request Flow 
    Frontend -- "3. API Request<br/>Authorization: Bearer JWT" --> Gateway 


    %% 4. Routing & Authorization 
    Gateway -- "Route /products<br/>(Validated JWT passed)" --> ProductService 
    Gateway -- "Route /orders<br/>(Validated JWT passed)" --> OrderService 


    %% 5. Inter-Service Communication 
    OrderService -- "Feign Client Request<br/>(JWT Propagated)" --> ProductService 


    %% Internal Service Details (Notes) 
    note[Both Services use <b>JwtAuthConverter</b><br/>to extract Keycloak roles]:::microservice -.- ProductService 
    note -.- OrderService 
```

## 2. Key Components

### 🖥️ Frontend (React App)
- **Tech**: React.js, Axios, `oidc-client-ts` (or similar adapter).
- **Port**: `3000`
- **Responsibility**: 
  - Handles User Interface.
  - Manages OIDC Login/Logout flows (Authorization Code Flow with PKCE).
  - Stores Access Tokens in memory/session.
  - Sends Requests with `Authorization: Bearer <token>` header.

### 🚪 API Gateway (Spring Cloud Gateway)
- **Tech**: Spring Boot, Spring Cloud Gateway.
- **Port**: `8085`
- **Responsibility**:
  - **Single Entry Point**: All external requests go through here.
  - **Routing**: Routes `/products/**` to Product Service and `/orders/**` to Order Service.
  - **CORS**: centralized CORS configuration allowing requests from `localhost:3000`.
  - **Security**: Validates JWT Signatures locally (Resource Server).

### 🔐 Authorization Server (Keycloak)
- **Tech**: Keycloak (Dockerized).
- **Port**: `9080`
- **Realm**: `microservices-realm`
- **Responsibility**:
  - **Identity Provider**: Manages Users (`user1`, `admin`).
  - **Authentication**: Verifies credentials.
  - **Token Issuance**: Issues Access Tokens (JWT), Refresh Tokens, and ID Tokens.
  - **Role Management**: Defines `USER` and `ADMIN` roles.

### 📦 Microservices

#### 1. Product Service
- **Tech**: Spring Boot, H2 Database.
- **Port**: `8081`
- **Endpoints**:
  - `GET /products`: Public/User access.
  - `POST /products`: **Admin only** (`hasAuthority('ADMIN')`).
  - `DELETE /products/{id}`: **Admin only**.
- **Security**:
  - Acts as a Resource Server.
  - Custom `JwtAuthConverter` extracts Keycloak roles (`realm_access.roles`) into Spring Security Authorities.

#### 2. Order Service
- **Tech**: Spring Boot, H2 Database, OpenFeign.
- **Port**: `8082`
- **Endpoints**:
  - `GET /orders`: View all orders.
  - `GET /orders/search/byCustomerId?customerId={id}`: View specific user's orders.
- **Inter-Service Communication**:
  - Uses **OpenFeign** to fetch product details when an order is placed.
  - **RequestInterceptor**: Propagates the JWT token from the incoming request to the downstream Product Service call to maintain security context.

## 3. Security Flow (OAuth2 & OIDC)

1.  **Login**: User clicks "Login" in React. Redirected to Keycloak login page.
2.  **Auth**: User enters credentials. Keycloak authenticates and redirects back to React with an `authorization_code`.
3.  **Token Exchange**: React exchanges code for `Access Token` and `ID Token` (PKCE verification happens here).
4.  **API Request**:
    *   React sends `GET http://localhost:8085/products` with Header `Authorization: Bearer <jwt>`.
5.  **Gateway Processing**:
    *   Gateway intercepts request.
    *   Validates JWT signature against Keycloak's JWK Set (`http://localhost:9080/.../certs`).
    *   Forwards request to **Product Service**.
6.  **Service Authorization**:
    *   Product Service receives request.
    *   `JwtAuthConverter` reads roles from JWT.
    *   Checks `@PreAuthorize` rules (e.g., is user `ADMIN`?).
    *   Returns response or `403 Forbidden`.

## 4. Docker Infrastructure

The entire stack is containerized via `docker-compose.yml`:

| Service | Container Name | Internal Port | Mapped Port |
| :--- | :--- | :--- | :--- |
| **Keycloak** | `keycloak` | 8080 | 9080 |
| **Discovery** | N/A | N/A | N/A |
| **Product** | `product-service` | 8081 | 8081 |
| **Order** | `order-service` | 8082 | 8082 |
| **Gateway** | `gateway` | 8085 | 8085 |
| **Frontend** | `react-app` | 80 | 3000 |

## 5. How to Run

```bash
# 1. Start all services
docker-compose up -d --build

# 2. Access Frontend
http://localhost:3000

# 3. Access Keycloak Console
http://localhost:9080
```
