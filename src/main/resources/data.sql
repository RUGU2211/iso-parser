-- Single SQL script used by the application (spring.sql.init). No other .sql file is required at runtime.
-- PostgreSQL: public.pc_card_ext_lim_12_b (JPA entity CardLimit). Statements end with @@ (spring.sql.init.separator).
-- Hibernate creates/updates tables first; this script migrates legacy public.card_limits when needed.

-- 0) Legacy: Hibernate may have created an empty pc_card_ext_lim_12_b while old data still lives in card_limits.
DO $$
BEGIN
    IF to_regclass('public.card_limits') IS NOT NULL AND to_regclass('public.pc_card_ext_lim_12_b') IS NOT NULL THEN
        IF (SELECT COUNT(*) FROM public.pc_card_ext_lim_12_b) = 0 THEN
            DROP TABLE public.pc_card_ext_lim_12_b;
            ALTER TABLE public.card_limits RENAME TO pc_card_ext_lim_12_b;
        END IF;
    ELSIF to_regclass('public.card_limits') IS NOT NULL AND to_regclass('public.pc_card_ext_lim_12_b') IS NULL THEN
        ALTER TABLE public.card_limits RENAME TO pc_card_ext_lim_12_b;
    END IF;
END $$@@

-- 0b) Rename legacy payload column limits -> card_limits when present
DO $$
BEGIN
    IF to_regclass('public.pc_card_ext_lim_12_b') IS NULL THEN
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'limits')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'card_limits') THEN
        ALTER TABLE public.pc_card_ext_lim_12_b RENAME COLUMN limits TO card_limits;
    END IF;
END $$@@

-- 1) Rename legacy columns when present
DO $$
BEGIN
    IF to_regclass('public.pc_card_ext_lim_12_b') IS NULL THEN
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'iso_nr')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'issuer_nr') THEN
        ALTER TABLE public.pc_card_ext_lim_12_b RENAME COLUMN iso_nr TO issuer_nr;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'last_upd_date')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'last_updated_date') THEN
        ALTER TABLE public.pc_card_ext_lim_12_b RENAME COLUMN last_upd_date TO last_updated_date;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'last_upd_user')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'last_updated_user') THEN
        ALTER TABLE public.pc_card_ext_lim_12_b RENAME COLUMN last_upd_user TO last_updated_user;
    END IF;
END $$@@

ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS issuer_nr INTEGER@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS total_data_received TEXT NOT NULL DEFAULT '{}'::text@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS created_date TIMESTAMP@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS last_updated_date TIMESTAMP@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS last_updated_user VARCHAR(20)@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS date_deleted TIMESTAMP@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS pan VARCHAR(66)@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ADD COLUMN IF NOT EXISTS seq_nr VARCHAR(3)@@

UPDATE public.pc_card_ext_lim_12_b SET issuer_nr = 13 WHERE issuer_nr IS NULL@@

ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN issuer_nr SET NOT NULL@@

ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN card_limits TYPE TEXT USING COALESCE(card_limits, '')::TEXT@@

ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b DROP COLUMN IF EXISTS date_date@@

UPDATE public.pc_card_ext_lim_12_b SET
    created_date = COALESCE(created_date, last_updated_date, CURRENT_TIMESTAMP),
    last_updated_date = COALESCE(last_updated_date, created_date, CURRENT_TIMESTAMP),
    last_updated_user = LEFT(COALESCE(NULLIF(TRIM(last_updated_user), ''), 'sp'), 20)@@

DO $$
BEGIN
    IF to_regclass('public.pc_card_ext_lim_12_b') IS NULL THEN
        RETURN;
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'last_updated_user') THEN
        ALTER TABLE public.pc_card_ext_lim_12_b
            ALTER COLUMN last_updated_user TYPE VARCHAR(20)
            USING LEFT(COALESCE(last_updated_user, 'sp'), 20);
    END IF;
END $$@@

ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN created_date SET NOT NULL@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN last_updated_date SET NOT NULL@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN last_updated_user SET NOT NULL@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN card_limits SET NOT NULL@@
ALTER TABLE IF EXISTS public.pc_card_ext_lim_12_b ALTER COLUMN card_limits SET DEFAULT ''@@

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'limit_extra_data')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'pc_card_ext_lim_12_b' AND column_name = 'total_data_received') THEN
        ALTER TABLE public.pc_card_ext_lim_12_b RENAME COLUMN limit_extra_data TO total_data_received;
    END IF;
END $$@@

UPDATE public.pc_card_ext_lim_12_b
SET total_data_received = COALESCE(NULLIF(total_data_received, ''), '{}')@@

DROP TABLE IF EXISTS public.limit_extra_data@@

DO $$
DECLARE r RECORD;
BEGIN
    IF to_regclass('public.pc_card_ext_lim_12_b') IS NULL THEN
        RETURN;
    END IF;
    FOR r IN (
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_namespace n ON t.relnamespace = n.oid
        WHERE n.nspname = 'public' AND t.relname = 'pc_card_ext_lim_12_b' AND c.contype = 'u'
    ) LOOP
        EXECUTE format('ALTER TABLE public.pc_card_ext_lim_12_b DROP CONSTRAINT IF EXISTS %I', r.conname);
    END LOOP;
END $$@@

CREATE UNIQUE INDEX IF NOT EXISTS ux_pc_card_ext_lim_12_b_active_issuer_pan_seq
    ON public.pc_card_ext_lim_12_b (issuer_nr, pan, seq_nr)
    WHERE date_deleted IS NULL@@

CREATE INDEX IF NOT EXISTS ix_pc_card_ext_lim_12_b_issuer_pan
    ON public.pc_card_ext_lim_12_b (issuer_nr, pan)
    INCLUDE (seq_nr, last_updated_date)
    WHERE date_deleted IS NULL@@

ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS binary_hex@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS iso_fields_formatted@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS de11@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS pan@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS expiry_date@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS seq_nr@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS cash_limit@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS goods_limit@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS card_not_present_limit@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS error_message@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS processing_time_ms@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS request_id@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS api_operation@@
ALTER TABLE IF EXISTS public.iso_audit DROP COLUMN IF EXISTS api_detail@@

ALTER TABLE IF EXISTS limit_master ADD COLUMN IF NOT EXISTS limit_type VARCHAR(30)@@
ALTER TABLE IF EXISTS limit_master ADD COLUMN IF NOT EXISTS priority INTEGER@@
ALTER TABLE IF EXISTS limit_master ADD COLUMN IF NOT EXISTS is_active BOOLEAN@@

INSERT INTO limit_master (limit_name, limit_pnr, limit_rule_nr, limit_type, priority, is_active) VALUES
('goods_limit', 42, 52, 'POS', 10, true),
('card_not_present_limit', 41, 53, 'ECOM', 20, true),
('cash_limit', 42, 54, 'CASH', 30, true),
('upi_limit', 41, 55, 'UPI', 40, true),
('tap_and_pay_limit', 42, 56, 'TAP', 50, true),
('international_limit', 42, 57, 'INTL', 60, true),
('atm_limit', 42, 58, 'ATM', 70, true),
('recurring_limit', 41, 59, 'RECUR', 80, true),
('contactless_limit', 42, 60, 'CLESS', 90, true)
ON CONFLICT (limit_name) DO UPDATE SET
limit_pnr = EXCLUDED.limit_pnr,
limit_rule_nr = EXCLUDED.limit_rule_nr,
limit_type = EXCLUDED.limit_type,
priority = EXCLUDED.priority,
is_active = EXCLUDED.is_active@@
