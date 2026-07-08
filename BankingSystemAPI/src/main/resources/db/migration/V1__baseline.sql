-- Baseline schema (portable across H2 in MySQL compatibility mode and MySQL 8).
-- No CREATE DATABASE / USE — the target schema/database is selected by the datasource URL.

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone_number VARCHAR(30),
    email VARCHAR(100),
    address VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER'
);

CREATE TABLE accounts (
    account_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(30) NOT NULL,
    balance DECIMAL(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE transactions (
    transaction_id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts(account_id)
);

CREATE TABLE logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    message VARCHAR(255),
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);
