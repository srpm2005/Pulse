CREATE TABLE tracked_stocks (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    symbol          VARCHAR(50) NOT NULL,
    company_name    VARCHAR(255),
    created_at      TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, symbol)
);
