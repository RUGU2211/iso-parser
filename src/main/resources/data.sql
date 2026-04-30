ALTER TABLE IF EXISTS limit_master ADD COLUMN IF NOT EXISTS limit_type VARCHAR(30);
ALTER TABLE IF EXISTS limit_master ADD COLUMN IF NOT EXISTS priority INTEGER;
ALTER TABLE IF EXISTS limit_master ADD COLUMN IF NOT EXISTS is_active BOOLEAN;

CREATE TABLE IF NOT EXISTS limit_extra_data (
    id BIGSERIAL PRIMARY KEY,
    pan VARCHAR(50) NOT NULL,
    seq_nr VARCHAR(10) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_value VARCHAR(500),
    source VARCHAR(20),
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_limit_extra_data_pan_seq ON limit_extra_data (pan, seq_nr);

DROP TABLE IF EXISTS card_limit_values;

-- limit_pnr = profile_nr, limit_rule_nr = rule_nr
INSERT INTO limit_master (limit_name, limit_pnr, limit_rule_nr, limit_type, priority, is_active) VALUES
('goods_limit', 42, 52, 'POS', 10, true),
('card_not_present_limit', 41, 53, 'ECOM', 20, true),
('cash_limit', 42, 54, 'CASH', 30, true)
ON CONFLICT (limit_name) DO UPDATE SET
limit_pnr = EXCLUDED.limit_pnr,
limit_rule_nr = EXCLUDED.limit_rule_nr,
limit_type = EXCLUDED.limit_type,
priority = EXCLUDED.priority,
is_active = EXCLUDED.is_active;
