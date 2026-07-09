# Banking System — Full-Stack Demo Application

**A full-featured banking application showcasing modern backend and frontend architecture.** React + TypeScript SPA with a Spring Boot 3.5 REST API, PostgreSQL-backed persistent sessions, and comprehensive transaction processing.

> **⚠️ Educational Portfolio Project — No Real Money or Payment Rails**  
> This is a simulated banking environment with demo data only. No real funds are transferred. Built to demonstrate full-stack architecture, transaction consistency, authentication/authorization, and deployment readiness.

---

## Features

### Banking Operations
- **Account Management** — Open checking/savings accounts, freeze/unfreeze (admin), view balance & statement
- **Money Movement** — Deposits, withdrawals, internal transfers (same-user), external transfers (validated recipient lookup)
- **Transaction History** — Chronological list with category filtering, date ranges, status, running balance, CSV export
- **Alerts** — Real-time notifications for large transactions, security events, failed operations

### Analytics & Insights
- **Dashboard** — Total balance, spending trends, category breakdown, largest transactions, 30-day flow
- **Balance History** — Monthly aggregate and daily line chart
- **Category Analysis** — Spending by category with totals

### Security & Administration
- **Authentication** — Username/password with BCrypt hashing, database-backed bearer tokens
- **Authorization** — User and admin roles; fine-grained checks on every account/transaction access
- **Admin Console** — User management, account status control, system statistics, activity logs
- **Audit Logging** — All operations recorded with user, timestamp, and outcome

### User Experience
- **Responsive Design** — Mobile-first CSS, tested on all viewports
- **Dark Mode** — Theme toggle with system preference detection
- **Form Validation** — Client-side + server-side; specific error messages (e.g., "Username taken", "Insufficient funds")

---

## Screenshots

Placeholder for project screenshots (owner to populate):
- Place images in `docs/screenshots/`
- Suggested: landing page, dashboard, transfers, analytics, admin panel, mobile view, dark mode

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Frontend** | React + TypeScript + Vite | React 19.1, TS 5.8, Vite 6.3 |
| | Routing & State | React Router 7.6, custom React Context |
| | Charting | Recharts 2.15 |
| | Testing | Vitest 2.1, React Testing Library 16.1 |
| **Backend** | Spring Boot (REST API) | 3.5.14 |
| | Data Access | Spring JdbcTemplate (no ORM) |
| | Schema Migration | Flyway 10+ |
| | Validation | Jakarta Validation (Bean Validation) |
| | Testing | Spring Boot Test, JUnit 5, Mockito |
| **Databases** | Development | H2 (in-memory) |
| | Production | MySQL 8.4+ |
| | Sessions | Database table (salted hash, TTL) |
| **Deployment** | Containerization | Docker & Docker Compose |
| | Runtime | JDK 17+, Node 18+ |
| **Security** | Passwords | BCrypt (strength ≥11) |
| | Authorization | Custom Bearer-token filter |
| | Rate Limiting | Per-IP memory cache (10 req/60s on auth endpoints) |
| | CORS | Allowlist-based (default: localhost:5173) |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│  React 19 SPA (banking-frontend/)                           │
│  - Pages: landing, login, dashboard, accounts, transfer,   │
│    transactions, analytics, alerts, settings, admin        │
│  - Routing: ProtectedRoute, AdminRoute guards              │
│  - State: AuthContext (user, token), ThemeContext         │
└────────────────────────┬────────────────────────────────────┘
                         │ 
                  Vite proxy or 
                  VITE_API_URL
                         │
