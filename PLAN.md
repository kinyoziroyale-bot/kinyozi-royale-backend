# Kinyozi Royale — Backend Plan & Handover

This document is a complete brief any developer (or AI agent) can follow to
rebuild, run, or extend the Kinyozi Royale backend from scratch. It mirrors
every decision we have made over the project's lifetime.

## 1. Product

Kinyozi Royale is a **multi-tenant salon & spa management** system. Each
business that registers becomes its own tenant; all data is strictly scoped
by `tenant_id`. Owners log in with username + password and receive a JWT.

## 2. Tech stack

- **Java 17** (JDK 17) — required
- **Spring Boot 3.3.4** (`spring-boot-starter-parent`)
- **Maven** build
- **PostgreSQL** (any 13+)
- **Spring Data JPA / Hibernate** with `ddl-auto=update`
- **Spring Security** + **JJWT 0.12.6** for stateless JWT auth
- **BCrypt** for password hashing
- **Lombok 1.18.34** (must match JDK 17)

## 3. Project layout (package-per-feature, IntelliJ friendly)

```
src/main/java/com/kinyozi/royale/
  KinyoziRoyaleApplication.java
  config/        # Jackson, etc.
  controller/    # one REST controller per resource
  dto/           # request/response records
  exception/     # custom + GlobalExceptionHandler
  model/         # @Entity classes (one per file)
  repository/    # JpaRepository interfaces (one per entity)
  security/      # JwtUtil, JwtFilter, SecurityConfig, CurrentUser
  service/       # AuthService, StockMovementService (business logic)
src/main/resources/
  application.properties
schema.sql       # optional: bootstrap a fresh DB without Hibernate
pom.xml
```

This layout matches typical Spring Boot conventions (compare with the
`com.school.erp` ERP project: `config / controller / dto / exception /
model / repository / security / service`).

## 4. Environment variables (production)

| Var | Default | Notes |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/kinyozi` | JDBC URL |
| `DB_USER` | `postgres` | |
| `DB_PASSWORD` | `root` | |
| `PORT` | `8080` | server port |
| `JWT_SECRET` | dev placeholder | **must be ≥ 32 chars** in prod |
| `JWT_EXPIRATION_MS` | `86400000` | 24 h |
| `CORS_ORIGINS` | `http://localhost:5173,...` | comma-separated |

Context path is `/api` — every endpoint below is implicitly prefixed.

## 5. Domain model

Tenant-scoped entities (all carry `tenant_id UUID`):

- `Tenant` — the salon/shop (business name, owner, phone)
- `User` — login account; `role` ∈ {OWNER, MANAGER, WORKER}; belongs to one tenant
- `Worker` — staff member who performs services
- `ServiceItem` — service offered (name, price, duration)
- `InventoryItem` — consumable stock (name, currentQty, thresholdQty, unit)
- `Customer` — client of the salon
- `Ticket` — POS ticket linking customer + worker + service + amount + status
- `StockMovement` — audit log of stock changes (who took what, when, why)

Tenant isolation rule: every read filters by `CurrentUser.tenantId()` and
every write stamps `tenant_id` from the JWT. Cross-tenant access returns 404.

## 6. Authentication flow

1. `POST /api/auth/register` with `{ businessName, ownerName, phone, username, password, email }`
   - creates a `Tenant` and an `OWNER` `User`
   - returns `{ token, tenantId, username, businessName, role }`
2. `POST /api/auth/login` with `{ username, password }` → same response.
3. Frontend stores the token + tenantId in `localStorage` and attaches
   `Authorization: Bearer <token>` on every request.
4. `JwtFilter` parses the token, sets the Spring Security principal, and
   stashes the tenant id in `auth.details` so `CurrentUser.tenantId()` works.
5. Everything except `/auth/**` and `/health` is authenticated.

## 7. REST endpoints (all prefixed with `/api`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Create tenant + owner |
| POST | `/auth/login` | Get JWT |
| GET | `/health` | Liveness |
| `*` | `/workers` | CRUD |
| `*` | `/services` | CRUD |
| `*` | `/inventory` | CRUD (add product = POST here) |
| `*` | `/customers` | CRUD |
| `*` | `/tickets` | CRUD (POS) |
| GET/POST | `/stock-movements` | List/record usage or restock; updates inventory atomically |

`*` = GET (list), GET `/{id}`, POST, PUT `/{id}`, DELETE `/{id}`.

## 8. Stock movements (inventory audit)

`POST /api/stock-movements` body:
```json
{ "itemId": "uuid", "workerId": "uuid|null", "quantity": 50,
  "reason": "USAGE|RESTOCK|ADJUSTMENT", "note": "optional" }
```
Quantity is sent positive; the service signs it based on `reason`
(`RESTOCK` → +, else −), clamps `currentQty` at 0, and saves both the
inventory update and the movement row in one transaction.

## 9. Frontend contract (already implemented)

The React frontend in this workspace already:
- has `/login` and `/register` pages backed by `/auth/*`
- gates `/dashboard`, `/pos`, `/services`, `/inventory`, `/reports/daily`
  behind `RequireAuth` (redirects to `/login`)
- attaches `Authorization` + `X-Tenant-Id` headers via `src/lib/api.ts`
- has an "Add product" dialog and a "Record usage" dialog on `/inventory`
- shows "Recent stock movements" history

`VITE_API_BASE_URL` should point to `http://localhost:8080/api` in dev.

## 10. Run locally

```bash
# 1. Database
createdb kinyozi
psql -d kinyozi -f schema.sql   # optional; Hibernate also auto-updates

# 2. Backend (needs JDK 17)
export DB_URL=jdbc:postgresql://localhost:5432/kinyozi
export DB_USER=postgres
export DB_PASSWORD=root
export JWT_SECRET=please-change-this-to-32-plus-chars-secret
./mvnw spring-boot:run
# → http://localhost:8080/api/health
```

## 11. How to extend (recipe)

Adding a new tenant-scoped resource `Foo`:
1. `model/Foo.java` — `@Entity` with `tenantId` UUID + fields
2. `repository/FooRepository.java` — extend `JpaRepository<Foo,UUID>` with `findByTenantId`
3. `controller/FooController.java` — copy any existing CRUD controller (e.g. `WorkerController`); change `${Name}` to `Foo` and path
4. (If business logic needed) `service/FooService.java`
5. Restart — Hibernate creates the new table automatically

## 12. Why this layout

- One class per file → small diffs, easy code review
- Package-per-feature → matches the screenshot of `com.school.erp`
- DTOs separated from entities → safe to evolve API without breaking JPA
- `CurrentUser` helper centralises tenant extraction so no controller can leak data
- No seed/dummy data — DB starts empty; everything comes through `/auth/register`

## 13. Change log (what we built)

- v1: initial scaffold (single file, dummy data)
- v2: split into entities/repos/controllers
- v3 (JDK 17): downgraded Spring Boot to 3.3.4 + Lombok 1.18.34 to fix
  `java.lang.ExceptionInInitializerError` on JDK 17/25 mismatch
- v4: persistent auth — `Tenant`, `User`, JWT, `/auth/register` + `/auth/login`,
  CORS, BCrypt, `RequireAuth` on the frontend, inventory `StockMovement`
- v5 (this version): re-organised into clean package-per-feature layout
  (`config / controller / dto / exception / model / repository / security /
  service`), one class per file, ready to drop into IntelliJ.
