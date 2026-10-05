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
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS institution VARCHAR(120) NOT NULL DEFAULT 'Manual source';
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS account_type VARCHAR(30) NOT NULL DEFAULT 'CHECKING';
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS masked_number VARCHAR(20) NOT NULL DEFAULT '••••';
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) NOT NULL DEFAULT 'CONNECTED';
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS connection_status VARCHAR(20) NOT NULL DEFAULT 'CONNECTED';
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS balance_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE financial_accounts ADD COLUMN IF NOT EXISTS account_scope VARCHAR(20) NOT NULL DEFAULT 'PERSONAL';
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
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS purpose VARCHAR(160);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS category_source VARCHAR(20) NOT NULL DEFAULT 'RULE';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_action_id VARCHAR(40);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_receipt_id VARCHAR(40);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_quote_id VARCHAR(80);
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS payment_channel_id VARCHAR(40);

CREATE TABLE IF NOT EXISTS transaction_categories (
  name VARCHAR(80) PRIMARY KEY,
  category_type VARCHAR(20) NOT NULL,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS budgets (
  category VARCHAR(80) PRIMARY KEY,
  monthly_limit DECIMAL(20,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS finance_plans (
  id VARCHAR(40) PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  plan_type VARCHAR(30) NOT NULL,
  category VARCHAR(80) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  cadence VARCHAR(20) NOT NULL,
  next_due_date DATE NOT NULL,
  funding_account_id VARCHAR(40),
  reserve_funds BOOLEAN NOT NULL,
  status VARCHAR(20) NOT NULL,
  notes VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
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
  recipient_name VARCHAR(160) NOT NULL,
  recipient_bank_name VARCHAR(160) NOT NULL,
  recipient_bank_code VARCHAR(34) NOT NULL,
  recipient_account VARCHAR(120) NOT NULL UNIQUE,
  destination_country VARCHAR(40) NOT NULL,
  destination_currency VARCHAR(3) NOT NULL,
  verification_status VARCHAR(20) NOT NULL
);
ALTER TABLE school_registry ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(160) NOT NULL DEFAULT 'Not provided';
ALTER TABLE school_registry ADD COLUMN IF NOT EXISTS recipient_bank_name VARCHAR(160) NOT NULL DEFAULT 'Not provided';
ALTER TABLE school_registry ADD COLUMN IF NOT EXISTS recipient_bank_code VARCHAR(34) NOT NULL DEFAULT 'Not provided';
UPDATE school_registry SET recipient_name='Shenzhen Demo University Tuition Office', recipient_bank_name='Shenzhen Demo Education Bank', recipient_bank_code='SZDUCNBSXXX' WHERE institution='Shenzhen Demo University' AND recipient_account='SZDU-TUITION-2026' AND recipient_name='Not provided';
UPDATE school_registry SET recipient_name='Pacific Demo College Bursar', recipient_bank_name='Pacific Demo Bank', recipient_bank_code='PDCMUS33XXX' WHERE institution='Pacific Demo College' AND recipient_account='PDC-TUITION-USD' AND recipient_name='Not provided';
UPDATE school_registry SET recipient_name='Sydney Demo Institute Fees Office', recipient_bank_name='Sydney Demo Bank', recipient_bank_code='SDIIAU2SXXX' WHERE institution='Sydney Demo Institute' AND recipient_account='SDI-TUITION-AUD' AND recipient_name='Not provided';

CREATE TABLE IF NOT EXISTS international_bills (
  id INTEGER PRIMARY KEY,
  expense_type VARCHAR(30) NOT NULL DEFAULT 'TUITION',
  title VARCHAR(120) NOT NULL DEFAULT 'Tuition fee',
  institution VARCHAR(120) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  destination_country VARCHAR(40) NOT NULL DEFAULT 'China',
  recipient_name VARCHAR(160) NOT NULL,
  recipient_bank_name VARCHAR(160) NOT NULL,
  recipient_bank_code VARCHAR(34) NOT NULL,
  recipient_account VARCHAR(120) NOT NULL,
  payment_reference VARCHAR(80) NOT NULL,
  due_date DATE NOT NULL,
  evidence_label VARCHAR(120) NOT NULL,
  document_name VARCHAR(255),
  document_content_type VARCHAR(100),
  document_size BIGINT,
  selected BOOLEAN NOT NULL DEFAULT FALSE,
  lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS expense_type VARCHAR(30) NOT NULL DEFAULT 'TUITION';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS title VARCHAR(120) NOT NULL DEFAULT 'Tuition fee';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS destination_country VARCHAR(40) NOT NULL DEFAULT 'China';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(160) NOT NULL DEFAULT 'Not provided';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS recipient_bank_name VARCHAR(160) NOT NULL DEFAULT 'Not provided';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS recipient_bank_code VARCHAR(34) NOT NULL DEFAULT 'Not provided';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS document_name VARCHAR(255);
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS document_content_type VARCHAR(100);
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS document_size BIGINT;
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS selected BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE international_bills ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE international_bills SET recipient_name='Shenzhen Demo University Tuition Office', recipient_bank_name='Shenzhen Demo Education Bank', recipient_bank_code='SZDUCNBSXXX' WHERE institution='Shenzhen Demo University' AND recipient_account='SZDU-TUITION-2026' AND recipient_name='Not provided';
UPDATE international_bills SET recipient_name='Pacific Demo College Bursar', recipient_bank_name='Pacific Demo Bank', recipient_bank_code='PDCMUS33XXX' WHERE institution='Pacific Demo College' AND recipient_account='PDC-TUITION-USD' AND recipient_name='Not provided';
UPDATE international_bills SET recipient_name='Sydney Demo Institute Fees Office', recipient_bank_name='Sydney Demo Bank', recipient_bank_code='SDIIAU2SXXX' WHERE institution='Sydney Demo Institute' AND recipient_account='SDI-TUITION-AUD' AND recipient_name='Not provided';
UPDATE international_bills b SET selected=FALSE WHERE b.selected=TRUE AND NOT EXISTS (
  SELECT 1 FROM school_registry r WHERE r.institution=b.institution AND r.recipient_name=b.recipient_name
    AND r.recipient_bank_name=b.recipient_bank_name AND r.recipient_bank_code=b.recipient_bank_code
    AND r.recipient_account=b.recipient_account AND r.destination_country=b.destination_country
    AND r.destination_currency=b.currency AND r.verification_status='VERIFIED'
);
UPDATE international_bills SET selected=TRUE WHERE id=(
  SELECT MIN(b.id) FROM international_bills b WHERE b.lifecycle_status='ACTIVE' AND EXISTS (
    SELECT 1 FROM school_registry r WHERE r.institution=b.institution AND r.recipient_name=b.recipient_name
      AND r.recipient_bank_name=b.recipient_bank_name AND r.recipient_bank_code=b.recipient_bank_code
      AND r.recipient_account=b.recipient_account AND r.destination_country=b.destination_country
      AND r.destination_currency=b.currency AND r.verification_status='VERIFIED'
  )
) AND NOT EXISTS (SELECT 1 FROM international_bills WHERE selected=TRUE);


CREATE TABLE IF NOT EXISTS payment_channels (
  id VARCHAR(30) PRIMARY KEY,
  source_account_id VARCHAR(40),
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
ALTER TABLE payment_channels ADD COLUMN IF NOT EXISTS source_account_id VARCHAR(40);

CREATE TABLE IF NOT EXISTS payment_channel_corridors (
  channel_id VARCHAR(30) NOT NULL,
  destination_country VARCHAR(40) NOT NULL,
  destination_currency VARCHAR(3) NOT NULL,
  eligible BOOLEAN NOT NULL,
  eligibility_reason VARCHAR(255) NOT NULL,
  rate_vnd_per_unit DECIMAL(20,4) NOT NULL,
  transfer_fee_vnd DECIMAL(20,2) NOT NULL,
  fx_markup_rate DECIMAL(12,6) NOT NULL,
  safety_score INTEGER NOT NULL,
  settlement_min_days INTEGER NOT NULL,
  settlement_max_days INTEGER NOT NULL,
  quote_source VARCHAR(120) NOT NULL,
  PRIMARY KEY (channel_id,destination_country,destination_currency)
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
ALTER TABLE fx_quotes ADD COLUMN IF NOT EXISTS destination_country VARCHAR(40) NOT NULL DEFAULT 'China';
ALTER TABLE fx_quotes ADD COLUMN IF NOT EXISTS destination_currency VARCHAR(3) NOT NULL DEFAULT 'CNY';

CREATE TABLE IF NOT EXISTS agent_policy (
  id INTEGER PRIMARY KEY,
  mode VARCHAR(20) NOT NULL,
  agent_state VARCHAR(20) NOT NULL,
  runtime_mode VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
  per_transaction_limit DECIMAL(20,2) NOT NULL,
  daily_limit DECIMAL(20,2) NOT NULL,
  frequency_limit INTEGER NOT NULL,
  safety_buffer DECIMAL(20,2) NOT NULL
);
ALTER TABLE agent_policy ADD COLUMN IF NOT EXISTS runtime_mode VARCHAR(20) NOT NULL DEFAULT 'OFFLINE';
CREATE TABLE IF NOT EXISTS recipient_allowlist (
  recipient_id VARCHAR(120) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  purpose VARCHAR(30) NOT NULL,
  PRIMARY KEY (recipient_id,purpose)
);
CREATE TABLE IF NOT EXISTS action_plans (
  id VARCHAR(40) PRIMARY KEY,
  action_type VARCHAR(30) NOT NULL,
  purpose VARCHAR(255) NOT NULL,
  debit_amount DECIMAL(20,2) NOT NULL,
  conversion_amount DECIMAL(20,2) NOT NULL,
  transfer_fee DECIMAL(20,2) NOT NULL,
  fx_markup DECIMAL(20,2) NOT NULL,
  source_currency VARCHAR(3) NOT NULL,
  destination_amount DECIMAL(20,2) NOT NULL,
  destination_currency VARCHAR(3) NOT NULL,
  recipient VARCHAR(120) NOT NULL,
  channel_id VARCHAR(30),
  quote_id VARCHAR(50),
  required_permission VARCHAR(20) NOT NULL,
  impact VARCHAR(500) NOT NULL,
  risk VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL,
  action_hash VARCHAR(64) NOT NULL,
  idempotency_key VARCHAR(80) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL
);
ALTER TABLE action_plans ADD COLUMN IF NOT EXISTS source_account_id VARCHAR(40);
ALTER TABLE action_plans ADD COLUMN IF NOT EXISTS expense_id INTEGER;
CREATE TABLE IF NOT EXISTS action_payment_snapshots (
  action_id VARCHAR(40) PRIMARY KEY,
  bill_updated_at TIMESTAMP NOT NULL,
  bill_title VARCHAR(120) NOT NULL,
  institution VARCHAR(120) NOT NULL,
  payment_reference VARCHAR(80) NOT NULL,
  due_date DATE NOT NULL,
  destination_country VARCHAR(40) NOT NULL,
  recipient_name VARCHAR(160) NOT NULL,
  recipient_bank_name VARCHAR(160) NOT NULL,
  recipient_bank_code VARCHAR(34) NOT NULL,
  recipient_account VARCHAR(120) NOT NULL,
  source_display_name VARCHAR(120) NOT NULL,
  source_institution VARCHAR(120) NOT NULL,
  source_masked_number VARCHAR(20) NOT NULL,
  channel_name VARCHAR(120) NOT NULL,
  rate DECIMAL(20,4) NOT NULL,
  quote_source VARCHAR(120) NOT NULL,
  quoted_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  settlement_min_days INTEGER NOT NULL,
  settlement_max_days INTEGER NOT NULL,
  latest_safe_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS approvals (
  action_id VARCHAR(40) PRIMARY KEY,
  approved_hash VARCHAR(64) NOT NULL,
  approved_by VARCHAR(80) NOT NULL,
  approved_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL
);
CREATE TABLE IF NOT EXISTS sandbox_accounts (
  id VARCHAR(40) PRIMARY KEY,
  display_name VARCHAR(120) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  balance DECIMAL(20,2) NOT NULL
);
CREATE TABLE IF NOT EXISTS payment_source_accounts (
  account_id VARCHAR(40) PRIMARY KEY,
  institution VARCHAR(120) NOT NULL,
  account_type VARCHAR(40) NOT NULL,
  masked_number VARCHAR(20) NOT NULL,
  connection_status VARCHAR(20) NOT NULL,
  verification_status VARCHAR(20) NOT NULL,
  cross_border_enabled BOOLEAN NOT NULL,
  selected BOOLEAN NOT NULL DEFAULT FALSE,
  display_order INTEGER NOT NULL
);
ALTER TABLE payment_source_accounts ADD COLUMN IF NOT EXISTS financial_account_id VARCHAR(40);
CREATE UNIQUE INDEX IF NOT EXISTS payment_source_canonical_account ON payment_source_accounts(financial_account_id);
CREATE TABLE IF NOT EXISTS sandbox_transactions (
  id VARCHAR(40) PRIMARY KEY,
  action_id VARCHAR(40) NOT NULL UNIQUE,
  idempotency_key VARCHAR(80) NOT NULL UNIQUE,
  channel_id VARCHAR(30),
  quote_id VARCHAR(50),
  recipient VARCHAR(120) NOT NULL,
  vnd_balance_before DECIMAL(20,2) NOT NULL,
  vnd_debit DECIMAL(20,2) NOT NULL,
  conversion_vnd DECIMAL(20,2) NOT NULL,
  fee_deduction_vnd DECIMAL(20,2) NOT NULL,
  vnd_balance_after DECIMAL(20,2) NOT NULL,
  cny_balance_before DECIMAL(20,2) NOT NULL,
  cny_credit DECIMAL(20,2) NOT NULL,
  cny_balance_after DECIMAL(20,2) NOT NULL,
  rate_vnd_per_cny DECIMAL(20,4) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
ALTER TABLE sandbox_transactions ADD COLUMN IF NOT EXISTS source_account_id VARCHAR(40);
CREATE TABLE IF NOT EXISTS sandbox_ledger_entries (
  id VARCHAR(40) PRIMARY KEY,
  transaction_id VARCHAR(40) NOT NULL,
  entry_type VARCHAR(30) NOT NULL,
  account_id VARCHAR(40) NOT NULL,
  amount DECIMAL(20,2) NOT NULL,
  currency VARCHAR(3) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
CREATE TABLE IF NOT EXISTS audit_log (
  id VARCHAR(40) PRIMARY KEY,
  occurred_at TIMESTAMP NOT NULL,
  actor VARCHAR(40) NOT NULL,
  event_type VARCHAR(50) NOT NULL,
  reference_id VARCHAR(40),
  status VARCHAR(30) NOT NULL,
  reason_code VARCHAR(50),
  details VARCHAR(500) NOT NULL
);
CREATE TABLE IF NOT EXISTS conversation_messages (
  id VARCHAR(40) PRIMARY KEY,
  role VARCHAR(20) NOT NULL,
  message VARCHAR(1000) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
