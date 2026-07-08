CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

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
