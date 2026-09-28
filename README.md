# Pulse — Financial & Stock Dashboard

[![Live Demo](https://img.shields.io/badge/Live_Demo-Vercel-black?style=for-the-badge&logo=vercel)](https://pulse-peach-delta.vercel.app/)
[![Backend](https://img.shields.io/badge/Backend-Render-46E3B7?style=for-the-badge&logo=render&logoColor=white)](https://pulse-015c.onrender.com)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React_19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://react.dev)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Neon_DB-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://neon.tech)

> **Live Application:** [https://pulse-peach-delta.vercel.app/](https://pulse-peach-delta.vercel.app/)  
> **Backend API:** [https://pulse-015c.onrender.com](https://pulse-015c.onrender.com)

Pulse is a modern, full-stack real-time financial tracking dashboard featuring a high-density "Terminal-Chic" user interface, interactive portfolio management, auto-updating Chart.js sparklines, stateless JWT authentication, password recovery via 6-digit OTP codes, and an automated background cron scheduler for price alert notifications.

<img width="1917" height="1022" alt="Pulse Dashboard Screenshot" src="https://github.com/user-attachments/assets/21a43282-4eaf-4ce3-afc3-62269802f2b1" />

---

## 🚀 Key Features

- **Terminal-Chic UI:** A heavily optimized, grid-based interface enforcing high data density, true `#000` canvas backdrops, 1px `#222` slate borders, and dynamic SVG vectors. Built natively in React + Vite without bloated component libraries.
- **Interactive Portfolio & Watchlist:** Live market data integration with Yahoo Finance. Supports US Equities (NASDAQ/NYSE), Indian stocks (NSE/BSE), ETFs, and more. Tracked portfolios are securely persisted in PostgreSQL.
- **Auto-Updating Chart.js Sparklines:** Minimalist time-series trendlines on HTML5 canvas with dynamic color-coding (Emerald Green for gains, Crimson Red for dips) and in-place dataset updates.
- **Automated Price Alerts:** Set dynamic `ABOVE` or `BELOW` target thresholds. A background cron scheduler silently evaluates alerts during active market hours and automatically dispatches styled HTML emails via Gmail SMTP.
- **Forgot Password & 6-Digit OTP Reset:** Secure password recovery flow issuing time-limited (15-minute) 6-digit cryptographic verification codes delivered directly to user inboxes.
- **Cloud-Optimized Backend:** The Spring Boot backend is tuned for low-memory container environments using `-Xmx256m` caps, SerialGC, Hikari connection pooling, and non-blocking security random generators.
- **RAG Quantitative Research Assistant:** Institutional-grade quantitative analysis engine computing SMA-20/50, 14-day RSI momentum, 52-week envelope position, and 30-day realized annualized volatility. Grounded retrieval eliminates LLM hallucinations by binding prompts strictly to verifiable Yahoo Finance telemetry and database alerts with interactive citations (`[DOC-n]`), live-streamed over Server-Sent Events (SSE).
- **Secure Authentication:** Stateless JSON Web Token (JWT) architecture backed by Spring Security and BCrypt password hashing.

---

## 🛠 Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Frontend** | React 19, Vite 8, Chart.js, Vanilla CSS Design System |
| **Backend** | Java 17, Spring Boot 3.2.4 (Security, Data JPA, Mail, Scheduling) |
| **Database** | Serverless PostgreSQL (Neon DB) with Flyway Migrations (`V1`–`V4`) |
| **Deployment** | Vercel (Frontend SPA) + Render (Docker / Spring Boot Container) |
| **External APIs** | Yahoo Finance (Market Data) + Gmail SMTP (STARTTLS on port 587) |

---

## ⚙️ Local Setup

### 1. Prerequisites
- **Java 17+** and **Maven** (`mvn`)
- **Node.js 18+** and **npm**
- A **PostgreSQL** instance (e.g., [Neon DB](https://neon.tech))
- A **Google App Password** for SMTP email delivery

### 2. Backend Configuration
Navigate to the `backend` directory and create `.env`:
```env
# SMTP Alerting Configuration
GMAIL_USER=your_email@gmail.com
GMAIL_APP_PASSWORD=your_16_digit_app_password

# Database Configuration
NEON_HOST=ep-rough-breeze-aoyyhe9m.c-2.ap-southeast-1.aws.neon.tech
NEON_USER=neondb_owner
NEON_PASSWORD=your_db_password
NEON_DB=neondb

# Security (Base64 256-bit secret key)
JWT_SECRET=VnJ1blNlY3JldEtleU11c3RCZUF0TGVhc3QyNTZCaXRMb25nMTIzNDU2==
```

### 3. Run the Backend
Flyway will automatically execute SQL migrations (`V1` to `V4`) during startup:
```bash
cd backend
mvn spring-boot:run
```
*The backend will launch on `http://localhost:8080`.*

### 4. Run the React Frontend
In a new terminal window:
```bash
cd frontend
npm install
npm run dev
```
*The frontend will launch on `http://localhost:5173`.*

---

## 📡 API Reference

All protected endpoints require a standard Bearer token in the `Authorization` request header: `Bearer <jwt_token>`.

### 🔐 Authentication & Account (`/api/auth`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `POST` | `/api/auth/register` | Register a new user account | No |
| `POST` | `/api/auth/login` | Authenticate user and receive JWT access token | No |
| `POST` | `/api/auth/forgot-password` | Request a 15-minute 6-digit OTP verification email | No |
| `POST` | `/api/auth/reset-password` | Validate OTP code and update account password | No |
| `GET` | `/api/auth/me` | Retrieve profile information for authenticated user | Yes |
| `GET` | `/api/auth/health` | Health check endpoint for uptime and container monitoring | No |

### 📈 Market & Stock Data (`/api/stocks`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/stocks/quote?symbol={symbol}` | Fetch real-time market quote, change %, and volume | No |
| `GET` | `/api/stocks/search?q={query}` | Autocomplete search for equities and ticker symbols | No |
| `GET` | `/api/stocks/chart?symbol={symbol}&range={range}` | Fetch historical time-series data for sparkline charts | No |

### 💼 Portfolio & Watchlist (`/api/stocks/tracked`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/stocks/tracked` | Fetch all tracked portfolio symbols for the current user | Yes |
| `POST` | `/api/stocks/tracked` | Add a stock symbol to the user's tracked portfolio | Yes |
| `DELETE` | `/api/stocks/tracked/{symbol}` | Remove a stock symbol from the tracked portfolio | Yes |

### 🔔 Price Alerts (`/api/alerts`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/alerts` | List all active price threshold alerts for the user | Yes |
| `POST` | `/api/alerts` | Create a new target price alert (`ABOVE` / `BELOW` threshold) | Yes |
| `DELETE` | `/api/alerts/{id}` | Delete a configured price alert by ID | Yes |

### ⚡ Quantitative Research Assistant & RAG (`/api/research`)

| Method | Endpoint | Description | Auth Required |
| :--- | :--- | :--- | :---: |
| `GET` | `/api/research/stream?query={q}&symbols={s}` | Server-Sent Events (SSE) live streaming research report with grounded facts and citations | Yes |
| `POST` | `/api/research/query` | Synchronous grounded quantitative analysis report | Yes |
| `GET` | `/api/research/presets` | Quick-start query presets for automated research | No |

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
