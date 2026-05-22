# Banking System Application

Full-stack banking app — **Spring Boot API** + **React + Vite frontend** + **MySQL** (or in-memory H2 for quick dev).

---

## Quick start (two terminals, every time)

> **Order matters.** Start the backend first, then the frontend.

### Terminal 1 — API

```bash
./run.sh          # H2 in-memory (no MySQL needed — data resets on restart)
```

or with MySQL:

```bash
./setup-mysql.sh  # run once to create tables
./run-mysql.sh    # start API with MySQL (persistent data)
```

API will be available at **http://localhost:8080**

### Terminal 2 — Frontend

```bash
./run-frontend.sh
```

Open **http://localhost:5173** in your browser.

---

## Why you get `ECONNREFUSED` on the frontend

```
[vite] http proxy error: /api/auth/register
AggregateError [ECONNREFUSED]
```

This means the **backend is not running**. Vite proxies `/api` requests to `http://localhost:8080`, but nothing is listening there yet.

**Fix:** Make sure `./run.sh` (Terminal 1) is fully started before opening the browser. You should see:

```
Started BankingSystemApiApplication in 1.2 seconds
Tomcat started on port 8080
```

---

## Project layout

| Path                | Purpose                              |
| ------------------- | ------------------------------------ |
| `BankingSystemAPI/` | Spring Boot REST API                 |
| `banking-frontend/` | React + Vite UI                      |
| `run.sh`            | Start API with H2 (default)          |
| `run-mysql.sh`      | Start API with MySQL + `.env`        |
| `run-frontend.sh`   | Install deps + start Vite dev server |
| `setup-mysql.sh`    | Apply `schema.sql` to MySQL          |
| `.env`              | MySQL credentials (never committed)  |
| `.env.example`      | Safe placeholder — commit this       |

---

## API profiles

| Profile         | Database               | When to use                            |
| --------------- | ---------------------- | -------------------------------------- |
| `dev` (default) | H2 in-memory           | Fast local dev; data resets on restart |
| `mysql`         | MySQL `banking_system` | Real persistence                       |

```bash
SPRING_PROFILES_ACTIVE=dev   ./run.sh   # H2 (same as just ./run.sh)
SPRING_PROFILES_ACTIVE=mysql ./run.sh   # MySQL (same as ./run-mysql.sh)
```

---

## Required environment variables

Only needed for the MySQL profile. Copy `.env.example` → `.env` and fill in:

```bash
DB_URL=jdbc:mysql://localhost:3306/banking_system
DB_USERNAME=root
DB_PASSWORD=your_mysql_password_here
```

**Never commit `.env`** — it is in `.gitignore`.

---

## Password rules

Passwords must have:

- 8+ characters
- At least one uppercase letter
- At least one number
- At least one special character

Example for local testing: `Demo123!` (do not use in production)

---

## Account types

Use `BASIC` or `SAVING` when opening accounts.

---

## Security overview

| Feature                | Implementation                                       |
| ---------------------- | ---------------------------------------------------- |
| Password storage       | BCrypt (via Spring Security Crypto)                  |
| Authentication         | Stateless session tokens (UUID, in-memory map)       |
| Authorization          | Custom filter — all `/api/*` paths require token     |
| CORS                   | Allows `localhost:5173` only                         |
| Rate limiting          | 10 requests/minute per IP on login & register        |
| Secrets management     | `.env` file (gitignored) + environment variables     |
| H2 console             | Disabled (even in dev)                               |
| Stack traces to client | Disabled via `server.error.include-stacktrace=never` |

---

## Verify the stack

```bash
# Backend compiles
cd BankingSystemAPI && mvn clean compile

# Backend tests
cd BankingSystemAPI && mvn test

# Frontend type-check + build
cd banking-frontend && npm install && npm run build
```

---

## Troubleshooting

| Problem                  | Fix                                                                  |
| ------------------------ | -------------------------------------------------------------------- |
| `ECONNREFUSED` on `/api` | Start `./run.sh` first, wait for "Tomcat started on port 8080"       |
| Port 8080 already in use | `./run.sh` stops the existing listener automatically                 |
| MySQL access denied      | Check `.env` credentials and that the database exists                |
| Unknown column `role`    | Run `./setup-mysql.sh` (includes migration)                          |
| Registration fails       | Use a stronger password (see rules above)                            |
| Username already taken   | The API now returns a specific message — choose a different username |
| Data disappeared         | H2 is in-memory; switch to MySQL profile for persistence             |

---

## Cursor / VS Code

Launch configurations in `.vscode/launch.json`:

- **Banking System API (H2 dev)** — start API with H2
- **Banking System API (MySQL)** — start API with `.env` credentials

---

## Running with Docker

