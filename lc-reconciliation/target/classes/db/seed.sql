-- Seed data for LC Reconciliation System

-- Default admin user (password: admin123)
IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin')
INSERT INTO users (username, password_hash, first_name, last_name, email, department, role, is_active)
VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Admin', 'User', 'admin@smipl.com', 'IT', 'SUPER_ADMIN', 1);

-- Default document types
IF NOT EXISTS (SELECT 1 FROM document_types WHERE type_code = 'MASTER_LC')
BEGIN
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('MASTER_LC', 'Master LC Document', 0, 1, 0);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('LC_ADDENDUM', 'LC Addendum', 0, 1, 1);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('INVOICE', 'Invoice', 1, 1, 2);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('PACKING_LIST', 'Packing List', 3, 1, 3);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('BENEF_CERT', 'Beneficiary Certificate', 0, 1, 4);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('COO', 'Certificate of Origin', 0, 1, 5);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('BILL_EXCH', 'Bill of Exchange', 0, 1, 6);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('SHIP_ADVISE', 'Shipping Advise', 0, 1, 7);
    INSERT INTO document_types (type_code, type_name, page_limit, is_active, display_order) VALUES ('BL', 'Bill of Lading', 0, 1, 8);
END;

-- Default settings
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'openai_endpoint')
BEGIN
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('openai_endpoint', '', 'OPENAI');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('openai_api_key', '', 'OPENAI');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('openai_model', 'gpt-4o', 'OPENAI');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('openai_deployment', '', 'OPENAI');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('openai_api_version', '2024-12-01-preview', 'OPENAI');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('doc_intel_endpoint', '', 'DOC_INTELLIGENCE');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('doc_intel_api_key', '', 'DOC_INTELLIGENCE');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_host', '', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_port', '587', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_from', '', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_username', '', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_password', '', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_tls_enabled', 'true', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_enabled', 'true', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_timeout_seconds', '20', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('notification_email_to', '', 'SMTP');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('cost_openai_input_per_1m_tokens', '1.25', 'COST_RATES');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('cost_openai_output_per_1m_tokens', '5.00', 'COST_RATES');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('cost_doc_intel_per_page', '8.50', 'COST_RATES');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('cost_usd_to_inr', '85.00', 'COST_RATES');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('graph_tenant_id', '', 'GRAPH_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('graph_client_id', '', 'GRAPH_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('graph_client_secret', '', 'GRAPH_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('graph_drive_id', '', 'GRAPH_API');
    -- SAP OData API for HSN code compliance check
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('sap_base_url', 'https://vhszkd61ci.hec.suzuki.co.jp:44300/sap/opu/odata/sap/ZVINCSD_ZINCSDTBDPARTS_CDS/ZVINCSD_ZINCSDTBDPARTS', 'SAP_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('sap_client', '110', 'SAP_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('sap_username', '', 'SAP_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('sap_password', '', 'SAP_API');
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('sap_enabled', 'false', 'SAP_API');
    -- HSN comparison source: 'SAP' (SAP OData API) or 'INVOICE' (open-source PDF extraction of invoice page 2+)
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('hsn_source', 'SAP', 'SAP_API');
END;

-- Settings added after the initial release. The block above only runs on a fresh install, so each
-- new key is inserted individually here to also reach already-deployed databases.
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'smtp_enabled')
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_enabled', 'true', 'SMTP');

IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'smtp_timeout_seconds')
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('smtp_timeout_seconds', '20', 'SMTP');

-- Hard wall-clock limit per reconciliation job; the watchdog fails the job and frees its worker thread.
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'job_timeout_minutes')
    INSERT INTO app_settings (setting_key, setting_value, setting_group) VALUES ('job_timeout_minutes', '120', 'JOB');
