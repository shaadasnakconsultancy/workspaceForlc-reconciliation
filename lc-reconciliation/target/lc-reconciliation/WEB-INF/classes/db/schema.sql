-- LC Reconciliation Database Schema for SQL Server

-- Users table for authentication
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'users')
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    first_name NVARCHAR(50),
    last_name NVARCHAR(50),
    email NVARCHAR(100),
    department NVARCHAR(100),
    role NVARCHAR(20) DEFAULT 'DEPARTMENT_USER',
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Add new columns if table already exists (migration)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'first_name')
    ALTER TABLE users ADD first_name NVARCHAR(50)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'last_name')
    ALTER TABLE users ADD last_name NVARCHAR(50)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'department')
    ALTER TABLE users ADD department NVARCHAR(100)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('users') AND name = 'role')
    ALTER TABLE users ADD role NVARCHAR(20) DEFAULT 'DEPARTMENT_USER'

UPDATE users SET first_name = 'Admin', last_name = 'User', role = 'SUPER_ADMIN' WHERE username = 'admin' AND (role IS NULL OR first_name IS NULL)

-- Application settings (key-value store)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'app_settings')
CREATE TABLE app_settings (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    setting_key NVARCHAR(100) NOT NULL UNIQUE,
    setting_value NVARCHAR(2000),
    setting_group NVARCHAR(50),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Document types master
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'document_types')
CREATE TABLE document_types (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    type_code NVARCHAR(50) NOT NULL UNIQUE,
    type_name NVARCHAR(100) NOT NULL,
    page_limit INT DEFAULT 0,
    is_active BIT DEFAULT 1,
    display_order INT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Prompt templates per document type
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'prompt_templates')
CREATE TABLE prompt_templates (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    document_type_id BIGINT NOT NULL,
    prompt_name NVARCHAR(100) NOT NULL,
    prompt_text NVARCHAR(MAX) NOT NULL,
    response_schema NVARCHAR(MAX),
    is_active BIT DEFAULT 1,
    version INT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (document_type_id) REFERENCES document_types(id)
);

-- Add response_schema column if table already exists (migration)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('prompt_templates') AND name = 'response_schema')
    ALTER TABLE prompt_templates ADD response_schema NVARCHAR(MAX)

-- LC documents (master LC + addendums)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'lc_documents')
CREATE TABLE lc_documents (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    lc_number NVARCHAR(50),
    file_name NVARCHAR(255) NOT NULL,
    file_path NVARCHAR(500) NOT NULL,
    file_size BIGINT,
    is_addendum BIT DEFAULT 0,
    upload_batch_id NVARCHAR(36) NOT NULL,
    ocr_text NVARCHAR(MAX),
    ocr_status NVARCHAR(20) DEFAULT 'PENDING',
    extracted_json NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Supporting documents (Invoice, Packing List, BL, COO, etc.)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'supporting_documents')
CREATE TABLE supporting_documents (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    upload_batch_id NVARCHAR(36) NOT NULL,
    document_type_id BIGINT NOT NULL,
    file_name NVARCHAR(255) NOT NULL,
    file_path NVARCHAR(500) NOT NULL,
    file_size BIGINT,
    page_limit INT DEFAULT 0,
    ocr_text NVARCHAR(MAX),
    ocr_status NVARCHAR(20) DEFAULT 'PENDING',
    extracted_json NVARCHAR(MAX),
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (document_type_id) REFERENCES document_types(id)
);

-- Reconciliation jobs
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'reconciliation_jobs')
CREATE TABLE reconciliation_jobs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    upload_batch_id NVARCHAR(36) NOT NULL,
    lc_number NVARCHAR(50),
    status NVARCHAR(20) DEFAULT 'QUEUED',
    total_steps INT DEFAULT 0,
    completed_steps INT DEFAULT 0,
    current_step NVARCHAR(200),
    error_message NVARCHAR(MAX),
    report_path NVARCHAR(500),
    non_compliance_report_path NVARCHAR(500),
    email_sent BIT DEFAULT 0,
    notification_email NVARCHAR(200),
    started_at DATETIME2,
    completed_at DATETIME2,
    created_at DATETIME2 DEFAULT GETDATE(),
    created_by NVARCHAR(50),
    created_by_department NVARCHAR(100)
);

