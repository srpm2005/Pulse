CREATE INDEX IF NOT EXISTS idx_tracked_user ON tracked_stocks(user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_user ON price_alerts(user_id);
CREATE INDEX IF NOT EXISTS idx_alerts_triggered ON price_alerts(triggered, user_id);
