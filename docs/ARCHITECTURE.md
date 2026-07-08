# Architecture Specification — Ben Banking (v2)

> Demo banking application for portfolio/educational use only. No real money movement,
> no real payment rails. All balances and transfers are simulated.

## Stack (verified, keep)

- **Backend**: Spring Boot 3.5 (Java 17+ target, runs on 25), Maven, `JdbcTemplate` repositories (no JPA — deliberate choice: explicit SQL, easy to explain in interviews)
- **Frontend**: React 19 + Vite 6 + TypeScript, react-router 7, hand-rolled CSS design system (no Tailwind)
- **DB**: H2 in-memory (`dev` profile, default), MySQL 8 (`mysql` profile / Docker)
- **Auth**: Custom bearer tokens (opaque UUID). Upgrading from in-memory map → **DB-backed sessions** (survive restarts, revocable, expiring). Token stored SHA-256-hashed in DB.
- **Migrations**: **Flyway** (new) replaces raw `schema.sql` init.

## Money handling (critical change)

All monetary amounts move from `double` → **`java.math.BigDecimal`** in Java. DB stays `DECIMAL(19,2)`.
- Comparisons via `compareTo`, never `equals`/`==`.
- Scale 2, `RoundingMode.HALF_EVEN` when normalizing input.
- Backend never trusts client-computed balances.

## Database schema v2 (Flyway)

Migrations live in `BankingSystemAPI/src/main/resources/db/migration/` and run on both H2 and MySQL (use portable SQL; `flyway-mysql` dependency added).

- `V1__baseline.sql` — current users/accounts/transactions/logs tables.
- `V2__schema_v2.sql` — the changes below.

### users (extend)
- add `created_at TIMESTAMP NOT NULL`, `updated_at TIMESTAMP NOT NULL`.

### accounts (extend)
- add `created_at`, `updated_at TIMESTAMP NOT NULL`.
- migrate type values: `BASIC` → `CHECKING`, `SAVING` → `SAVINGS` (update enum `BankAccountType { CHECKING, SAVINGS }`).
- widen `balance` to `DECIMAL(19,2)`.

### transactions (rebuild — v2)
| column | type | notes |
|---|---|---|
| transaction_id | INT identity PK | |
| reference | VARCHAR(40) NOT NULL UNIQUE | `TXN-` + UUID fragment, user-facing |
| account_id | INT NOT NULL FK | the account this row belongs to |
| counterparty_account_id | INT NULL | other side of a transfer |
| transaction_type | VARCHAR(30) | DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN |
| status | VARCHAR(20) | PENDING, COMPLETED, FAILED, REVERSED |
| amount | DECIMAL(19,2) NOT NULL | always positive; type carries direction |
| running_balance | DECIMAL(19,2) NULL | account balance after this row (null for FAILED) |
| category | VARCHAR(40) NOT NULL | see categories below |
| description | VARCHAR(255) | |
| memo | VARCHAR(140) NULL | user-supplied |
| created_at | TIMESTAMP NOT NULL | |

Indexes: `(account_id, created_at)`, `(status)`.

**Failed transactions are recorded** (status=FAILED, running_balance NULL) — e.g. insufficient funds — so history and admin views show them.

Categories (server-side enum `TransactionCategory`): INCOME, TRANSFER, GROCERIES, DINING, RENT, UTILITIES, ENTERTAINMENT, SHOPPING, TRANSPORT, HEALTH, FEES, OTHER. Deposits default INCOME, transfers TRANSFER, withdrawals default OTHER; client may pass a category for deposits/withdrawals.

### sessions (new)
`session_id` PK, `token_hash CHAR(64) NOT NULL UNIQUE` (SHA-256 hex), `user_id` FK, `created_at`, `expires_at` (now + 24h), `last_seen_at`. Expired rows rejected and purged opportunistically.

### alerts (new)
`alert_id` PK, `user_id` FK, `alert_type` (LOW_BALANCE, LARGE_TRANSACTION, TRANSFER_RECEIVED, TRANSACTION_FAILED, SECURITY), `severity` (INFO, WARNING, CRITICAL), `message VARCHAR(255)`, `is_read BOOLEAN`, `created_at`.

Alert rules (in service layer, post-commit of the triggering operation):
- balance drops below $100 → LOW_BALANCE (WARNING)
- single transaction ≥ $1,000 → LARGE_TRANSACTION (INFO)
- incoming external transfer → TRANSFER_RECEIVED (INFO)
- failed transaction → TRANSACTION_FAILED (WARNING)
- password change → SECURITY (INFO)