-- Add department column if jobs table already exists (migration)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'created_by_department')
    ALTER TABLE reconciliation_jobs ADD created_by_department NVARCHAR(100)

-- Reason the notification email was not sent (disabled / no recipient / SMTP failure). NULL when sent.
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'email_error')
    ALTER TABLE reconciliation_jobs ADD email_error NVARCHAR(1000)

-- Reconciliation results (per parameter per document)
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'reconciliation_results')
CREATE TABLE reconciliation_results (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    job_id BIGINT NOT NULL,
    lc_number NVARCHAR(50),
    parameter_name NVARCHAR(100) NOT NULL,
    lc_clause_no NVARCHAR(50),
    lc_value NVARCHAR(MAX),
    document_type_code NVARCHAR(50),
    document_type_name NVARCHAR(100),
    document_value NVARCHAR(MAX),
    status NVARCHAR(30),
    reason NVARCHAR(2000),
    document_values NVARCHAR(MAX),
    match_result NVARCHAR(30),
    display_order INT DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (job_id) REFERENCES reconciliation_jobs(id)
);

-- Add new columns to reconciliation_results if they don't exist (migration)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_results') AND name = 'lc_clause_no')
    ALTER TABLE reconciliation_results ADD lc_clause_no NVARCHAR(50)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_results') AND name = 'document_type_code')
    ALTER TABLE reconciliation_results ADD document_type_code NVARCHAR(50)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_results') AND name = 'document_type_name')
    ALTER TABLE reconciliation_results ADD document_type_name NVARCHAR(100)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_results') AND name = 'document_value')
    ALTER TABLE reconciliation_results ADD document_value NVARCHAR(MAX)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_results') AND name = 'status')
    ALTER TABLE reconciliation_results ADD status NVARCHAR(30)

-- Job execution logs
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'job_logs')
CREATE TABLE job_logs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    job_id BIGINT NOT NULL,
    log_level NVARCHAR(10),
    message NVARCHAR(2000),
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (job_id) REFERENCES reconciliation_jobs(id)
);

-- Migration: Add new columns to reconciliation_jobs
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'job_name')
    ALTER TABLE reconciliation_jobs ADD job_name NVARCHAR(200)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'shipment_doc_name')
    ALTER TABLE reconciliation_jobs ADD shipment_doc_name NVARCHAR(200)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'xlsx_report_path')
    ALTER TABLE reconciliation_jobs ADD xlsx_report_path NVARCHAR(500)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'xlsx_nc_report_path')
    ALTER TABLE reconciliation_jobs ADD xlsx_nc_report_path NVARCHAR(500)

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'email_group_id')
    ALTER TABLE reconciliation_jobs ADD email_group_id BIGINT

-- Email groups
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'email_groups')
CREATE TABLE email_groups (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    group_name NVARCHAR(100) NOT NULL UNIQUE,
    description NVARCHAR(500),
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);

-- Email group members
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'email_group_members')
CREATE TABLE email_group_members (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    group_id BIGINT NOT NULL,
    email_address NVARCHAR(200) NOT NULL,
    member_name NVARCHAR(100),
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (group_id) REFERENCES email_groups(id)
);

-- Job API costs tracking
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'job_api_costs')
CREATE TABLE job_api_costs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    job_id BIGINT NOT NULL,
    api_type NVARCHAR(20) NOT NULL,
    document_name NVARCHAR(255),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    pages_processed INT DEFAULT 0,
    cost_inr DECIMAL(10,4) DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    FOREIGN KEY (job_id) REFERENCES reconciliation_jobs(id)
);

-- Email templates
IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'email_templates')
CREATE TABLE email_templates (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    template_name NVARCHAR(100) NOT NULL,
    subject_template NVARCHAR(500) NOT NULL,
    body_template NVARCHAR(MAX) NOT NULL,
    is_default BIT DEFAULT 0,
    is_active BIT DEFAULT 1,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE()
);
