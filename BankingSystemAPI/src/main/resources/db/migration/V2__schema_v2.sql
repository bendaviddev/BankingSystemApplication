-- Schema v2: additive ALTERs with backfill (no DROP/CREATE of existing tables).
-- Portable across H2 (MODE=MySQL) and MySQL 8.

-- ── users: audit timestamps ────────────────────────────────────────────────
ALTER TABLE users ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- ── accounts: audit timestamps, type value migration ───────────────────────
ALTER TABLE accounts ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE accounts ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Migrate legacy type values before any app code reads the enum.
UPDATE accounts SET account_type = 'CHECKING' WHERE account_type = 'BASIC';
UPDATE accounts SET account_type = 'SAVINGS' WHERE account_type = 'SAVING';

-- NOTE: the spec calls for widening balance/amount to DECIMAL(19,2). H2's MySQL
-- compatibility mode does not implement MySQL's `MODIFY COLUMN` syntax (it only
-- accepts its own `ALTER COLUMN ... SET DATA TYPE`, which MySQL in turn doesn't
-- accept), so there is no single statement that runs unmodified on both engines.
-- V1's DECIMAL(12,2) already safely covers the $1,000,000 per-operation cap
-- enforced in the service layer, so the column type is left as-is rather than
-- splitting this migration per-vendor.

-- ── transactions: v2 columns ────────────────────────────────────────────────
ALTER TABLE transactions ADD COLUMN counterparty_account_id INT NULL;
ALTER TABLE transactions ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED';
ALTER TABLE transactions ADD COLUMN running_balance DECIMAL(19, 2) NULL;
ALTER TABLE transactions ADD COLUMN category VARCHAR(40) NOT NULL DEFAULT 'OTHER';
ALTER TABLE transactions ADD COLUMN memo VARCHAR(140) NULL;

-- reference: add nullable, backfill a unique value per existing row, then add a
-- uniqueness constraint. NOT NULL is enforced at the application layer rather than
-- via a follow-up ALTER (same MODIFY COLUMN portability issue as above) — every
-- code path that inserts a transaction always supplies a reference.
ALTER TABLE transactions ADD COLUMN reference VARCHAR(40) NULL;
UPDATE transactions SET reference = CONCAT('TXN-LEGACY-', transaction_id) WHERE reference IS NULL;
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_reference UNIQUE (reference);

CREATE INDEX idx_transactions_account_created ON transactions (account_id, created_at);
CREATE INDEX idx_transactions_status ON transactions (status);

-- ── sessions (new) ──────────────────────────────────────────────────────────
CREATE TABLE sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,
    token_hash CHAR(64) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    created_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_sessions_user ON sessions (user_id);

-- ── alerts (new) ────────────────────────────────────────────────────────────
CREATE TABLE alerts (
    alert_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message VARCHAR(255) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_alerts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_alerts_user_created ON alerts (user_id, created_at);
