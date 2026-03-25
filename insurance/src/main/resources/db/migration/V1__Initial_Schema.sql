-- V1__Initial_Schema.sql

CREATE TABLE IF NOT EXISTS insurance_quotes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(255) NOT NULL,
    offer_id VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    total_monthly_premium_amount DECIMAL(19,2) NOT NULL,
    total_coverage_amount DECIMAL(19,2) NOT NULL,

    document_number VARCHAR(11) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,

    document_number_cust VARCHAR(11) NOT NULL,
    name_cust VARCHAR(255) NOT NULL,
    type_cust VARCHAR(20) NOT NULL,
    gender_cust VARCHAR(10) NOT NULL,
    date_of_birth_cust DATE NOT NULL,
    email_cust VARCHAR(255) NOT NULL,
    phone_number_cust BIGINT NOT NULL,

    insurance_policy_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,

    INDEX idx_status (status),
    INDEX idx_document (document_number),
    INDEX idx_created_at (created_at),
    INDEX idx_policy_id (insurance_policy_id)
);

CREATE TABLE IF NOT EXISTS quote_coverages (
    quote_id BIGINT NOT NULL,
    coverage_name VARCHAR(255) NOT NULL,
    coverage_amount DECIMAL(19,2) NOT NULL,
    PRIMARY KEY (quote_id, coverage_name),
    FOREIGN KEY (quote_id) REFERENCES insurance_quotes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS quote_assistances (
    quote_id BIGINT NOT NULL,
    assistance_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (quote_id, assistance_name),
    FOREIGN KEY (quote_id) REFERENCES insurance_quotes(id) ON DELETE CASCADE
);

