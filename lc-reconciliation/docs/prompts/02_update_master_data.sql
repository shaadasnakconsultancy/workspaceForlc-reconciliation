/* ============================================================================
   LC Reconciliation - master data / settings updates
   Run AFTER 01_update_prompts.sql, against the PROD LetterOfCreditDB.

   Safe to run more than once. It changes no table structure except one
   additive, nullable column that is guarded by IF NOT EXISTS.
   ============================================================================ */

SET NOCOUNT ON;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

/* ---------------------------------------------------------------------------
   SECTION 1 - Invoice page limit (REQUIRED for this release)

   The invoice is OCR'd up to this many pages. It was 1, which only reads the
   first page, so goods lots continuing onto later pages were never checked for
   Production Month and Engine CC.

   Cost note: this increases OCR cost per invoice, billed per page.
   --------------------------------------------------------------------------- */

UPDATE document_types
SET page_limit = 3, updated_at = GETDATE()
WHERE type_code = 'INVOICE';

PRINT 'Section 1: INVOICE page_limit set to 3';

/* ---------------------------------------------------------------------------
   SECTION 2 - August release DB changes (idempotent)

   Only needed if the August build has NOT yet been deployed to this server.
   If it has, every statement below is skipped by its own guard and nothing
   changes. Running it either way is harmless.
   --------------------------------------------------------------------------- */

-- Reason the notification email was not sent (disabled / no recipient / SMTP failure).
-- NULL when the email was sent. Additive and nullable.
IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'email_error')
BEGIN
    ALTER TABLE reconciliation_jobs ADD email_error NVARCHAR(1000);
    PRINT 'Section 2: added column reconciliation_jobs.email_error';
END
ELSE
    PRINT 'Section 2: column reconciliation_jobs.email_error already present';

-- Master on/off switch for all outbound email.
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'smtp_enabled')
BEGIN
    INSERT INTO app_settings (setting_key, setting_value, setting_group)
    VALUES ('smtp_enabled', 'true', 'SMTP');
    PRINT 'Section 2: added setting smtp_enabled = true';
END

-- SMTP connect/read/write timeout, so a mail problem can never hang a job.
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'smtp_timeout_seconds')
BEGIN
    INSERT INTO app_settings (setting_key, setting_value, setting_group)
    VALUES ('smtp_timeout_seconds', '20', 'SMTP');
    PRINT 'Section 2: added setting smtp_timeout_seconds = 20';
END

-- Hard wall-clock limit per job; the watchdog fails the job and frees its worker thread.
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'job_timeout_minutes')
BEGIN
    INSERT INTO app_settings (setting_key, setting_value, setting_group)
    VALUES ('job_timeout_minutes', '120', 'JOB');
    PRINT 'Section 2: added setting job_timeout_minutes = 120';
END

COMMIT TRANSACTION;

/* ---------------------------------------------------------------------------
   Verification - all four rows below should be returned
   --------------------------------------------------------------------------- */

SELECT 'INVOICE page_limit' AS item, CAST(page_limit AS VARCHAR(10)) AS value
FROM document_types WHERE type_code = 'INVOICE'
UNION ALL
SELECT 'column email_error', CASE WHEN EXISTS (SELECT * FROM sys.columns
        WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'email_error')
    THEN 'present' ELSE '*** MISSING ***' END
UNION ALL
SELECT 'setting ' + setting_key, setting_value
FROM app_settings
WHERE setting_key IN ('smtp_enabled', 'smtp_timeout_seconds', 'job_timeout_minutes');
