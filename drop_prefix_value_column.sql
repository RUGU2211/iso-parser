-- Run this in your PostgreSQL client to drop the prefix_value column from limit_master
-- (Entity and data.sql already updated; this updates the existing database)

ALTER TABLE limit_master DROP COLUMN IF EXISTS prefix_value;
