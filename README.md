# SecureBank - Transactional Banking API
## Chai Kok Cheng 217463
A secure backend API for a digital banking system using Java Spring Boot, handling user authentication, fund transfers, and transaction history.

**The Problem: Concurrency & Stateless Security**
Building a secure digital banking REST API from scratch involves strict constraints. The biggest technical challenge with digital money is the race condition (or "double-spend") problem: if two concurrent transfer requests deduct funds from the same account at the exact same millisecond, they both might read the same initial balance and succeed, leading to unauthorized negative balances. Additionally, the system needed a scalable, stateless authentication mechanism.

**The Solution: A Robust Spring Boot Architecture**
I engineered a secure transaction processing backend using Java Spring Boot, PostgreSQL, and Spring Security.
- **Security:** Implemented JSON Web Token (JWT) authentication tied to Role-Based Access Control (RBAC), establishing secure, stateless REST API endpoints.
- **Transactions:** Architected a core `TransferService` wrapped in Spring's `@Transactional` to guarantee ACID compliance, ensuring that if any part of a multi-step fund transfer fails, all database changes are completely rolled back.

**The Roadblocks: Overcoming the Concurrency Bug**
- **What broke:** During stress-testing, I discovered that the standard `@Transactional` isolation wasn't enough. Concurrent threads were reading the account balance simultaneously before the first transaction committed its update, successfully bypassing my "insufficient funds" checks.
- **How I fixed it:** I dove into database concurrency mechanisms and enforced row-level locking via Spring Data JPA's `@Lock(LockModeType.PESSIMISTIC_WRITE)`. This instructed PostgreSQL to lock the specific account row during the balance check. Concurrent transfer requests now automatically queue up and wait until the row lock is released. This single change completely eradicated the concurrency bug, guaranteeing 100% financial accuracy under load.

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


