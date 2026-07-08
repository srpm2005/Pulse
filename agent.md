# Pulse — Stock Dashboard App: Agent Build Plan

## Project Overview

**Pulse** is a full-stack stock tracking dashboard with:
- Email/password authentication (JWT-based)
- Real-time stock widgets powered by Upstox API
- Custom price alerts that trigger email notifications via Resend API
- Spring Boot backend, NeonDB (PostgreSQL) database, vanilla HTML/CSS/JS frontend

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | HTML5, CSS3 (custom, no Tailwind), Vanilla JavaScript |
| Backend | Java 21, Spring Boot 3.x |
| Database | NeonDB (PostgreSQL via JDBC) |
| ORM | Spring Data JPA + Hibernate |
| Auth | JWT (jjwt library) |
| Stock Data | Upstox Market Data API (free tier) |
| Email | Resend API (REST via `RestTemplate` / `WebClient`) |
| Build Tool | Maven |
| Deployment | Local dev (Spring Boot embedded Tomcat) |

---

## Project Structure

```
d:/pulse/
├── backend/                          # Spring Boot Maven project
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/com/pulse/
│           │   ├── PulseApplication.java
│           │   ├── config/
│           │   │   ├── SecurityConfig.java
│           │   │   ├── JwtConfig.java
│           │   │   └── WebConfig.java
│           │   ├── controller/
│           │   │   ├── AuthController.java
│           │   │   ├── StockController.java
│           │   │   └── AlertController.java
│           │   ├── dto/
│           │   │   ├── LoginRequest.java
│           │   │   ├── RegisterRequest.java
│           │   │   ├── AuthResponse.java
│           │   │   ├── StockQuoteDto.java
│           │   │   ├── AlertRequest.java
│           │   │   └── AlertResponse.java
│           │   ├── entity/
│           │   │   ├── User.java
│           │   │   └── PriceAlert.java
│           │   ├── repository/
│           │   │   ├── UserRepository.java
│           │   │   └── PriceAlertRepository.java
│           │   ├── service/
│           │   │   ├── AuthService.java
│           │   │   ├── JwtService.java
│           │   │   ├── UpstoxService.java
│           │   │   ├── AlertService.java
│           │   │   └── EmailService.java
│           │   ├── scheduler/
│           │   │   └── AlertScheduler.java
│           │   └── security/
│           │       ├── JwtAuthFilter.java
│           │       └── UserDetailsServiceImpl.java
│           └── resources/
│               └── application.properties
└── frontend/
    ├── index.html                    # Login page
    ├── dashboard.html                # Main dashboard
    ├── css/
    │   ├── main.css                  # Global styles, variables, reset
    │   ├── login.css                 # Login page styles
    │   └── dashboard.css             # Dashboard layout and widgets
    ├── js/
    │   ├── api.js                    # Centralized API call helpers
    │   ├── auth.js                   # Login / register / logout logic
    │   ├── dashboard.js              # Dashboard bootstrap and state
    │   ├── stockWidget.js            # Stock widget render + refresh
    │   └── alerts.js                 # Price alert CRUD and modal logic
    └── assets/
        └── logo.svg
```

---

## Database Schema (NeonDB / PostgreSQL)

### `users` table
```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);
```

### `price_alerts` table
```sql
CREATE TABLE price_alerts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symbol          VARCHAR(50) NOT NULL,
    company_name    VARCHAR(255),
    target_price    NUMERIC(12, 2) NOT NULL,
    condition       VARCHAR(10) NOT NULL CHECK (condition IN ('ABOVE', 'BELOW')),
    triggered       BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

---

## Backend Implementation Details

### `application.properties`
```properties
spring.datasource.url=jdbc:postgresql://<neon-host>/<db>?sslmode=require
spring.datasource.username=<neon-user>
spring.datasource.password=<neon-password>
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

jwt.secret=<secure-random-base64-256bit-key>
jwt.expiration-ms=86400000

upstox.api.key=<your-upstox-api-key>
upstox.base-url=https://api.upstox.com/v2

resend.api.key=<your-resend-api-key>
resend.from-email=alerts@yourdomain.com