Docker Compose starts MySQL, the Spring Boot API, and the Vite dev server in a single command. No local Java, Maven, or MySQL installation is needed.

### Required software

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (includes Docker Compose v2)

### 1. Create your `.env` file

```bash
cp .env.example .env
```

Open `.env` and set a real password:

```bash
DB_PASSWORD=ChooseAStrongPassword1!
```

`DB_USERNAME` defaults to `root` and `DB_URL` is handled automatically — you do not need to change them for Docker.

### 2. Build and start all services

```bash
docker compose up --build
```

First run takes a few minutes (Maven downloads dependencies, npm installs packages). Subsequent starts reuse cached layers.

| Service  | URL                   |
| -------- | --------------------- |
| Frontend | http://localhost:5173 |
| API      | http://localhost:8080 |
| MySQL    | localhost:3306        |

### 3. Stop the containers

```bash
docker compose down
```

To also delete the MySQL data volume (full reset):

```bash
docker compose down -v
```

### 4. Rebuild after dependency changes

If you add a Maven dependency (`pom.xml` changed) or an npm package (`package.json` changed), Docker needs to rebuild the affected image:

```bash
docker compose up --build backend   # pom.xml changed
docker compose up --build frontend  # package.json changed
docker compose up --build           # rebuild everything
```

Source code changes in the **frontend** (`banking-frontend/src/`) are reflected instantly via Vite's HMR — no rebuild needed.

Source code changes in the **backend** require a rebuild because Java must be compiled:

```bash
docker compose up --build backend
```

### How each service works

| Service    | Image / Build                        | Notes                                                                                           |
| ---------- | ------------------------------------ | ----------------------------------------------------------------------------------------------- |
| `db`       | `mysql:8.4`                          | Schema applied once from `schema.sql` on first start. Data persists in the `mysql_data` volume. |
| `backend`  | Multi-stage Maven → JRE Alpine       | Compiled to a fat JAR. Connects to MySQL via the `db` service name (not `localhost`).           |
| `frontend` | Node 22 Alpine running `npm run dev` | Vite HMR active. Proxies `/api` to `http://backend:8080` via `BACKEND_URL`.                     |

### Why `localhost` behaves differently inside containers

Inside a Docker container, `localhost` (and `127.0.0.1`) refers to **that container's own loopback**, not the host machine or any sibling container. If the frontend proxy pointed to `http://localhost:8080`, it would try to reach the backend _inside the frontend container_, where nothing is listening — causing `ECONNREFUSED`.

Docker Compose puts all services on a shared network and assigns each a DNS name matching its service name. The fix: `vite.config.ts` reads `BACKEND_URL` from the environment and defaults to `http://localhost:8080` for local development. Docker Compose injects `BACKEND_URL=http://backend:8080` so the proxy finds the backend container by name.

### Database setup notes

The MySQL container runs `schema.sql` automatically when the data volume is empty (first start). The tables include `users`, `accounts`, `transactions`, and `logs`. The `migrate-add-role.sql` file is also applied and is safe to re-run.

If you need to re-apply the schema (e.g., after `docker compose down -v`):

```bash
docker compose down -v   # wipe the volume
docker compose up        # schema is re-applied on fresh start
```

### Troubleshooting

| Problem                                | Fix                                                                                                                                              |
| -------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| `ECONNREFUSED` on frontend             | The backend container is still starting. Wait for `Started BankingSystemApiApplication` in logs, then reload.                                    |
| `Access denied for user 'root'`        | Check `DB_PASSWORD` in `.env` matches what MySQL was initialized with. If changed after first start, run `docker compose down -v` and restart.   |
| Port 3306 / 8080 / 5173 already in use | Stop the conflicting process or change the host port in `docker-compose.yml`.                                                                    |
| Frontend changes not reflecting        | Confirm the file is inside `banking-frontend/src/` (the mounted directory). Changes outside `src/` require `docker compose up --build frontend`. |
| Backend changes not reflecting         | Run `docker compose up --build backend`. Java must be recompiled.                                                                                |
| Container keeps restarting             | Run `docker compose logs backend` to see the Spring Boot error. Usually a wrong `DB_PASSWORD` or the database not yet healthy.                   |

---

## Next steps for production readiness

- [ ] Replace in-memory sessions with **JWT** (stateless, survives restarts)
- [ ] Add **role-based authorization** (admin vs user endpoints)
- [ ] Use **Flyway or Liquibase** for schema migrations
- [ ] Add **backend integration tests** (MockMvc)
- [x] Set up **Docker Compose** (app + MySQL)
- [ ] Configure **HTTPS** (TLS termination)
- [ ] Add **GitHub Actions CI** (test + build on push)
- [ ] Set up **audit logging** (who did what, when)
- [ ] Add **transaction search and filtering**
