# LC Reconciliation - SMIPL Project Memory
_Last updated: 2026-06-02_

---

## Project Location
- **Source Code:** `E:\Work\Projects\Suzuki\Letter of Credit\Development\lc-reconciliation`
- **Tomcat:** `E:\Work\apache-tomcat-8.5.23-windows-x64\apache-tomcat-8.5.23`
- **Webapps:** `E:\Work\apache-tomcat-8.5.23-windows-x64\apache-tomcat-8.5.23\webapps\`
- **App URL:** `http://localhost:8080/lc-reconciliation`
- **Build:** `mvn clean package` → WAR at `target/lc-reconciliation.war`
- **Deploy:** Copy WAR to webapps, delete old exploded dir, run `startup.bat`

---

## Tech Stack
- Java 8 (compiled), JDK 11 (Eclipse Adoptium at runtime)
- Tomcat 8.5.23
- JSP + JSTL frontend (Bootstrap 5.3.2, Bootstrap Icons)
- SQL Server (MSSQL) database
- Maven WAR packaging

---

## Roles & Access Control

### Roles (stored as strings in DB `users.role` column)
| Role | Value | Access |
|------|-------|--------|
| IT Admin | `IT_ADMIN` | **Full access** — all pages including Settings |
| Super Admin | `SUPER_ADMIN` | All pages **except** Settings |
| Department User | `DEPARTMENT_USER` | Dashboard, LC Upload, Job Monitor, Reports, Email Groups, Document Types only |

### Page Access Matrix
| Page / Feature | IT_ADMIN | SUPER_ADMIN | DEPARTMENT_USER |
|---|---|---|---|
| Settings (`/settings`) | ✅ | ❌ | ❌ |
| User Management (`/master/users`) | ✅ | ✅ | ❌ |
| Prompt Templates (`/master/prompts`) | ✅ | ✅ | ❌ |
| Email Templates (`/master/email-templates`) | ✅ | ✅ | ❌ |
| Email Groups (`/master/email-groups`) | ✅ | ✅ | ✅ |
| Document Types (`/master/document-types`) | ✅ | ✅ | ✅ |
| Dashboard / LC Upload / Job Monitor / Reports | ✅ | ✅ | ✅ (dept-filtered) |

### Session Attributes Set at Login (LoginServlet.java)
```java
session.setAttribute("user", user);
session.setAttribute("username", user.getUsername());
session.setAttribute("fullName", user.getFullName());
session.setAttribute("userEmail", user.getEmail());
session.setAttribute("userRole", user.getRole());
session.setAttribute("userDepartment", user.getDepartment());
session.setAttribute("isItAdmin", user.isItAdmin());       // added in session 2026-06-02
session.setAttribute("isSuperAdmin", user.isSuperAdmin());
```

### AuthFilter logic (AuthFilter.java)
```java
boolean itAdmin = Boolean.TRUE.equals(session.getAttribute("isItAdmin"));
boolean superAdmin = Boolean.TRUE.equals(session.getAttribute("isSuperAdmin"));

// Settings: IT_ADMIN only
if (path.startsWith("/settings")) {
    if (!itAdmin) → 403
}
// User Mgmt, Prompts, Email Templates: IT_ADMIN or SUPER_ADMIN
if (path.startsWith("/master/users") || path.startsWith("/master/prompts")
    || path.startsWith("/master/email-templates")) {
    if (!itAdmin && !superAdmin) → 403
}
```

---

## Document View Feature

### New Servlet: DocumentViewServlet.java
- **URL:** `/document/view?type=lc&id={docId}` or `?type=supporting&id={docId}`
- **Location:** `src/main/java/com/smipl/lcrecon/servlet/DocumentViewServlet.java`
- **Behaviour:** PDF → served `inline` (opens in browser tab); DOCX/XLSX/etc. → `attachment` (download)
- **Auth:** Requires valid session; reads file path from DB by ID

### Where View Buttons Were Added
1. **Job Monitor detail page** (`job-monitor.jsp`) — "View" button in both LC Docs table and Supporting Docs table → links to `/document/view`
2. **LC Upload Step 3 Review** (`upload.js` `populateReview()`) — "View" button using `URL.createObjectURL(file)` for local browser preview before job submission

### New DAO Methods Added (LcDocumentDao.java)
- `findById(long id)` → returns `LcDocument`
- `findSupportingById(long id)` → returns `SupportingDocument`

---