alert.scheduler.cron=0 * * * * *
```

---

### Security & Auth

**`SecurityConfig.java`**
- Disable CSRF (REST API)
- Permit `/api/auth/**` without authentication
- All other endpoints require a valid JWT
- Register `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`
- Configure `PasswordEncoder` bean (`BCryptPasswordEncoder`)
- Configure `AuthenticationManager` bean

**`JwtService.java`**
- `generateToken(String email)` → signs and returns JWT string
- `extractEmail(String token)` → parses subject claim
- `isTokenValid(String token, UserDetails userDetails)` → validates signature and expiry
- Use `io.jsonwebtoken:jjwt-api:0.12.x` + `jjwt-impl` + `jjwt-jackson`

**`JwtAuthFilter.java`** (extends `OncePerRequestFilter`)
- Read `Authorization: Bearer <token>` header
- Validate token via `JwtService`
- Set `UsernamePasswordAuthenticationToken` into `SecurityContextHolder`

**`UserDetailsServiceImpl.java`**
- Implements `UserDetailsService`
- Loads user from `UserRepository` by email

**`AuthController.java`** — endpoints:
- `POST /api/auth/register` — validate uniqueness, hash password, save user, return JWT
- `POST /api/auth/login` — verify credentials, return JWT
- `GET /api/auth/me` — return current user email (protected)

**`AuthService.java`**
- `register(RegisterRequest)` — throws `EmailAlreadyExistsException` if duplicate
- `login(LoginRequest)` — throws `InvalidCredentialsException` on failure
- Both return `AuthResponse { token, email }`

---

### Stock Data (Alpha Vantage)

**`AlphaVantageService.java`**
- Uses `RestTemplate` or `WebClient`
- `getQuote(String symbol)` → calls `GET /query?function=GLOBAL_QUOTE&symbol={key}&apikey={token}`
- `searchSymbol(String query)` → calls `GET /query?function=SYMBOL_SEARCH&keywords={query}&apikey={token}`
- Returns `StockQuoteDto { symbol, companyName, lastPrice, change, changePercent, high, low, volume }`

**`StockController.java`** — endpoints:
- `GET /api/stocks/quote?symbol={symbol}` — returns live quote (protected)
- `GET /api/stocks/search?q={query}` — returns matching instruments (protected)

---

### Price Alerts

**`PriceAlert.java`** entity fields:
- `id`, `user` (ManyToOne → User), `symbol`, `companyName`, `targetPrice`, `condition` (enum `ABOVE`/`BELOW`), `triggered` (boolean), `createdAt`

**`AlertService.java`**
- `createAlert(AlertRequest, String email)` — saves new alert linked to authenticated user
- `getAlerts(String email)` — returns all alerts for user
- `deleteAlert(Long id, String email)` — deletes if owner matches
- `checkAndTriggerAlerts()` — fetched by scheduler; iterates all untriggered alerts, fetches live quote per unique symbol, evaluates condition, marks `triggered = true`, calls `EmailService`

**`AlertController.java`** — endpoints:
- `POST /api/alerts` — create alert
- `GET /api/alerts` — list user's alerts
- `DELETE /api/alerts/{id}` — delete alert

**`AlertScheduler.java`**
- Annotated with `@Scheduled(cron = "${alert.scheduler.cron}")`
- Calls `alertService.checkAndTriggerAlerts()`
- Enable scheduling in main class with `@EnableScheduling`

---

### Email Notifications (Resend)

**`EmailService.java`**
- Uses `RestTemplate` to call Resend REST API: `POST https://api.resend.com/emails`
- Sets `Authorization: Bearer <resend-api-key>` header
- Sends JSON body: `{ "from": "...", "to": ["user@email.com"], "subject": "...", "html": "..." }`
- Method: `sendPriceAlertEmail(String toEmail, String symbol, String companyName, double targetPrice, double currentPrice, String condition)`
- HTML email body: clean, minimal template indicating stock name, current price, and triggered condition

---

### Maven `pom.xml` Dependencies
```xml
<!-- Spring Boot Starters -->
spring-boot-starter-web
spring-boot-starter-data-jpa
spring-boot-starter-security
spring-boot-starter-validation

<!-- Database -->
postgresql (runtime)

<!-- JWT -->
io.jsonwebtoken:jjwt-api:0.12.6
io.jsonwebtoken:jjwt-impl:0.12.6 (runtime)
io.jsonwebtoken:jjwt-jackson:0.12.6 (runtime)

<!-- Utility -->
org.projectlombok:lombok
```

---

## Frontend Implementation Details

### Design System (`css/main.css`)
- CSS custom properties (variables):
  - `--bg-primary: #0d0f14` (deep dark background)
  - `--bg-card: #161a24` (card surface)
  - `--bg-card-hover: #1e2433`
  - `--accent: #6366f1` (indigo)
  - `--accent-hover: #4f46e5`
  - `--success: #22c55e`
  - `--danger: #ef4444`
  - `--text-primary: #f1f5f9`
  - `--text-secondary: #94a3b8`
  - `--border: #2a2f3e`
  - `--radius: 12px`
  - `--shadow: 0 4px 24px rgba(0,0,0,0.4)`
- Google Font: `Inter` (weights 300, 400, 500, 600, 700)
- Global reset: box-sizing border-box, margin/padding 0
- Smooth transitions on interactive elements (200ms ease)
- Scrollbar styling for dark theme

### Login Page (`index.html` + `css/login.css`)
- Full-screen centered layout, dark background
- Card with glass-morphism effect: `backdrop-filter: blur(10px)`, semi-transparent border
- Fields: Email, Password
- Buttons: Login, Register (toggle between forms inline)
- Animated gradient accent line at top of card
- Error message div (hidden by default, shown on failure)
- On success: store JWT in `localStorage`, redirect to `dashboard.html`

### Dashboard Page (`dashboard.html` + `css/dashboard.css`)
**Layout:**
- Fixed sidebar (240px wide): Logo, nav links (Dashboard, Alerts), user email, Logout button
- Main content area: header bar + scrollable content

**Stock Widget Section:**
- "Add Stock" button → opens a search modal
- Search modal: debounced input (300ms) → calls `/api/stocks/search` → displays results list → user clicks to add
- Added stocks rendered as cards in a CSS grid (auto-fill, min 260px)
- Each stock card shows:
  - Company name + symbol badge
  - Last price (large, prominent)
  - Change and % change (green if positive, red if negative, with up/down arrow icon)
  - High / Low chips
  - "Set Alert" button + "Remove" icon button
  - Auto-refresh every 30 seconds via `setInterval`

**Alerts Section:**
- Table listing all user alerts: Symbol, Company, Condition (ABOVE/BELOW), Target Price, Status (Pending/Triggered badge)
- "Set Alert" modal (triggered from stock card or nav):
  - Dropdown: symbol (pre-filled if opened from card)
  - Input: target price
  - Radio: ABOVE / BELOW
  - Submit → calls `POST /api/alerts`
- Delete button per row → calls `DELETE /api/alerts/{id}`

### JavaScript Modules

**`api.js`**
- `BASE_URL = 'http://localhost:8080'`
- `getToken()` → reads JWT from `localStorage`
- `authHeaders()` → returns `{ Authorization: 'Bearer <token>', 'Content-Type': 'application/json' }`
- `apiFetch(path, options)` → wrapper around `fetch`, auto-attaches auth header, handles 401 by redirecting to login

**`auth.js`**
- `login(email, password)` → calls `POST /api/auth/login`, stores token, redirects
- `register(email, password)` → calls `POST /api/auth/register`, stores token, redirects
- `logout()` → clears `localStorage`, redirects to `index.html`
- On page load: if JWT present → redirect to dashboard (login page guard)

**`dashboard.js`**
- On page load: verifies JWT (calls `GET /api/auth/me`), redirects to login if expired
- Renders user email in sidebar
- Bootstraps `stockWidget.js` and `alerts.js`
- Manages navigation between "Dashboard" and "Alerts" sections (single-page feel, show/hide divs)

**`stockWidget.js`**
- `trackedStocks` — in-memory array of `{ instrumentKey, symbol, companyName }`
- Persisted to `localStorage` under key `pulse_tracked_stocks`
- `addStock(instrument)` — adds to array, saves, renders card, starts refresh
- `removeStock(instrumentKey)` — removes from array, saves, removes card from DOM
- `fetchAndRenderQuote(instrumentKey)` — calls `/api/stocks/quote`, updates card DOM
- `startAutoRefresh()` — `setInterval` every 30s calling `fetchAndRenderQuote` for all tracked stocks
- `renderStockCard(instrument)` — creates and inserts card DOM element
- `updateCardUI(instrumentKey, quoteDto)` — updates price, change, colors in existing card

**`alerts.js`**
- `loadAlerts()` — `GET /api/alerts`, renders table rows
- `createAlert(payload)` — `POST /api/alerts`, reloads table
- `deleteAlert(id)` — `DELETE /api/alerts/{id}`, reloads table
- Modal open/close logic
- Form validation before submit

---

## API Contract Summary

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Register new user |
| POST | `/api/auth/login` | None | Login, get JWT |
| GET | `/api/auth/me` | JWT | Get current user email |
| GET | `/api/stocks/quote?symbol=` | JWT | Get live stock quote |
| GET | `/api/stocks/search?q=` | JWT | Search instruments |
| GET | `/api/alerts` | JWT | List user's alerts |
| POST | `/api/alerts` | JWT | Create price alert |
| DELETE | `/api/alerts/{id}` | JWT | Delete alert |

---

## Upstox API Usage Notes

- **Authentication:** Upstox free tier uses an API key passed as `Authorization: Bearer <api-key>`
- **Quote endpoint:** `GET https://api.upstox.com/v2/market-quote/quotes?instrument_key=NSE_EQ|<ISIN_or_ticker>`
- **Search endpoint:** `GET https://api.upstox.com/v2/charges/brokerage` — for instrument lookup, use `GET https://api.upstox.com/v2/search/instruments?query={q}`
- Map response field `last_price` → `lastPrice`, `ohlc.high` → `high`, `ohlc.low` → `low`, `net_change` → `change`
- Wrap Upstox calls in a service layer; all field mappings centralized in `UpstoxService`

---

## Resend API Usage Notes

- Register at resend.com, verify a sending domain (or use onboarding@resend.dev for testing)
- REST call: `POST https://api.resend.com/emails` with JSON body and `Authorization: Bearer <key>` header
- Email HTML template should be inline-styled for email client compatibility
- Template variables: `{{symbol}}`, `{{companyName}}`, `{{condition}}`, `{{targetPrice}}`, `{{currentPrice}}`

---

## Code Quality Rules (Mandatory)

1. **No useless comments** — no `// getter`, `// constructor`, `// TODO: implement`, or obvious inline comments
2. **No commented-out code** — remove dead code entirely
3. **DTOs over entities in controllers** — never expose JPA entities directly in REST responses
4. **Single responsibility** — each class/service does one thing
5. **No magic strings** — use constants or config properties
6. **Validation** — use `@Valid`, `@NotBlank`, `@Email` on DTOs; handle `MethodArgumentNotValidException` globally
7. **Global exception handler** — `@ControllerAdvice` class returning structured `{ error, message, status }` JSON
8. **Environment variables via `application.properties`** — never hardcode secrets in source files
9. **CSS only in .css files** — no inline styles in HTML (except dynamically set by JS)
10. **JS: no inline event handlers** — all listeners attached via `addEventListener` in JS files
11. **Consistent naming:** Java → camelCase methods, PascalCase classes; JS → camelCase; CSS → kebab-case; DB → snake_case

---

## Implementation Order for the Agent

1. **Backend first:**
   a. Scaffold Maven project with correct `pom.xml`
   b. Write `application.properties` (placeholders for secrets)
   c. Entities → Repositories → DTOs → Services → Controllers
   d. Auth layer (Security config, JWT filter, UserDetailsService)
   e. Upstox service + Stock endpoints
   f. Alert entity, service, controller
   g. Email service (Resend)
   h. Alert scheduler

2. **Frontend second:**
   a. `css/main.css` design tokens and global styles
   b. `index.html` + `css/login.css` + `js/auth.js`
   c. `dashboard.html` skeleton + sidebar + `css/dashboard.css`
   d. `js/api.js` utility
   e. `js/stockWidget.js` + stock card component
   f. Search modal
   g. `js/alerts.js` + alerts table + alert modal
   h. `js/dashboard.js` bootstrap and navigation

3. **Integration & polish:**
   a. CORS config in Spring Boot (allow frontend origin)
   b. Test auth flow end-to-end
   c. Test stock search → add widget → auto-refresh cycle
   d. Test alert creation → scheduler trigger → email received
   e. Error state UI (loading spinners, empty states, error toasts)

---

## Environment Setup Checklist

- [ ] Java 21 installed, `JAVA_HOME` set
- [ ] Maven installed
- [ ] NeonDB project created, connection string obtained
- [ ] Upstox developer account created, API key obtained
- [ ] Resend account created, API key obtained, sending domain verified
- [ ] Fill in `application.properties` with real values before running backend
- [ ] Run backend: `mvn spring-boot:run` from `backend/`
- [ ] Open `frontend/index.html` in browser (or serve with Live Server)

---

## Error Handling Patterns

**Backend:**
- `GlobalExceptionHandler.java` (`@ControllerAdvice`):
  - `handleValidationErrors` → 400 with field errors map
  - `handleEmailAlreadyExists` → 409
  - `handleInvalidCredentials` → 401
  - `handleAccessDenied` → 403
  - `handleGeneric` → 500 with sanitized message

**Frontend:**
- `apiFetch` wrapper catches non-2xx responses, parses `{ message }` from body, throws descriptive error
- All async calls wrapped in try/catch; errors shown in a toast notification (top-right, auto-dismiss 4s)
- Loading state: spinner overlay on stock cards while fetching, disabled buttons during form submit
