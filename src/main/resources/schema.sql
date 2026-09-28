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

CREATE TABLE IF NOT EXISTS student_corridor_profile (
  id INTEGER PRIMARY KEY,
  demo_profile_id INTEGER NOT NULL,
  source_country VARCHAR(40) NOT NULL,
  destination_country VARCHAR(40) NOT NULL,
  source_currency VARCHAR(3) NOT NULL,
  destination_currency VARCHAR(3) NOT NULL,
  preference VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS school_registry (
  id INTEGER PRIMARY KEY,
  institution VARCHAR(120) NOT NULL,
  recipient_account VARCHAR(120) NOT NULL UNIQUE,
  destination_country VARCHAR(40) NOT NULL,
  destination_currency VARCHAR(3) NOT NULL,
  verification_status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS international_bills (
  id INTEGER PRIMARY KEY,
  institution VARCHAR(120) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  recipient_account VARCHAR(120) NOT NULL,
  payment_reference VARCHAR(80) NOT NULL,
  due_date DATE NOT NULL,
  evidence_label VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_channels (
  id VARCHAR(30) PRIMARY KEY,
  display_name VARCHAR(120) NOT NULL,
  eligible BOOLEAN NOT NULL,
  eligibility_reason VARCHAR(255) NOT NULL,
  seed_rate_vnd_per_cny DECIMAL(20,4) NOT NULL,
  transfer_fee_vnd DECIMAL(20,2) NOT NULL,
  fx_markup_rate DECIMAL(12,6) NOT NULL,
  safety_score INTEGER NOT NULL,
  settlement_min_days INTEGER NOT NULL,
  settlement_max_days INTEGER NOT NULL,
  quote_source VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS fx_quotes (
  id VARCHAR(50) PRIMARY KEY,
  channel_id VARCHAR(30) NOT NULL,
  rate_vnd_per_cny DECIMAL(20,4) NOT NULL,
  source_label VARCHAR(120) NOT NULL,
  quoted_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  quote_status VARCHAR(20) NOT NULL
);
