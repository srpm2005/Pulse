# Pulse

Pulse is a modern, full-stack real-time stock tracking dashboard featuring an elegant dark glassmorphism UI, secure JWT authentication, and automated email price alerts.


<img width="1917" height="1021" alt="image" src="https://github.com/user-attachments/assets/9315708d-c368-4e7e-b0fc-89cb26351f0f" />

## 🚀 Features

- **Global Stock Tracking:** Live market data scraping natively hooked into Yahoo Finance. Supports US Equities, Indian NSE/BSE stocks, ETFs, and more.
- **Automated Price Alerts:** Set dynamic `ABOVE` or `BELOW` target thresholds. A background cron-scheduler silently polls the market and automatically triggers emails directly to your inbox using Gmail SMTP.
- **Secure Authentication:** Stateless JSON Web Token (JWT) architecture backed by Spring Security and password encoding.
- **Beautiful UI:** A clean, responsive, vanilla web user interface styled with premium dark mode aesthetics.

## 🛠 Tech Stack

**Frontend**
- Vanilla HTML5 / CSS3 / JavaScript
- Fetch API for asynchronous REST communication

**Backend**
- Java 17 / Spring Boot 3.2
- Spring Security (JWT)
- Spring Mail (JavaMailSender / Gmail SMTP)
- Spring Data JPA & Hibernate
- Flyway Database Migrations
- Yahoo Finance Native HTTP Scraper

**Database**
- PostgreSQL (Hosted via Neon DB)

## ⚙️ Local Setup

### 1. Prerequisites
- **Java 17+** and **Maven** installed.
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
Ensure your database is active. Flyway will automatically execute SQL scripts and scaffold your schema during startup.
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```
*The backend will safely launch on `http://localhost:8080`.*

### 4. Run the Frontend
In a new terminal window, navigate to the `frontend` directory and serve the static files natively:
```bash
cd frontend
npx serve .
```
Open `http://localhost:3000` in your web browser. Register an account, log in, and enjoy your dashboard!

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
