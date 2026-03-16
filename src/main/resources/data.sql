-- limit_pnr = profile_nr (42=POS, 41=Ecom, 42=Cash), limit_rule_nr = rule_nr (52=Pos, 53=Ecom, 54=Cash)
INSERT INTO limit_master (limit_name, limit_pnr, limit_rule_nr) VALUES
('goods_limit', 42, 52),
('card_not_present_limit', 41, 53),
('cash_limit', 42, 54)
ON CONFLICT (limit_name) DO UPDATE SET limit_pnr = EXCLUDED.limit_pnr, limit_rule_nr = EXCLUDED.limit_rule_nr;
