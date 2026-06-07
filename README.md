# BankieAPI

REST API for **Bankie**, a banking application built for Project 2.4 (Inholland).
It supports customer onboarding/approval, current & savings accounts, money
transfers, ATM-style deposits/withdrawals, and employee administration, secured
with JWT authentication and role-based authorization.

## Tech stack

- **Java 21**, **Spring Boot 4.0.5**
- **Spring Security** + **JWT** (jjwt 0.12.6), stateless sessions
- **Spring Data JPA** with an in-memory **H2** database
- **MapStruct 1.6.3** for entity ↔ DTO mapping
- **Lombok** for boilerplate reduction
- **Bean Validation** (Jakarta) for request validation
- **springdoc-openapi 3.0.2** for Swagger UI
- **Spring Actuator** for health checks
- Build: **Maven** (wrapper included)

## Prerequisites

- JDK 21 on your `PATH`
- No local database needed — H2 runs in-memory and is recreated on every start

## Configuration

Configuration lives in `src/main/resources/application.yaml` and is driven by
environment variables. **`JWT_SECRET` is required and has no default** — the app
will fail to start without it.

| Variable | Required | Default | Notes |
|---|---|---|---|
| `JWT_SECRET` | **Yes** | — | HMAC-SHA256 signing key. Must be **at least 32 characters** (256 bits). |
| `JWT_EXPIRATION_MS` | No | `86400000` | Token lifetime in ms (default 24h). |
| `PORT` | No | `8080` | HTTP port. |
| `H2_CONSOLE_ENABLED` | No | `false` | Enables the H2 web console at `/h2-console`. Keep off in production. |
| `CORS_ORIGINS` | No | `http://localhost:5173,http://localhost:3000,https://*.netlify.app` | Comma-separated allowed origins. |

Non-secret tuning (in `application.yaml`): `bankie.iban.bank-code` and
`bankie.account.default-daily-limit`.

## Running locally

Set `JWT_SECRET`, then start the app with the Maven wrapper.

**Windows (PowerShell):**
```powershell
$env:JWT_SECRET = "change-me-to-a-32+-character-secret-value"
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**
```bash
export JWT_SECRET="change-me-to-a-32+-character-secret-value"
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI spec:** http://localhost:8080/api-docs
- **Health check:** http://localhost:8080/actuator/health
- **H2 console** (if enabled): http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:bankiedb`, user `sa`, empty password

## Running the tests

```bash
./mvnw test          # macOS / Linux
.\mvnw.cmd test      # Windows
```

## Seed data

On first startup (empty DB) the app seeds demo users and accounts via
`DataLoader`. These credentials are **for local/demo use only**.

| Name | Email | Password | Role | Approved |
|---|---|---|---|---|
| Bankie Employee | `admin@bankie.nl` | `Admin123!` | EMPLOYEE | yes |
| Ryan Reynolds | `ryan@deadpool.com` | `GreenLanternSucks1!` | CUSTOMER | yes |
| Taylor Swift | `taylor@eras.com` | `ShakeItOff123!` | CUSTOMER | yes |
| Keanu Reeves | `keanu@matrix.com` | `YouAreBreathtaking1!` | CUSTOMER | yes |
| Pending Pete | `pete@waiting.nl` | `PetePass123!` | CUSTOMER | **no** (awaiting approval) |

## Authentication flow

1. `POST /auth/register` — a new customer self-registers (created **unapproved**).
2. An employee approves the customer and opens their accounts via
   `POST /accounts/customers/{customerId}/approve`.
3. `POST /auth/login` — returns a JWT.
4. Send the token on subsequent requests: `Authorization: Bearer <token>`.

Tokens are signed with HS256 and carry the user id (subject), email, and role.

## Roles & access

- **CUSTOMER** — can view/manage **only their own** accounts and transactions,
  transfer between accounts, and deposit/withdraw on their own checking account.
- **EMPLOYEE** — can list users, approve customers, open/close accounts, set
  limits, and view all accounts and transactions.

## API overview

All endpoints are under `http://localhost:8080`. `/auth/**`, Swagger, and the
health endpoint are public; everything else requires a valid JWT.

### Auth (`/auth`)
| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/auth/register` | Public | Register a new (unapproved) customer |
| POST | `/auth/login` | Public | Authenticate and receive a JWT |

### Accounts (`/accounts`)
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/accounts` | Authenticated | List accounts (own for customers, all/`?customerId=` for employees) |
| GET | `/accounts/search` | Authenticated | Find accounts by owner first/last name |
| GET | `/accounts/verify-recipient` | Authenticated | Verify a recipient's IBAN against a name |
| POST | `/accounts/customers/{customerId}/approve` | Employee | Approve a customer and create their accounts |
| PATCH | `/accounts/{iban}/close` | Employee | Close an account |
| PATCH | `/accounts/{iban}/absolute-limit` | Employee | Set the account's absolute (overdraft) limit |
| PATCH | `/accounts/{iban}/daily-limit` | Employee | Set the account's daily transfer limit |

### Transactions (`/transactions`)
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/transactions` | Authenticated | List transactions, filterable by `customerId`, `iban`, `type`, date range, amount range |
| POST | `/transactions` | Customer/Employee | Transfer money between accounts |
| POST | `/transactions/withdraw` | Customer/Employee | ATM withdrawal from a checking account |
| POST | `/transactions/deposit` | Customer/Employee | ATM deposit to a checking account |

### Users (`/users`)
| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/users` | Employee | List users; `?status=no-accounts` or `?status=all-closed` to filter |
| GET | `/users/me` | Authenticated | Get the authenticated user's profile |
| PATCH | `/users/me` | Authenticated | Update the authenticated user's profile |

## Domain rules

- All monetary amounts use `BigDecimal`; currency is **EUR**.
- An account's balance may not drop below its **absolute limit** (overdraft floor).
- Outgoing movements are capped by the account's **daily transfer limit**.
- IBANs follow the Dutch format and are generated server-side.
- Business-rule violations return **422 Unprocessable Entity**; malformed requests
  return **400 Bad Request**; missing resources return **404**.

## Project structure

```
src/main/java/com/bankie/bankie_api/
├── config/        # Security, CORS, data seeding
├── controller/    # REST endpoints
├── dto/           # Request/response DTOs (records) + AuthContext
├── entity/        # JPA entities (User, Account, Transaction)
├── enums/         # Role, AccountType, AccountStatus, TransactionType
├── exception/     # Custom exceptions + global handler
├── mapper/        # MapStruct mappers
├── policy/        # Business-rule guards (account/transaction/user)
├── repository/    # Spring Data JPA repositories
├── security/      # JWT filter, JWT service, user details
├── service/       # Application/business logic
└── util/          # IBAN generator
```