## Microsoft Graph API
- **Token URL:** `https://login.microsoftonline.com/{tenantId}/oauth2/v2.0/token`
- **Graph Base:** `https://graph.microsoft.com/v1.0`
- **Purpose:** Convert non-PDF documents (DOCX, XLSX, PPT, etc.) to PDF via OneDrive
- **Flow:** Upload to OneDrive temp folder → Download as PDF → Delete temp file
- **Config keys in DB settings:** `graph_tenant_id`, `graph_client_id`, `graph_client_secret`, `graph_drive_id`
- **Client:** `src/main/java/com/smipl/lcrecon/integration/GraphApiClient.java`

---

## Key Files Reference

| File | Path |
|------|------|
| AuthFilter | `src/main/java/com/smipl/lcrecon/filter/AuthFilter.java` |
| LoginServlet | `src/main/java/com/smipl/lcrecon/servlet/LoginServlet.java` |
| User model | `src/main/java/com/smipl/lcrecon/model/User.java` |
| LcDocumentDao | `src/main/java/com/smipl/lcrecon/dao/LcDocumentDao.java` |
| DocumentViewServlet | `src/main/java/com/smipl/lcrecon/servlet/DocumentViewServlet.java` |
| UserManagementServlet | `src/main/java/com/smipl/lcrecon/servlet/UserManagementServlet.java` |
| LcUploadServlet | `src/main/java/com/smipl/lcrecon/servlet/LcUploadServlet.java` |
| JobMonitorServlet | `src/main/java/com/smipl/lcrecon/servlet/JobMonitorServlet.java` |
| GraphApiClient | `src/main/java/com/smipl/lcrecon/integration/GraphApiClient.java` |
| FileUtil | `src/main/java/com/smipl/lcrecon/util/FileUtil.java` |
| sidebar.jsp | `src/main/webapp/WEB-INF/jsp/layout/sidebar.jsp` |
| header.jsp | `src/main/webapp/WEB-INF/jsp/layout/header.jsp` |
| job-monitor.jsp | `src/main/webapp/WEB-INF/jsp/job-monitor.jsp` |
| lc-upload.jsp | `src/main/webapp/WEB-INF/jsp/lc-upload.jsp` |
| upload.js | `src/main/webapp/static/js/upload.js` |
| users.jsp | `src/main/webapp/WEB-INF/jsp/master/users.jsp` |
| DB schema | `src/main/resources/db/schema.sql` |
| DB seed | `src/main/resources/db/seed.sql` |

---

## Database Tables Summary
| Table | Purpose |
|-------|---------|
| `users` | User accounts, role stored as NVARCHAR(20) |
| `lc_documents` | Uploaded LC / Amendment files |
| `supporting_documents` | Invoice, BL, COO, etc. |
| `reconciliation_jobs` | Job tracking (status, progress, report paths) |
| `reconciliation_results` | Per-parameter reconciliation output |
| `job_logs` | Execution log per job |
| `job_api_costs` | OCR + GPT API cost tracking |
| `document_types` | Master list of supported doc types |
| `email_groups` / `email_group_members` | Notification distribution lists |
| `prompt_templates` | AI prompts per document type |
| `email_templates` | Email body templates |
| `settings` | App config (SMTP, OpenAI, Graph API, etc.) |

---

## Upload Flow Summary
1. **Step 1** — User uploads Master LC + optional Amendments → `POST /lc-upload action=uploadLC` → stored under `{catalinaBase}/lc-uploads/{batchId}/lc/`
2. **Step 2** — User uploads Supporting docs (Invoice, BL, etc.) → `POST /lc-upload action=uploadSupporting` → stored under `{batchId}/supporting/`
3. **Step 3** — Review & Submit → `POST /lc-upload action=startJob` → creates `ReconciliationJob`, submits to `JobManager` thread pool
4. Redirects to `/job-monitor?jobId={id}` for live progress polling

---

## Changes Made in This Session (2026-06-02)
1. **User.java** — Added `isItAdmin()`, updated `getRoleDisplay()` and `getRoleBadgeClass()` for IT_ADMIN
2. **LoginServlet.java** — Added `isItAdmin` session attribute
3. **AuthFilter.java** — Multi-role path protection (3 roles, 3 path groups)
4. **sidebar.jsp** — Role-based nav: Settings for IT_ADMIN only; Prompts/Email Templates/User Mgmt for IT_ADMIN or SUPER_ADMIN
5. **header.jsp** — Dark "IT Admin" badge for IT_ADMIN; red "Admin" badge for SUPER_ADMIN
6. **users.jsp** — Added IT_ADMIN option to role dropdown in Add/Edit User modal
7. **LcDocumentDao.java** — Added `findById()` and `findSupportingById()` methods
8. **DocumentViewServlet.java** — NEW: serves uploaded files at `/document/view`
9. **job-monitor.jsp** — Added "View" button column in LC docs and Supporting docs tables
10. **upload.js** — Step 3 populateReview() now shows View buttons with `URL.createObjectURL()`
