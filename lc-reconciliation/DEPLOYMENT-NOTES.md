# LC Reconciliation — Deployment Notes

**Build date:** 2026-08-12
**Artifact:** `target/lc-reconciliation.war`

---

## 1. Database changes

### Applied automatically at startup

`AppContextListener` runs `db/schema.sql` then `db/seed.sql` on every boot. Both are idempotent, so
a normal deployment needs **no manual DB work** — provided the application's SQL account can run
`ALTER TABLE` and `INSERT`.

| Change | Object | Purpose |
|---|---|---|
| New column | `reconciliation_jobs.email_error NVARCHAR(1000) NULL` | Why the notification email was not sent |
| New setting | `smtp_enabled` = `true` (group `SMTP`) | Master on/off switch for all outbound email |
| New setting | `smtp_timeout_seconds` = `20` (group `SMTP`) | SMTP connect/read/write timeout |
| New setting | `job_timeout_minutes` = `120` (group `JOB`) | Hard wall-clock limit per job |

All defaults preserve existing behaviour: email stays enabled, and no job today runs near 120 minutes.

### IMPORTANT — verify the column exists after the first startup

If the DB account lacks `ALTER TABLE`, the statement fails but the script continues (it only logs a
warning). Every job query then throws while reading `email_error`, which shows up as an **empty Job
Monitor** and a **broken Full Report page** — the count is right but no rows appear.

```sql
SELECT name FROM sys.columns
WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'email_error';
```

Returns nothing? Apply manually:

```sql
IF NOT EXISTS (SELECT * FROM sys.columns
               WHERE object_id = OBJECT_ID('reconciliation_jobs') AND name = 'email_error')
    ALTER TABLE reconciliation_jobs ADD email_error NVARCHAR(1000);
```

### Manual fallback for the settings rows

```sql
IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'smtp_enabled')
    INSERT INTO app_settings (setting_key, setting_value, setting_group)
    VALUES ('smtp_enabled', 'true', 'SMTP');

IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'smtp_timeout_seconds')
    INSERT INTO app_settings (setting_key, setting_value, setting_group)
    VALUES ('smtp_timeout_seconds', '20', 'SMTP');

IF NOT EXISTS (SELECT 1 FROM app_settings WHERE setting_key = 'job_timeout_minutes')
    INSERT INTO app_settings (setting_key, setting_value, setting_group)
    VALUES ('job_timeout_minutes', '120', 'JOB');
```

Missing settings are not fatal — the code defaults to enabled / 20s / 120min — but the Settings page
shows blanks until the rows exist.

### No data migration, no destructive change

The column is additive and nullable. Existing job rows keep `email_error = NULL` and display the old
grey **No** badge. Nothing is dropped, renamed or back-filled.

---

## 2. Code changes — 30 files

### New files (2)

| File | Purpose |
|---|---|
| `util/HttpClientFactory.java` | HTTP clients that always carry timeouts |
| `servlet/TestConnectionServlet.java` | Backs the Settings "Test Connection" buttons (`/settings/test-connection`) |

### Email

| File | Change |
|---|---|
| `service/EmailService.java` | Rewritten. `smtp_enabled` gate, connect/read/write timeouts, `EmailResult` returned instead of throwing, human-readable failure classification, `testConnection()` |
| `service/UserEmailService.java` | Same gate + timeouts; returns success/failure instead of swallowing |

### Job execution & reliability

| File | Change |
|---|---|
| `job/JobManager.java` | Rewritten. One worker thread per job (was two), abort flag + direct thread interrupt, watchdog on a separate scheduled pool, named daemon threads |
| `job/ReconciliationJobRunner.java` | Email step cannot fail the job; records the reason; abort checkpoints added around report/email; `resolveRecipients()` |
| `integration/AzureOpenAIClient.java` | Timeouts; `testConnection()` now sends a schema and returns the real error |
| `integration/AzureDocIntelligenceClient.java` | Timeouts; `testConnection()` derives the service root correctly and returns the real error |
| `integration/GraphApiClient.java` | Timeouts on all 5 calls |

### Data access & models

| File | Change |
|---|---|
| `dao/JobDao.java` | `email_error` added to all 7 SELECT column lists; `updateEmailSent(id, sent, error)`; `rerunJob` clears the column |
| `dao/BaseDao.java` | `truncate()` helper |
| `model/ReconciliationJob.java` | `emailError` field |
| `util/JsonUtil.java` | `asLong` / `asInt` / `asBoolean` lenient readers; `writeServerError()` with reference code |

### Servlets — 33 unsafe casts replaced

