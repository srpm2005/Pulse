# Pulse — Stock Dashboard

Pulse is a modern, full-stack real-time stock tracking dashboard featuring a high-density "Terminal-Chic" user interface, stateless JWT authentication, and an automated background cron-scheduler for email price alerts.

<img width="1917" height="1022" alt="image" src="https://github.com/user-attachments/assets/21a43282-4eaf-4ce3-afc3-62269802f2b1" />

## 🚀 Features

- **Terminal-Chic UI:** A heavily optimized, grid-based interface enforcing high data density, true `#000` canvas backdrops, 1px `#222` slate borders, and dynamic Lucide SVG vectors. Built natively in React + Vite without bloated component libraries.
- **Global Stock Tracking:** Live market data scraping natively hooked into Yahoo Finance. Supports US Equities, Indian NSE/BSE stocks, ETFs, and more. Tracked portfolios are securely synced to the database.
- **Automated Price Alerts:** Set dynamic `ABOVE` or `BELOW` target thresholds. A background cron-scheduler silently polls the market every 5 minutes (localized to Indian Market Hours) and automatically triggers emails directly to your inbox via Gmail SMTP.
- **Cloud-Optimized Backend:** The Spring Boot backend is aggressively tuned for Render's free tier, utilizing `-Xmx256m` caps, SerialGC, Hikari connection limiting (3 min/max), and Tomcat thread capping (20) to prevent OOM kills.
- **Secure Authentication:** Stateless JSON Web Token (JWT) architecture backed by Spring Security and BCrypt password encoding. 

## 🛠 Tech Stack

**Frontend (Vite / React 18)**
- React Hooks for local scope & API abstraction services (`apiFetch`)
- Vanilla CSS3 Modules (Grid, Flexbox, Keyframes) mapping strict design tokens
- Chart.js for real-time charting canvases

**Backend (Java 17 / Spring Boot 3.2)**
- Spring Security (JWT filter chaining)
- Spring Mail (JavaMailSender / Gmail SMTP)
- Spring Data JPA, Hibernate (open-in-view disabled)
- Flyway Database Migrations
- Yahoo Finance Native HTTP Scraper

**Database**
- Serverless PostgreSQL (Hosted via Neon DB)

## ⚙️ Local Setup

### 1. Prerequisites
- **Java 17+** and **Maven** installed.
- **Node.js 18+** installed.
- A **PostgreSQL** instance (e.g., Neon DB).
- A **Google App Password** for SMTP mailing.

### 2. Backend Configuration
Navigate to the `backend` directory:
```bash
cd backend
```
Create a `.env` file in the `backend` folder securely referencing your own keys:
```env
# SMTP Alerting Configurations
GMAIL_USER=your_real_gmail_address@gmail.com
GMAIL_APP_PASSWORD=your_16_digit_app_password

# Database Configuration
NEON_HOST=ep-...aws.neon.tech
NEON_USER=neondb_owner
NEON_PASSWORD=your_db_password
NEON_DB=neondb

# Security (Generate a random secure Base64 256-bit hash)
JWT_SECRET=VnJ1blNlY3JldEtleU11c3RCZUF0TGVhc3QyNTZCaXRMb25nMTIzNDU2==
```

### 3. Run the Backend
Ensure your database is active. Flyway will automatically execute SQL scripts (`V1` to `V3`) and scaffold your schema during startup.
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```
*The backend will safely launch on `http://localhost:8080`.*

### 4. Run the React Frontend
In a new terminal window, navigate to the `frontend` directory:
```bash
cd frontend
npm install
npm run dev
```

### 5. Environment Variables
Your frontend requires the following `.env` at the root of `frontend`:
```env
VITE_API_BASE_URL=http://localhost:8080
```

Open `http://localhost:5173` in your web browser. Register an account, log in, and enjoy your dashboard!

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
