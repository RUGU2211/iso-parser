INSERT INTO limit_master (limit_name, limit_pnr, limit_rule_nr) VALUES
('goods_limit', 42, 52),
('card_not_present_limit', 43, 53),
('cash_limit', 44, 54)
ON CONFLICT (limit_name) DO NOTHING;
