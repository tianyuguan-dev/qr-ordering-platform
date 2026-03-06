## QR Ordering Platform

[![CI](https://github.com/tianyuguan-dev/qr-ordering-platform/actions/workflows/ci.yml/badge.svg)](https://github.com/tianyuguan-dev/qr-ordering-platform/actions/workflows/ci.yml)

Multi-tenant QR code ordering platform for restaurants, built as a **recruitment-ready showcase** project.

It demonstrates:

- **Backend**: Spring Boot 3, PostgreSQL, Redis, MinIO, Flyway
- **Architecture**: Outbox pattern + Redis event bus, SSE real-time updates, role-based access control
- **Observability**: Actuator + Prometheus + Grafana dashboard, structured logs with trace/tenant/order IDs
- **Frontend**: Vue 3 + Vite, role-specific dashboards, real-time toasts for new/ready orders
- **Testing**: JUnit + Mockito, Vitest + Vue Test Utils

---

## 1. High-level architecture

Backend (`/backend`):

- Spring Boot 3 REST API (`/api`)
- Multi-tenant by `tenantId` (restaurant id) on all domain entities
- Outbox table (`outbox_events`) + scheduled processor → publish domain events to Redis
- SSE controller streams order events to logged-in staff (waiter/kitchen/admin)
- Metrics via Actuator Prometheus endpoint

Frontend (`/frontend`):

- Vue 3 SPA with role-based layout:
  - **Platform admin**: manage restaurants, platform admins
  - **Restaurant admin**: manage staff, menu, tables, orders
  - **Waiter / Kitchen**: order list with status transitions and real-time alerts
- Global SSE composable so any page can react to order events

Infra / monitoring:

- PostgreSQL, Redis, MinIO via `docker-compose.dev.yml`
- Prometheus + Grafana containers with pre-provisioned:
  - Prometheus datasource
  - “QR Ordering Platform” dashboard JSON

---

## 2. Quick start (Docker, one command)

Requires only Docker + Docker Compose — no JDK or Node.js needed locally.

```bash
docker-compose up -d --build
```

| Service | URL |
|---|---|
| Frontend (Vue SPA) | http://localhost |
| Backend API | http://localhost/api |
| Grafana | http://localhost:3001 (admin / admin) |
| Prometheus | http://localhost:9090 |
| MinIO console | http://localhost:9001 (minioadmin / minioadmin) |

First startup takes a few minutes (Maven + npm builds run inside Docker). Subsequent starts are fast.

---

## 3. Local development (hot-reload)

### 3.1 Prerequisites

- JDK **17+**
- Node.js **18+**
- Docker + Docker Compose

### 3.2 Start infrastructure + monitoring

From project root:

```bash
docker-compose -f docker-compose.dev.yml up -d
```

Services:

- PostgreSQL: `localhost:5433` (db: `qr_ordering`, user: `admin`, password: `password`)
- Redis: `localhost:6379`
- MinIO: `http://localhost:9000` (console `http://localhost:9001`, `minioadmin` / `minioadmin`)
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3001` (user `admin`, password `admin`)

### 3.3 Run backend

```bash
cd backend
mvn spring-boot:run
```

Backend:

- Base URL: `http://localhost:8080/api`
- Actuator Prometheus endpoint: `http://localhost:8080/api/actuator/prometheus`

### 3.4 Run frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server:

- Vite dev: `http://localhost:5173`
- Proxies `/api` → `http://localhost:8080`

---

## 4. Features (for recruiters)

### 4.1 Domain & roles

- Restaurants, tables, menu categories/items
- Orders with items and state machine:
  - CREATED → CONFIRMED → PREPARING → READY → SERVED → COMPLETED
  - CANCELLED as terminal branch
- Roles:
  - PLATFORM_ADMIN
  - RESTAURANT_ADMIN
  - WAITER
  - KITCHEN

### 4.2 Real-time order flow

- **Outbox pattern**:
  - Business code writes rows to `outbox_events` inside DB transaction
  - `OutboxProcessor` (scheduled every 10s) reads NEW events, publishes to Redis channels
  - `OrderEventConsumer` consumes Redis messages and routes to SSE clients
- **SSE**:
  - `SseController` exposes `/sse/subscribe/{restaurantId}` (or `me`)
  - JWT can be in header or `token` query param (EventSource-friendly)
  - Per-restaurant fan-out via `SseEmitterManager`
- **Frontend SSE composable**:
  - Single `EventSource` for the whole app (`useOrderEvents`)
  - Exposes `lastOrderEvent` and `setRestaurantIdForSse`
  - Layout injects this into child views, so **any page** can react to order events

### 4.3 UX details (Waiter / Kitchen)

- Waiter / Restaurant admin:
  - Toast when **new order created** (“New order needs confirmation”)
  - Toast when order **READY** (“Order ready, please serve”)
- Kitchen:
  - Toast when order **CONFIRMED** (“Order confirmed, please prepare”)
- Orders list:
  - Multi-select status filter with “Select all” / “Clear”
  - Filter persisted to `localStorage` under a **per-user key** (`ordersFilterStatuses_{userId}`) so different staff keep independent preferences
- **Multi-tab safe**: auth token and user session stored in `sessionStorage` — opening multiple tabs with different roles (waiter + kitchen) doesn't overwrite each other's session

---

## 5. Observability & monitoring

- **Metrics** (`MetricsService`):
  - `orders.created`
  - `orders.status.changed`
  - `outbox.backlog`
  - `outbox.dead`
  - `outbox.publish.failed`
  - `sse.connections`
- **Health**:
  - Custom `OutboxHealthIndicator` on `/api/actuator/health`
  - Includes backlog and dead-letter counts
- **Logging**:
  - `MdcFilter` adds `traceId` for each request
  - Business code enriches MDC with `tenantId` and `orderId`
  - `logback-spring.xml` outputs structured, MDC-enriched logs
- **Grafana dashboard**:
  - Provisioned via `docker/grafana/provisioning`
  - Panels for order rates, outbox backlog, dead letters, SSE connections, etc.

---

## 6. Testing

### 6.1 Backend (JUnit + Spring Test)

Run:

```bash
cd backend
mvn test
```

Tests are split into three layers:

**Pure unit tests** (no Spring context, run on all JDKs):

- `OrderStatusTest` — `fromCode` / `tryFromCode` behaviour (valid, null, invalid)
- `OrderStateMachineTest` — valid and invalid transitions, null handling
- `UserRoleTest` — `fromCode`, `fromName`, round-trip for all roles
- `JwtServiceTest` — generate + validate + buildPrincipal round-trip; tampered/expired token errors
- `AuthConverterTest` — `toLoginResponse` for all four role codes

**Mocked unit tests** (Mockito; **skipped on JDK 25+** due to ByteBuddy incompatibility):

- `OrderServiceTest` — `list` with multi-status filters and invalid codes
- `OrderControllerTest` — REST contract, `status=1&status=4` passed as list
- `JwtPrincipalConverterTest` — `toClaimValues` / `toPrincipal` for both user types
- `RestaurantServiceTest` — full CRUD routing (findAll / findByStatus / findByName combos, duplicate name, not found)

**Integration tests** (Testcontainers, real PostgreSQL 15 + Redis 7; skipped when Docker unavailable locally, **runs in CI**):

- `RestaurantServiceIntegrationTest` — Flyway migration, JPA AttributeConverter (status as INTEGER), duplicate-name constraint, pagination filters, full CRUD round-trip

### 6.2 Frontend (Vitest + Vue Test Utils)

Run:

```bash
cd frontend
npm run test:run
```

**API layer** (`src/api/`):

- `orders.test.js` — URL building for `getOrders` (multi-status, pagination); payload and HTTP method for `updateOrderStatus`
- `auth.test.js` — `login` (POST body), `getProfile`, `changePassword`
- `restaurants.test.js` — URL building with filters; CRUD methods (create, update, delete)
- `menu.test.js` — categories CRUD, menu items with status/category filters, `updateMenuItemStatus`
- `tables.test.js` — tables CRUD, checkout summary and checkout call

**Composables**:

- `useOrderEvents.test.js` — composable shape (`lastOrderEvent`, `setRestaurantIdForSse`); no real SSE opened in tests

**Components**:

- `LoginView.test.js` — renders all inputs; validation (empty username/password); successful login stores token in sessionStorage and navigates; 401/network error display; button re-enabled after failure

---

## 7. Project structure

```text
backend/
  src/main/java/com/qrordering/...
    auth/          # Auth, security, JWT, user roles (platform admin + restaurant staff)
    restaurant/    # Restaurant CRUD and tenant management
    menu/          # Menu categories & items
    table/         # Tables, QR code generation, checkout
    order/         # Orders, state machine, idempotency, services, controllers
    publicapi/     # Public (unauthenticated) customer API for QR-scan ordering
    event/         # Outbox entities, repositories, processor, Redis publisher/subscriber
    sse/           # SSE controller and emitter manager
    storage/       # MinIO image upload (logo, dish photos)
    observability/ # Metrics, health indicator, MDC filter
    common/        # Shared exceptions, DTOs, global exception handler
    config/        # Web, filters, properties
  src/main/resources/
    application.yml
    db/migration/   # Flyway migrations
    logback-spring.xml

frontend/
  src/
    api/            # HTTP clients (orders, restaurants, auth, etc.)
    composables/    # `useOrderEvents`
    layouts/        # Dashboard layout with role-based navigation
    views/          # Screens (Orders, Restaurants, Staff, etc.)
    router/         # Vue Router

docker/
  prometheus/prometheus.yml
  grafana/provisioning/...
```

---

## 8. Demo script (for interviews)

### Setup (< 2 min)

**Option A — Docker only (recommended for demos):**
```bash
docker-compose up -d --build
# Frontend: http://localhost  |  Grafana: http://localhost:3001
```

**Option B — local dev (hot-reload):**
```bash
docker-compose -f docker-compose.dev.yml up -d   # infra + monitoring
cd backend && mvn spring-boot:run &               # http://localhost:8080
cd frontend && npm run dev                        # http://localhost:5173
```

### Step 1 — Platform admin flow

1. Open `http://localhost:5173` → Login with **Tenant ID: `PLATFORM`**, username `admin`, password `admin123`
2. Go to **Restaurants** → Create a restaurant (e.g. "Demo Ramen")
3. Go to **Platform Users** → Create a restaurant admin:
   - Tenant ID: the restaurant ID shown in the list (e.g. `REST_…`)
   - Role: `RESTAURANT_ADMIN`, username `owner`, password `owner123`

### Step 2 — Restaurant admin flow

4. Open a **new browser tab** (sessionStorage isolates sessions per tab)
5. Login with **Tenant ID: `REST_…`**, username `owner`, password `owner123`
6. Go to **Menu** → add a category and at least two items
7. Go to **Tables** → create a table (QR code auto-generated)
8. Go to **Staff** → create a waiter (`waiter` / `waiter123`) and kitchen staff (`kitchen` / `kitchen123`)

### Step 3 — Multi-role real-time demo

9. Open a **third tab** → Login as waiter (`REST_…` / `waiter` / `waiter123`)
10. Open a **fourth tab** → Login as kitchen staff (`REST_…` / `kitchen` / `kitchen123`)
11. In the **restaurant admin tab**, go to **Tables** → copy the QR URL for your table and open it in an incognito window (customer view)
12. Place an order in the customer view. Observe:
    - **Waiter tab**: toast "New order needs confirmation" appears in real time
    - Waiter clicks **Confirm** → **Kitchen tab**: toast "Order confirmed, please prepare"
    - Kitchen clicks **Ready** → **Waiter tab**: toast "Order ready, please serve"

> The four tabs use separate sessionStorage, so each role stays logged in independently — no need for separate browsers.

### Step 4 — Observability

13. Open Grafana at `http://localhost:3001` (admin / admin) → **QR Ordering Platform** dashboard
14. Place a few more orders and show panels reacting in real time:
    - Order creation rate, status-change events
    - Outbox backlog and dead-letter counters
    - Active SSE connections

