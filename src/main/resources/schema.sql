CREATE TABLE IF NOT EXISTS demo_profile (
  id INTEGER PRIMARY KEY,
  display_name VARCHAR(80) NOT NULL,
  source_country VARCHAR(40) NOT NULL,
  study_country VARCHAR(40) NOT NULL,
  source_currency VARCHAR(3) NOT NULL,
  destination_currency VARCHAR(3) NOT NULL
);
CREATE TABLE IF NOT EXISTS financial_accounts (
  id VARCHAR(40) PRIMARY KEY,
  owner_profile_id INTEGER NOT NULL,
  account_name VARCHAR(80) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  balance DECIMAL(20,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS bank_events (
  id VARCHAR(40) PRIMARY KEY,
  source_label VARCHAR(50) NOT NULL,
  raw_reference VARCHAR(80) NOT NULL UNIQUE,
  received_at TIMESTAMP NOT NULL,
  processing_status VARCHAR(30) NOT NULL
);
CREATE TABLE IF NOT EXISTS transactions (
  id VARCHAR(40) PRIMARY KEY,
  event_id VARCHAR(40) NOT NULL UNIQUE,
  fingerprint VARCHAR(64) NOT NULL UNIQUE,
  account_id VARCHAR(40) NOT NULL,
  occurred_at TIMESTAMP NOT NULL,
  merchant VARCHAR(120) NOT NULL,
  description VARCHAR(255) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  direction VARCHAR(3) NOT NULL,
  type VARCHAR(30) NOT NULL,
  source_label VARCHAR(50) NOT NULL
);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS category VARCHAR(80);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS previous_category VARCHAR(80);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS confidence INTEGER NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS review_status VARCHAR(30) NOT NULL DEFAULT 'PURPOSE_REQUIRED';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS categorization_evidence VARCHAR(255) NOT NULL DEFAULT 'Not categorized';

CREATE TABLE IF NOT EXISTS budgets (
  category VARCHAR(80) PRIMARY KEY,
  monthly_limit DECIMAL(20,2) NOT NULL
);