┌────────────────────────▼────────────────────────────────────┐
│  Spring Boot 3.5 REST API (BankingSystemAPI/)               │
│  Controllers: /auth, /accounts, /transactions, /alerts,    │
│              /analytics, /admin, /profile                  │
├─────────────────────────────────────────────────────────────┤
│  Services (business logic)                                  │
│  - AccountService: open, transfer, withdraw                │
│  - TransactionService: queries, filtering, CSV export      │
│  - AuthService: session creation/revocation                │
│  - AdminService: user/account management                   │
├─────────────────────────────────────────────────────────────┤
│  JdbcTemplate Repositories                                  │
│  - UserRepository, AccountRepository, TransactionRepository│
│  - SessionRepository, AlertRepository, LogRepository       │
└────────────────────────┬────────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────────┐
│  H2 (dev) or MySQL (prod)                                   │
│  - Schema via Flyway (V1__baseline.sql, V2__schema_v2.sql)  │
│  - Data: seeded demo accounts via DataSeeder on first run  │
└─────────────────────────────────────────────────────────────┘
```

**Key Architectural Decisions:**
- **Stateless API (Bearer tokens)**: Each request includes a token; no server-side session state (except token verification).
- **Database-backed tokens**: Tokens are SHA-256 hashes; lookup and expiry checked on every request. No JWT (intentional trade-off: no cryptographic refresh logic, simpler admin revocation).
- **Atomic transactions**: All money movements use conditional `UPDATE balance WHERE balance >= amount` with `@Transactional` to prevent race conditions.
- **Ownership checks**: Every account/transaction endpoint verifies the user owns the resource.
- **JdbcTemplate** (not JPA): Direct SQL for fine-grained control over transaction semantics and running balance calculation.

See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for full schema, API contract, and security model.

---

## Getting Started

### Prerequisites

- **Java 17+** (check with `java -version`)
- **Node 18+** (check with `node -v`)
- **Maven 3.8+** (check with `mvn -v`)

Optional:
- **MySQL 8.4+** (for production-like persistence; H2 used by default)
- **Docker Desktop** (for containerized stack)

### Quick Start — H2 Dev Mode (No Setup Required)

Two terminals, start the backend first:

```bash
# Terminal 1 — Backend (H2 in-memory, data resets on restart)
./run.sh
# Wait for: "Started BankingSystemApiApplication in X seconds"
```

Then start the frontend:

```bash
# Terminal 2 — Frontend
./run-frontend.sh
# Open http://localhost:5173 in your browser
```

**Demo Credentials** (seeded on first run):

| Username | Password | Role | Balance |
|----------|----------|------|---------|
| `demo` | `Demo123!` | User | $50.00 CHECKING, $736.46 SAVINGS |
| `sofia` | `Sofia123!` | User | $1000.00 CHECKING |
| `admin` | `Admin123!` | Admin | — |

**What just happened:**
- Spring Boot started with H2 in-memory database
- Flyway applied `V1__baseline.sql` (schema) + `V2__schema_v2.sql` (v2 tables)
- DataSeeder inserted demo users, accounts, and 85 sample transactions
- Vite dev server proxies `/api/*` to `http://localhost:8080` (see `vite.config.ts`)
- Open browser → login → dashboard with sample data

### MySQL for Persistent Data

To use MySQL instead:

```bash
# Terminal 1 — Setup (run once)
./setup-mysql.sh
# Prompts for password; verifies MySQL connection; creates database

# Terminal 1 — Start API with MySQL
./run-mysql.sh
```

Requires `.env` file:

```bash
# Copy from .env.example
cp .env.example .env

# Edit .env and set a real password:
DB_PASSWORD=YourMysqlPassword1!
DB_USERNAME=root  # change if needed
DB_URL=jdbc:mysql://localhost:3306/banking_system
```

### Docker Compose (All-in-One)

No local Java, Maven, or MySQL installation needed:

```bash
# 1. Copy and configure
cp .env.example .env
# Edit .env: set DB_PASSWORD to something strong

# 2. Start all services (MySQL + API + frontend)
docker compose up --build
# First run: 3-5 min (Maven/npm dependencies). Subsequent runs: 10-30 sec.

# 3. Open http://localhost:5173
# API: http://localhost:8080
# MySQL: localhost:3306

# 4. Stop all
docker compose down
# Reset data (delete volume): docker compose down -v
```

**Inside containers:**
- Backend connects to `db:3306` (service name, not localhost)
- Frontend proxies `/api` to `http://backend:8080` (via `BACKEND_URL` env var)
- Data persists in the `mysql_data` Docker volume

---

## Environment Variables

### Backend (BankingSystemAPI/)

Read from `.env` file or environment; see `application.properties` for defaults.

| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_URL` | `jdbc:h2:mem:banking_system` | JDBC connection string (MySQL/H2) |
| `DB_USERNAME` | `SA` | Database user (H2) or `root` (MySQL) |
| `DB_PASSWORD` | *(none)* | Database password (required for MySQL) |
| `APP_CORS_ORIGINS` | `http://localhost:5173` | CORS allowlist (comma-separated) |
| `APP_TRUST_PROXY` | `false` | Trust `X-Forwarded-For` for rate limit IP (set `true` behind proxy) |
| `APP_RATE_LIMIT_MAX_REQUESTS` | `10` | Max requests per IP per endpoint per 60 seconds |

**Production example:**
```bash
export APP_CORS_ORIGINS=https://mybank.example.com
export APP_TRUST_PROXY=true  # if behind Nginx
export APP_RATE_LIMIT_MAX_REQUESTS=20
./run.sh
```

### Frontend (banking-frontend/)

| Variable | Default | Purpose |
|----------|---------|---------|
| `VITE_API_URL` | *(empty, uses proxy)* | Absolute URL to backend API (production builds only) |

**Dev mode:** Vite proxy in `vite.config.ts` uses `BACKEND_URL` (Docker Compose sets this).  
**Prod mode:** Build time or runtime — set `VITE_API_URL=https://api.example.com npm run build`.

---

## Testing

### Backend (27 tests)

```bash
cd BankingSystemAPI && mvn test
```

Coverage:
- **Transaction Logic** — Deposit, withdrawal, internal/external transfers; insufficient funds; running balance
- **Auth Flows** — Login, logout, session revocation, token expiry
- **Ownership Checks** — Users can only access own accounts/transactions
- **Concurrency** — Concurrent deposits/withdrawals to same account (atomic updates)
- **Query Filtering** — Transactions by date, category, status; pagination
- **Utilities** — Password validation, CSV formatting

### Frontend (26 tests)

```bash
cd banking-frontend && npm test
```

Coverage:
- **Components** — ProtectedRoute, Pagination, TransactionTable
- **Formatting** — Currency, date, locale formatting
- **Routing Guards** — Redirects to login if unauthenticated

### E2E Testing

Manual E2E test plan in **[docs/TEST_PLAN.md](docs/TEST_PLAN.md)**.

---

## API Overview

**Base URL:** `http://localhost:8080/api/`  
**Auth:** Bearer token in `Authorization: Bearer <token>` header

| Endpoint | Method | Purpose |
|----------|--------|---------|
| **Authentication** | | |
| `/auth/register` | POST | Create account (username, password, firstName, lastName) |
| `/auth/login` | POST | Get bearer token (username, password) |
| `/auth/logout` | POST | Revoke token (invalidates session) |
| **Accounts** | | |
| `/accounts` | GET | List user's accounts |
| `/accounts/{id}` | GET | Get account detail (balance, type, status) |
| `/accounts/open` | POST | Create new account (type: CHECKING or SAVINGS) |
| `/accounts/{id}/deposit` | POST | Deposit to account |
| `/accounts/{id}/withdraw` | POST | Withdraw from account |
| `/accounts/lookup` | GET | Search by account number (rate-limited) |
| `/accounts/{id}/status` | PATCH | Change status: ACTIVE, FROZEN (admin only) |
| **Transfers** | | |
| `/transfers/internal` | POST | Send to own account |
| `/transfers/external` | POST | Send to another user's account (lookup + transfer) |
| **Transactions** | | |
| `/transactions` | GET | Query with filters (date, category, status, pagination) |
| `/transactions/{id}` | GET | Get single transaction |
| `/transactions/export` | GET | CSV export (all or filtered) |
| **Analytics** | | |
| `/analytics/summary` | GET | Dashboard data (balance, trends, spending by category) |
| **Alerts** | | |
| `/alerts` | GET | User's alerts (transaction, security) |
| `/alerts/{id}/read` | PATCH | Mark alert as read |
| `/alerts/read-all` | PATCH | Mark all as read |
| **Profile** | | |
| `/profile` | GET | Current user (username, role, name) |
| `/profile/password` | PATCH | Change password |
| **Admin** | | |
| `/admin/stats` | GET | System stats (users, accounts, transactions, volume, failures) |
| `/admin/users` | GET | List all users |
| `/admin/accounts` | GET | List all accounts |
| `/admin/audit-logs` | GET | Activity log (pagination) |

**27 endpoints total** across 7 controllers.

---

## Security

### Implemented

✅ **Password Security**
- BCrypt hashing (strength ≥11); passwords validated client & server (8+ chars, uppercase, number, special char)

✅ **Authentication**
- Username/password → UUID token; salted SHA-256 hash stored in database
- Token verified on every request; auto-revoked on logout or after 24h TTL

✅ **Authorization**
- User role (access own data only); Admin role (system-wide)
- Ownership checks on all account/transaction endpoints (SQL: `WHERE account_id IN (SELECT id FROM accounts WHERE user_id = ?)`)
- `/admin/*` endpoints reject non-admin users (403)

✅ **Data Integrity**
- Atomic conditional updates: `UPDATE accounts SET balance = balance + ? WHERE id = ? AND balance >= 0` with `@Transactional`
- Running balance calculation on every transaction; historical accuracy preserved
- Failed transactions recorded with status (not silent failures)

✅ **HTTP Security**
- CORS allowlist (default: dev only; production: set `APP_CORS_ORIGINS`)
- Bearer token in header (no URL params, no cookies for credentials)
- Server error traces hidden from clients (`server.error.include-stacktrace=never`)
- Rate limiting on sensitive endpoints: 10 req/60s per IP (login, register, lookup)

✅ **Code Practices**
- Input validation: DTOs with `@NotNull`, `@DecimalMin`, regex for usernames
- Parameterized SQL queries (JdbcTemplate safe by default; no string concatenation)
- No secrets in repo (`.env` is `.gitignore`d; env vars from environment)

### Known Limitations

⚠️ **Tokens are Opaque** (No JWT)
- Trade-off: Simpler token revocation, but server must query DB on every request
- Admin can instantly log out all users by clearing the session table
- No refresh-token strategy (token lifetime fixed at 24h)

⚠️ **No HTTPS Locally**
- Dev environment runs on HTTP only
- Tokens sent in plaintext in dev (acceptable for demo; production must use TLS)

⚠️ **No MFA**
- Single-factor auth only
- Acceptable for demo; production adds TOTP or email/SMS verification

⚠️ **Rate Limiting is In-Memory**
- Per-IP tracking stored in HashMap; lost on server restart
- Works for dev/testing; production uses Redis or WAF

⚠️ **Bearer Tokens in SessionStorage**
- Frontend stores token in browser sessionStorage (cleared on tab close)
- Not vulnerable to CSRF (no cookies), but exposed to XSS if frontend has DOM vulnerabilities
- Acceptable for demo; production hardens CSP and sanitizes HTML

---

## Deployment

### Frontend

**Build:**
```bash
cd banking-frontend
npm run build
# Output: dist/ folder (static files)
```

**Deployment Platforms:**
- **Vercel** — `npm run build` + connect repo; auto-deploy on push; set env `VITE_API_URL=https://api.mybank.com`
- **Netlify** — Drag & drop `dist/` or connect repo; set `VITE_API_URL` build env var
- **AWS S3 + CloudFront** — Build locally, upload `dist/` to S3, invalidate CloudFront cache
- **Docker** — Included `Dockerfile` (multi-stage Node Alpine); copy `dist/` to nginx

**Environment:** Set `VITE_API_URL` at build time to your API URL (e.g., `https://api.mybank.example.com`).

### Backend

**Build:**
```bash
cd BankingSystemAPI
mvn clean package -q
# Output: target/banking-system-api-1.0.0.jar
```

**Deployment Platforms:**
- **Render** — Connect Git repo; set env vars (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `APP_CORS_ORIGINS`); auto-deploy
- **Railway** — Similar; builds on push; specify Java 17 buildpack
- **Fly.io** — Deploy JAR with `fly deploy` (Dockerfile included); set secrets for credentials
- **AWS EC2/ECS** — Build Docker image, push to ECR, run container with env vars
- **Heroku** — `git push heroku main`; buildpack auto-detects Java; config vars via Heroku dashboard

**Environment Variables (Production):**
```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL=jdbc:mysql://prod-db.example.com:3306/banking_prod
export DB_USERNAME=prod_user
export DB_PASSWORD=strong_secure_password
export APP_CORS_ORIGINS=https://mybank.example.com
export APP_TRUST_PROXY=true  # if behind load balancer
```

**Database:**
- Use managed MySQL (AWS RDS, Google Cloud SQL, Azure Database for MySQL)
- Flyway auto-migrates on startup; `baseline-on-migrate=true` allows attaching existing schemas
- For safety: test migrations in staging first

**HTTPS / TLS:**
- Use reverse proxy (Nginx, Caddy) or cloud load balancer (AWS ALB, GCP Load Balancer)
- Terminate TLS at proxy; backend listens on HTTP
- Or use Spring Boot's embedded HTTPS (slower; not recommended at scale)

---

## Technical Highlights

### Full-Stack Architecture
- **Unified Type Safety**: TypeScript frontend + Java backend; shared domain models in docs/ARCHITECTURE.md
- **Consistent REST Contract**: OpenAPI-style endpoints; client validates responses
- **Scalable from Demo to Production**: H2 → MySQL, in-memory rates → Redis, Docker Compose → Kubernetes

### Transaction Processing & Consistency
- **Atomic Conditional Updates**: Every transfer/deposit/withdrawal uses `UPDATE ... WHERE balance >= ?` with `@Transactional`
- **Running Balance Calculation**: Real-time balance after each transaction; historical view preserved
- **Failed Transactions Recorded**: On insufficient funds, creates a FAILED transaction row (auditable; not silent)
- **Concurrency Safe**: Multiple deposits to the same account processed serially via database lock (not app-level synchronization)

### Authentication & Authorization
- **Database-Backed Sessions**: Tokens stored as (hash, user_id, expiry) in `sessions` table; no JWT signing overhead
- **Instant Revocation**: Admin can invalidate any token by deleting its row
- **Fine-Grained Ownership**: Every account/transaction check includes user context; SQL queries use `WHERE user_id = ?`
- **Layered Defense**: Auth filter → RequestAuth utility → service method ownership check

### Database Design & Flyway Migration
- **Schema v2 Story**: V1 baseline (old enum names: BASIC, SAVING); V2 adds new tables (sessions, activity_logs) and migrates enums (CHECKING, SAVINGS)
- **Baseline-on-Migrate**: Existing databases at V1 are treated as baseline; Flyway skips V1, applies V2 (no data loss)
- **Migration Safety**: Both migrations are idempotent (CREATE TABLE IF NOT EXISTS, UPDATE CASE statements); safe to re-run

### Security by Default
- **No Secrets in Code**: `.env` is `.gitignore`d; Docker uses secrets; CI/CD uses GitHub Secrets
- **Validation Everywhere**: DTOs validate input; service layer re-checks; database constraints (NOT NULL, UNIQUE)
- **Error Handling**: Specific messages for client (e.g., "Username taken"); generic "unexpected error" for server errors (no stack trace leaks)

### Testing Strategy
- **27 Backend Tests**: Cover happy paths, edge cases (insufficient funds, ownership violations, concurrency)
- **26 Frontend Tests**: Component rendering, routing guards, formatting
- **Unit + Integration**: Backend tests use Spring Boot Test (real in-memory DB) + Mockito; no mocking of repositories
- **CSV Export Tested**: Actual file generation; BOM for Excel compatibility

---

## Troubleshooting

| Problem | Cause | Fix |
|---------|-------|-----|
| **`ECONNREFUSED` on frontend** | Backend not running | Start `./run.sh` first; wait for "Tomcat started on port 8080" |
| **Blank frontend page** | VITE_API_URL mismatch in production build | Verify `VITE_API_URL=https://api.yoursite.com npm run build` |
| **`Access denied for user 'root'`** | MySQL password wrong | Check `.env` DB_PASSWORD matches actual MySQL root password; if changed, run `docker compose down -v` and restart |
| **Port 8080 / 5173 already in use** | Another process on the port | `./run.sh` auto-kills it; or `SERVER_PORT=8081 ./run.sh` for a different port |
| **`Unknown column 'fieldname'`** | Stale database schema | Flyway handles migrations; clear with `docker compose down -v` (data is demo, so safe to reset) |
| **Rate limit 429 during dev** | Too many login attempts | Wait 60 seconds or increase `APP_RATE_LIMIT_MAX_REQUESTS` |
| **Frontend changes not reflecting (Docker)** | Files outside `src/` not hot-reloaded | Changes in `src/` auto-update; others need `docker compose up --build frontend` |
| **Backend changes not reflecting (Docker)** | Java must recompile | Run `docker compose up --build backend` |
| **Data disappeared after restart** | Using H2 (in-memory) | Switch to MySQL for persistence: `./run-mysql.sh` |
| **`403 Forbidden` on admin endpoints** | User is not admin | Login as `admin` account or promote user via admin console |

---

## Project Structure

```
BankingSystemApplication/
├── BankingSystemAPI/                      # Spring Boot backend (Java 17, Maven)
│   ├── src/main/java/com/benbanking/api/
│   │   ├── controllers/                  # REST endpoints (7 controllers, 27 mappings)
│   │   ├── services/                     # Business logic (Account, Transaction, Auth, etc.)
│   │   ├── repositories/                 # JdbcTemplate data access
│   │   ├── models/                       # Entity classes (User, Account, Transaction, etc.)
│   │   ├── dto/                          # Request/Response DTOs with validation
│   │   ├── enums/                        # BankAccountType, TransactionType, etc.
│   │   ├── auth/                         # SessionService, RequestAuth utility
│   │   ├── config/                       # CorsConfig, FilterConfig, GlobalExceptionHandler, RateLimitFilter
│   │   └── seed/                         # DataSeeder (demo data on first run)
│   ├── src/test/java/                    # 27 integration tests
│   ├── src/main/resources/
│   │   ├── application.properties        # Global config
│   │   ├── application-dev.properties    # H2 dev profile
│   │   ├── application-mysql.properties  # MySQL profile
│   │   └── db/migration/
│   │       ├── V1__baseline.sql          # Initial schema (baseline)
│   │       └── V2__schema_v2.sql         # v2 tables + enum migration
│   ├── pom.xml                           # Maven dependencies
│   ├── Dockerfile                        # Multi-stage build (Maven → JRE Alpine)
│   └── target/                           # Compiled JAR (after `mvn package`)
│
├── banking-frontend/                      # React frontend (TS, Vite, Node 18+)
│   ├── src/
│   │   ├── pages/                        # Page components (Login, Dashboard, etc.)
│   │   ├── components/                   # Reusable UI components
│   │   ├── api/                          # API client (fetch wrapper)
│   │   ├── context/                      # React Context (Auth, Theme, Toast)
│   │   ├── lib/                          # Utilities (formatting, validation)
│   │   ├── types.ts                      # TypeScript type definitions
│   │   ├── App.tsx                       # Router setup
│   │   └── main.tsx                      # Entry point
│   ├── src/                              # 26 vitest tests
│   ├── vite.config.ts                    # Vite build + proxy config
│   ├── tsconfig.json                     # TypeScript config
│   ├── package.json                      # npm dependencies
│   ├── Dockerfile                        # Multi-stage build (Node → nginx Alpine)
│   └── dist/                             # Built output (after `npm run build`)
│
├── docs/
│   ├── ARCHITECTURE.md                   # Schema v2, API contract, security model
│   ├── TEST_PLAN.md                      # Manual E2E test scenarios
│   └── screenshots/                      # (Placeholder for project screenshots)
│
├── docker-compose.yml                    # Local stack: MySQL, API, frontend
├── run.sh                                # Start API with H2
├── run-mysql.sh                          # Start API with MySQL
├── run-frontend.sh                       # Start frontend dev server
├── setup-mysql.sh                        # Configure MySQL (run once)
├── .env.example                          # Environment variable template
├── .gitignore                            # Exclude .env, node_modules, target
├── pom.xml (root)                        # (Unused; only BankingSystemAPI/ uses Maven)
└── package.json (root)                   # (Unused; only banking-frontend/ uses npm)
```

---

## Resume Bullets

- **Full-stack banking application** — Spring Boot 3.5 REST API + React 19 TypeScript SPA with persistent sessions, 27 automated backend tests, 26 frontend tests, Docker Compose deployment. Demonstrates transaction consistency (atomic conditional updates), role-based authorization, audit logging.

- **Secure transaction processing** — Database-backed bearer tokens with instant revocation; atomic deposits/withdrawals prevent race conditions; running balance tracked per transaction; failed transactions recorded (not silent failures). All ownership checks enforced at SQL layer.

- **Production-ready architecture** — Flyway schema migrations (baseline-on-migrate for existing databases); environment-based config (H2 dev, MySQL prod); responsive CSS with dark mode; 27 REST endpoints with filtering, pagination, CSV export; error handling without stack trace leaks; rate limiting on auth endpoints.

---

## License

No license file present. This is an educational portfolio project.

---

## Next Steps

**For Recruiters/Interviewers:**
- Start with `/api/auth/login` (credential: demo / Demo123!)
- View accounts and transaction history with filters/export
- Try admin endpoints (creds: admin / Admin123!)
- Toggle dark mode, test mobile viewport (inspect DevTools)
- Review code: `docs/ARCHITECTURE.md`, `BankingSystemAPI/src/main/java/com/benbanking/api/`, `banking-frontend/src/`

**For Contributors:**
- Refer to **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for schema, API contract, and design decisions
- Run tests before committing: `cd BankingSystemAPI && mvn test` and `cd banking-frontend && npm test`
- Follow the TODO checklist in **Deployment roadmap** section above

---

**Built with:** Spring Boot, React, TypeScript, MySQL, Docker, Flyway  
**Author:** Ben David  
**Updated:** 2026-07-08