`DocumentTypeMasterServlet`, `PromptMasterServlet`, `EmailTemplateServlet`, `EmailGroupServlet`,
`UserManagementServlet`, `JobMonitorServlet` — every `((Number) body.get(...))` and
`(Boolean) body.get(...)` now goes through the lenient readers. This fixes master-data
**delete** failing with *"An error occurred while processing your request"*, and the same latent
crash in Job Monitor's **abort** and **rerun**.

Also: `SettingsServlet` (JOB group), `ChangePasswordServlet` (signs out after change).

### JSP

| File | Change |
|---|---|
| `settings.jsp` | Email on/off switch, SMTP timeout field, new **Job Execution** tab, working Test Connection |
| `job-monitor.jsp` | Amber "Not sent" badge with reason |
| `lc-upload.jsp` | Removed hidden auto-recipient; relabelled email group dropdown |
| `layout/header.jsp` | Change Password signs out |
| `master/users.jsp` | Resetting your own password redirects to login |

### HSN — no functional change

`InvoiceHsnExtractor` was briefly changed to scan all pages, then **reverted on request**. It still
starts at page 2 with the single-page guard. Only comments differ from the previous release.

---

## 3. Deployment steps

```bash
mvn clean package
```

1. Stop Tomcat.
2. Back up the current `webapps/lc-reconciliation.war` (needed for rollback).
3. Delete the exploded `webapps/lc-reconciliation/` directory.
4. Copy the new WAR into `webapps/`.
5. Start Tomcat.
6. Confirm in `catalina.out`:
   - `SQL script executed: db/schema.sql`
   - `JobManager initialized with 3 worker threads`
   - `LC Reconciliation System started successfully`
7. Run the `email_error` column check from section 1.

**Sessions are destroyed by the redeploy — all users must log in again.**

---

## 4. Post-deployment smoke test

| # | Check | Expected |
|---|---|---|
| 1 | Open Job Monitor | Existing jobs listed (**not** an empty table with a non-zero count) |
| 2 | Open Full Report | Loads |
| 3 | Settings → SMTP | Toggle + Timeout field render |
| 4 | Settings → Job Execution | New tab renders, shows 120 |
| 5 | Test Connection ×5 | Each returns a specific message |
| 6 | Master data: add + delete a document type | Both succeed |
| 7 | Run one job with **no** group | Completes; email goes to the submitter only |
| 8 | Run one job **with** a group | Completes; group members + submitter, no duplicates |
| 9 | Open a completed job | "Email Sent" shows green Yes, or amber **Not sent** with a reason |
| 10 | Abort a running job | Stops; status ABORTED |
| 11 | Reset your own password (User Management) | Redirected to login; new password required |

Any generic error now carries a reference code, e.g. `(Ref: 4FCD4671)`. Search `catalina.out` for
that code to get the exact stack trace.

---

## 5. Behaviour changes to communicate to users

1. **Whoever schedules a job always receives the report email**, whether or not a group is selected.
   Selecting a group *adds* its members. Previously the report went to the submitter via a hidden
   field regardless, and a group replaced rather than supplemented that.
2. **Resetting your own password signs you out** — both from User Management and the header
   Change Password dialog.
3. **Jobs are capped at 120 minutes** and marked FAILED with a timeout reason beyond that.
4. **Email can now be switched off entirely** — Settings → SMTP. Off means no job reports, no
   new-user credentials, no password-reset mails. Jobs still run and reports are still generated.
5. **A failing mail server no longer hangs a job.** It gives up after 20 seconds; the job completes
   with its reports and the Job Monitor states why the email did not go.

---

## 6. Rollback

Restore the previous WAR and restart. **No DB rollback is required** — the `email_error` column is
additive and the previous build's queries never reference it, so leaving it in place is harmless.
The three new settings rows are equally inert to the old build.

---

## 7. Known gaps — not fixed by this release

**The original on-prem problem is contained, not solved.** Jobs will no longer hang, and the reason
is now visible, but mail still will not send from the client server until:

- outbound TCP to the SMTP host on port 587 is open from that server, and
- *Authenticated SMTP* is enabled on the sender mailbox in the Microsoft 365 admin centre.

Confirm on the server with:

```bash
powershell -Command "$c=New-Object Net.Sockets.TcpClient; $c.Connect('smtp.office365.com',587); $r=New-Object IO.StreamReader($c.GetStream()); Write-Host $r.ReadLine()"
```

A `220 ... Microsoft ESMTP MAIL Service ready` reply means the network path is good. After deploying,
the **Test Connection** button on the SMTP tab does the same check including authentication.

### Not exercised at runtime before shipping

No end-to-end reconciliation job was run against this build. The following are compile-verified and
code-reviewed but never executed: the abort path, the 120-minute watchdog, the new HTTP timeouts,
the recipient rule, the amber badge, and the three JSP pages listed in section 2. Prioritise items
3, 4, 7, 8, 9 and 11 in the smoke test.