### logs (audit — extend usage, keep table)
Log: LOGIN, LOGIN_FAILED (new enum value), LOGOUT, OPEN_NEW_BANK_ACCOUNT, DEPOSIT, WITHDRAWAL, TRANSFER, EXTERNAL_TRANSFER (new), CHANGE_PASSWORD, UPDATE_PERSONAL_INFORMATION, ADMIN_ACCOUNT_STATUS_CHANGE (new).

## Service-layer rules

- `AccountService.deposit/withdraw` become `@Transactional` (they weren't).
- All mutations validate: positive amount, ACTIVE account, ownership (`findByIdAndUserId`), sufficient funds for debits, max single-operation amount $1,000,000.
- Services **throw domain exceptions** instead of returning boolean/null:
  `ApiException(HttpStatus, message)` hierarchy or specific `InsufficientFundsException`, `AccountNotFoundException`, `AccountNotActiveException`, `InvalidAmountException` → handled centrally in `GlobalExceptionHandler` with precise messages (`"Insufficient funds: available balance is $X."`).
- Transfers write both legs + running balances inside one `@Transactional` method. Deterministic lock order (load/update accounts in ascending account_id order) to avoid deadlocks.
- External transfer: recipient resolved by **account number**; sender may not transfer to own account via external route (use internal); recipient account must be ACTIVE; sender identity revealed to recipient only as first name + masked account.

## REST API contract (v2)

All authenticated routes require `Authorization: Bearer <token>`. Errors: `{ "message": string }` with proper status. Paginated responses: `{ "items": [...], "page": n, "size": n, "totalItems": n, "totalPages": n }`.

### Auth `/api/auth`
- `POST /register` — existing; returns `{token, userId, username, role, firstName}`.
- `POST /login` — existing; logs LOGIN / LOGIN_FAILED audit rows.
- `POST /logout` — revokes DB session.
- `GET /me` — returns full safe profile: userId, username, role, firstName, lastName, email, phoneNumber, address, createdAt.
- `PUT /password` — `{currentPassword, newPassword}`; validates current, strength-checks new, revokes **other** sessions, audit log + SECURITY alert.

### Profile `/api/profile`
- `PUT /` — update firstName, lastName, email, phoneNumber, address (validated).

### Accounts `/api/accounts`
- `GET /` — own accounts (adds `createdAt`, masked number `•••• 1234` computed client-side; API returns full number to owner only).
- `POST /` — `{accountType: CHECKING|SAVINGS, openingBalance}`; max 5 accounts/user.
- `GET /{id}` — own account detail.
- `POST /deposit`, `POST /withdraw` — `{accountId, amount, category?, memo?}`.
- `POST /transfer` — internal (own→own): `{fromAccountId, toAccountId, amount, memo?}`.
- `GET /lookup?accountNumber=` — resolve recipient for external transfer: returns `{accountNumber, ownerFirstName, ownerLastInitial}` or 404. Only ACTIVE accounts; never returns balance/userId.
- `POST /transfer/external` — `{fromAccountId, toAccountNumber, amount, memo?}`.

### Transactions `/api/transactions`
- `GET /` — query params: `page` (0-based), `size` (default 20, max 100), `accountId`, `type`, `status`, `category`, `search` (matches description/memo/reference), `from`, `to` (ISO dates), `sort` (`date_desc` default, `date_asc`, `amount_desc`, `amount_asc`). Ownership-scoped. Paginated response.
- `GET /{id}` — own transaction detail.
- `GET /export` — same filters, returns `text/csv` attachment.

### Analytics `/api/analytics`
- `GET /summary` — `{totalBalance, totalIn30d, totalOut30d, spendingByCategory: [{category, total}], monthlyFlow: [{month, inflow, outflow}] (last 6), largestTransactions: [top 5 completed], balanceHistory: [{date, balance}] (30d, derived from running balances)}`.

### Alerts `/api/alerts`
- `GET /` — own alerts, newest first (limit 50), plus `unreadCount`.
- `POST /{id}/read`, `POST /read-all`.

### Admin `/api/admin` (filter-enforced ADMIN role — keep, plus per-controller assertion)
- `GET /stats` — totalUsers, totalAccounts, totalTransactions, totalVolume, failedTransactions24h.
- `GET /users?search=` — safe DTOs.
- `GET /accounts?search=` — with owner username.
- `GET /transactions?status=&page=` — recent, paginated.
- `PATCH /accounts/{id}/status` — freeze/unfreeze/close; audit-logged with admin userId.
- `GET /audit-logs?page=` — paginated audit trail.

## Seed data (dev profile)

`DataSeeder` (CommandLineRunner, runs when users table is empty):
- `demo` / `Demo123!` (USER) — checking + savings, ~90 days of realistic categorized transactions (salary deposits, rent, groceries, transfers), a few alerts.
- `sofia` / `Sofia123!` (USER) — second user as external-transfer recipient.
- `admin` / `Admin123!` (ADMIN).
Amounts/dates generated with a fixed random seed so demos are reproducible. Runs on both H2 and MySQL.

## Frontend v2

New dependency: `recharts` (charts). Dev-deps: `vitest`, `@testing-library/react`, `jsdom`.

### Routes
```
/                  Landing page (public, marketing-style)
/login /register   Auth pages (public; redirect to /app if signed in)
/app               AppLayout (ProtectedRoute): sidebar + topbar + <Outlet/>
  /app             Dashboard (default index)
  /app/accounts    Accounts list + open-account modal
  /app/accounts/:id  Account detail + its transactions
  /app/transfer    Transfer flow (internal/external, review step, result)
  /app/transactions  History: search, filters, sort, pagination, CSV export, detail modal
  /app/analytics   Charts: category donut, monthly in/out bars, balance line, largest txns
  /app/alerts      Alert center
  /app/settings    Profile edit, password change, appearance (theme), demo notice
  /app/admin       AdminRoute: stats, users, accounts (freeze/unfreeze), transactions, audit log
*                  NotFound
```

### Structure
```
src/
  api/client.ts        typed endpoints incl. pagination/filters
  context/AuthContext  session + /me hydration; ToastContext; ThemeContext
  components/ui/       Button, Card, StatCard, Input, Select, Badge, Modal,
                       Spinner, EmptyState, ErrorState, Toast, Pagination, Tabs
  components/layout/   AppLayout, Sidebar, Topbar (alerts bell w/ unread badge, theme toggle, user menu)
  components/          ProtectedRoute, AdminRoute, TransactionTable, AccountCard, charts/*
  pages/               one file per route
  lib/format.ts        money/date/mask helpers
  types.ts             mirrors API DTOs
```

### Design system
- CSS custom properties in `index.css`: brand indigo/blue palette, neutral grays, semantic success/warning/danger tokens; light + dark themes via `[data-theme]` on `<html>`, persisted in localStorage, defaults to `prefers-color-scheme`.
- Inter-ish system font stack, consistent 4px spacing scale, 12px radii cards, subtle shadows.
- Landing page: hero, feature cards, security blurb, demo-credentials callout, CTA to register. Clearly labeled "Demo project — no real money".
- Token storage stays in `sessionStorage` (documented trade-off).

## Security posture

Kept/extended: BCrypt, rate limiting (extend to 30/min on all `/api/*`? no — keep 10/min auth-only), CORS allowlist via env, no stack traces, H2 console off, DTO validation, ownership checks on every account/transaction access, admin role filter + controller assertions, no secrets in repo (.env), sessions hashed + expiring, generic login errors, password strength rules, audit logging. Documented limitations: no HTTPS locally, no MFA, no CSRF concerns (no cookies), demo-only.

## Testing

Backend (JUnit + Spring Boot Test, H2):
- `AccountServiceTest` — deposit/withdraw/transfer happy paths, insufficient funds, frozen account, negative amount, ownership violation, running balance correctness.
- `ExternalTransferTest` — recipient lookup, inactive recipient, self-transfer rejection, both legs + alert created.
- `TransactionQueryTest` — filters, pagination, ownership scoping, CSV export.
- `AuthFlowTest` (MockMvc) — register/login/me/password-change/session-expiry/admin 403.
- Keep existing `BankingApiSecurityTest`, `PasswordUtilTest`.

Frontend (Vitest + RTL): format helpers, TransferPage validation, ProtectedRoute redirect, TransactionTable rendering states.

E2E: documented manual test plan in `docs/TEST_PLAN.md` (Playwright optional future work).

## Implementation invariants (from architect review — follow verbatim)

1. **Atomic balance writes — no read-modify-write.** All debits/credits use conditional relative UPDATEs inside `@Transactional` methods and check affected rows:
   ```sql
   -- debit: UPDATE accounts SET balance = balance - ?, updated_at = ? WHERE account_id = ? AND status = 'ACTIVE' AND balance >= ?
   -- credit: UPDATE accounts SET balance = balance + ?, updated_at = ? WHERE account_id = ? AND status = 'ACTIVE'
   ```
   0 rows on debit → re-check existence/status to throw the precise exception (insufficient funds vs. frozen vs. not found). `running_balance` = `SELECT balance` re-read **within the same transaction**, immediately after the UPDATE. `deposit`/`withdraw` become `@Transactional` too.
2. **Internal transfer ownership fix (pre-existing bug).** `POST /api/accounts/transfer` must load the destination with `findByIdAndUserId(toAccountId, userId)` — today any account ID is accepted. Cross-user movement only via `/transfer/external`.
3. **Flyway owns the schema everywhere.**
   - `spring.sql.init.mode=never` in dev; remove `schema-locations`; delete/retire `schema-h2.sql` as an init script.
   - `application-mysql.properties`: `spring.flyway.baseline-on-migrate=true`, `spring.flyway.baseline-version=1`.
   - Remove the `docker-entrypoint-initdb.d` schema mounts from `docker-compose.yml`; document `docker compose down -v` for stale volumes. Retire manual schema step in `setup-mysql.sh`.
   - Migrations: no `CREATE DATABASE`/`USE`; `INT AUTO_INCREMENT` identities; `DATETIME` (not `TIMESTAMP`); portable across H2 `MODE=MySQL` and MySQL 8. H2 dev must set MODE=MySQL in the JDBC URL.
   - **V2 is additive ALTER-with-backfill, not DROP/CREATE**: add columns with defaults, backfill `reference` per-row before adding UNIQUE, run `BASIC→CHECKING` / `SAVING→SAVINGS` UPDATEs (must complete before app code reads enums).
4. **BigDecimal DTO validation:** `@NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer=17, fraction=2)` on every amount field (`@DecimalMin` alone passes null). Normalize with `setScale(2, HALF_EVEN)` at the service boundary. Mappers use `getBigDecimal`/`setBigDecimal`.
5. **Sessions:** hash token (SHA-256 hex) inside `SessionService` on both insert and lookup; store `user_id` only and JOIN `users` at lookup so role/freeze changes apply immediately; expired rows deleted on lookup; throttle `last_seen_at` writes (only if >60s stale). Password change deletes `sessions WHERE user_id = ? AND token_hash <> ?` (caller's current token survives).
6. **Domain exceptions must have handlers**: map InsufficientFunds/InvalidAmount/AccountNotActive → 400 (or 409), NotFound → 404 in `GlobalExceptionHandler`; keep `{message}` shape (validation handler may add optional `errors`).
7. **CSV export via fetch+blob** on the frontend (browsers can't set Authorization on `<a href>`); never put tokens in query strings.
8. **Lookup endpoint hardening:** exact full account-number match only; 404 for both missing and non-ACTIVE; response is `{maskedAccountNumber, ownerFirstName, ownerLastInitial}` (never full number, userId, or balance); add `/api/accounts/lookup` to `RateLimitFilter` paths + `FilterConfig` patterns.
9. **Rate limiter:** use `request.getRemoteAddr()` by default; honor `X-Forwarded-For` only behind `app.trust-proxy=true`.
10. **Admin defense-in-depth:** keep filter check, add explicit role assertion in `AdminController` handlers; audit-log status changes with acting admin's userId (`ADMIN_ACCOUNT_STATUS_CHANGE`).
11. **Alerts fire post-commit** — never inside the money `@Transactional` (alert failure must not roll back a transfer).
12. **Login hardening:** run a dummy BCrypt check when username is unknown (timing); audit LOGIN / LOGIN_FAILED.
13. **CORS:** origins from env property; `allowCredentials(false)` (bearer header, no cookies).
14. **Analytics bucketing:** UTC. `balanceHistory` = per calendar day (last 30), last `running_balance` per account on/before that day, carry forward on quiet days, summed across accounts. `monthlyFlow` = same UTC month buckets, last 6.
15. **Pagination:** clamp `size` to [1,100], `page` to ≥0 server-side.
16. **Frontend enum lockstep:** `types.ts`/`client.ts` move to `CHECKING|SAVINGS` and v2 transaction types (DEPOSIT, WITHDRAWAL, TRANSFER_OUT, TRANSFER_IN) in the same change.

## Non-goals

Real payments, KYC, webhooks, microservices, Kafka, JWT (opaque DB sessions are the deliberate, defensible choice), i18n.
