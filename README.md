# SecureBank - Transactional Banking API
## Chai Kok Cheng 217463
A secure backend API for a digital banking system using Java Spring Boot, handling user authentication, fund transfers, and transaction history.

## Features

- **JWT Authentication**: Secure login with JSON Web Tokens
- **Role-Based Access Control (RBAC)**: Admin and User roles
- **ACID-Compliant Transfers**: Transactional fund transfers with rollback on failure
- **Transaction History**: View all past transactions

## Tech Stack

- Java 17
- Spring Boot 3.2
- Spring Security (JWT)
- Spring Data JPA
- PostgreSQL
- JUnit 5

## Project Structure

```
src/main/java/com/securebank/
├── controller/          # REST Controllers
├── dto/                 # Data Transfer Objects
├── exception/           # Custom Exceptions
├── model/               # JPA Entities
├── repository/          # Data Access Layer
├── security/            # JWT & Security Config
└── service/             # Business Logic
```

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 17

### Setup Database

```bash
createdb securebank
```

### Run the Application

```bash
./mvnw spring-boot:run
```

### Run Tests

```bash
./mvnw test
```

## 📡 API Endpoints

### Authentication (Public)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Register new user |
| `/api/auth/login` | POST | Login, returns JWT |

### Accounts (Protected)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/accounts/balance` | GET | Get account balance |

### Transfers (Protected)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/transfer` | POST | Transfer funds |
| `/api/transfer/history` | GET | Get transaction history |

## 📝 API Examples

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"password123","firstName":"John","lastName":"Doe"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"password123"}'
```

### Get Balance
```bash
curl http://localhost:8080/api/accounts/balance \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Transfer Funds
```bash
curl -X POST http://localhost:8080/api/transfer \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"toAccountNumber":"ACC002","amount":100.00,"description":"Payment"}'
```

## 🔐 Security

- Passwords are hashed using BCrypt
- JWT tokens expire after 24 hours
- All `/api/**` endpoints (except `/api/auth/**`) require authentication
- `/api/admin/**` endpoints require ADMIN role


