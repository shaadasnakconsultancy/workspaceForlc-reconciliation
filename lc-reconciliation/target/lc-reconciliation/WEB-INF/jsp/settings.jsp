<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Settings - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
</head>
<body>
    <c:set var="pageTitle" value="Settings" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">

            <div id="alertContainer"></div>

            <ul class="nav nav-tabs" id="settingsTabs" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="openai-tab" data-bs-toggle="tab" data-bs-target="#openai-panel"
                            type="button" role="tab" aria-controls="openai-panel" aria-selected="true">
                        <i class="bi bi-robot"></i> Azure OpenAI
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="docintel-tab" data-bs-toggle="tab" data-bs-target="#docintel-panel"
                            type="button" role="tab" aria-controls="docintel-panel" aria-selected="false">
                        <i class="bi bi-file-earmark-text"></i> Document Intelligence
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="smtp-tab" data-bs-toggle="tab" data-bs-target="#smtp-panel"
                            type="button" role="tab" aria-controls="smtp-panel" aria-selected="false">
                        <i class="bi bi-envelope"></i> SMTP
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="graph-tab" data-bs-toggle="tab" data-bs-target="#graph-panel"
                            type="button" role="tab" aria-controls="graph-panel" aria-selected="false">
                        <i class="bi bi-cloud-arrow-up"></i> Graph API
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="sap-tab" data-bs-toggle="tab" data-bs-target="#sap-panel"
                            type="button" role="tab" aria-controls="sap-panel" aria-selected="false">
                        <i class="bi bi-box-seam"></i> SAP (HSN)
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="cost-tab" data-bs-toggle="tab" data-bs-target="#cost-panel"
                            type="button" role="tab" aria-controls="cost-panel" aria-selected="false">
                        <i class="bi bi-currency-rupee"></i> Cost Rates
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="job-tab" data-bs-toggle="tab" data-bs-target="#job-panel"
                            type="button" role="tab" aria-controls="job-panel" aria-selected="false">
                        <i class="bi bi-hourglass-split"></i> Job Execution
                    </button>
                </li>
            </ul>

            <div class="tab-content mt-3" id="settingsTabContent">

                <!-- Azure OpenAI Tab -->
                <div class="tab-pane fade show active" id="openai-panel" role="tabpanel" aria-labelledby="openai-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-robot"></i> Azure OpenAI Configuration</h5></div>
                        <div class="card-body">
                            <form id="openaiForm">
                                <div class="row">
                                    <div class="col-md-12 mb-3">
                                        <label for="openai_endpoint" class="form-label">API URL <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="openai_endpoint" name="openai_endpoint"
                                               value="${openaiSettings['openai_endpoint']}" placeholder="https://api.openai.com/v1/chat/completions" required>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="openai_api_key" class="form-label">API Key <span class="text-danger">*</span></label>
                                        <div class="input-group">
                                            <input type="password" class="form-control" id="openai_api_key" name="openai_api_key"
                                                   value="${openaiSettings['openai_api_key']}" required>
                                            <button class="btn btn-outline-secondary toggle-password" type="button" data-target="openai_api_key">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="openai_model" class="form-label">Model Name <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="openai_model" name="openai_model"
                                               value="${openaiSettings['openai_model']}" placeholder="gpt-4o-mini" required>
                                    </div>
                                </div>
                                <div class="d-flex gap-2">
                                    <button type="button" class="btn btn-outline-info" onclick="testConnection('openai')">
                                        <i class="bi bi-plug"></i> Test Connection
                                    </button>
                                    <button type="submit" class="btn btn-primary">
                                        <i class="bi bi-save"></i> Save Settings
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <!-- Document Intelligence Tab -->
                <div class="tab-pane fade" id="docintel-panel" role="tabpanel" aria-labelledby="docintel-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-file-earmark-text"></i> Azure Document Intelligence Configuration</h5></div>
                        <div class="card-body">
                            <form id="docIntelForm">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="doc_intel_endpoint" class="form-label">Endpoint URL <span class="text-danger">*</span></label>
                                        <input type="url" class="form-control" id="doc_intel_endpoint" name="doc_intel_endpoint"
                                               value="${docIntelSettings['doc_intel_endpoint']}" placeholder="https://your-resource.cognitiveservices.azure.com/" required>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="doc_intel_api_key" class="form-label">API Key <span class="text-danger">*</span></label>
                                        <div class="input-group">
                                            <input type="password" class="form-control" id="doc_intel_api_key" name="doc_intel_api_key"
                                                   value="${docIntelSettings['doc_intel_api_key']}" required>
                                            <button class="btn btn-outline-secondary toggle-password" type="button" data-target="doc_intel_api_key">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="d-flex gap-2">
                                    <button type="button" class="btn btn-outline-info" onclick="testConnection('docintel')">
                                        <i class="bi bi-plug"></i> Test Connection
                                    </button>
                                    <button type="submit" class="btn btn-primary">
                                        <i class="bi bi-save"></i> Save Settings
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <!-- SMTP Tab -->
                <div class="tab-pane fade" id="smtp-panel" role="tabpanel" aria-labelledby="smtp-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-envelope"></i> SMTP Configuration</h5></div>
                        <div class="card-body">
                            <form id="smtpForm">
                                <div class="alert alert-light border d-flex align-items-center gap-3 mb-3">
                                    <div class="form-check form-switch mb-0">
                                        <input class="form-check-input" type="checkbox" role="switch"
                                               id="smtp_enabled" name="smtp_enabled"
                                               ${smtpSettings['smtp_enabled'] == 'false' ? '' : 'checked'}>
                                        <label class="form-check-label fw-bold" for="smtp_enabled">Enable Email Sending</label>
                                    </div>
                                    <span class="text-muted small">
                                        When switched off, the system sends no email at all - no job reports,
                                        no new-user credentials and no password-reset mails. Jobs still run and reports are still generated.
                                    </span>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="smtp_host" class="form-label">SMTP Host <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="smtp_host" name="smtp_host"
                                               value="${smtpSettings['smtp_host']}" placeholder="smtp.gmail.com" required>
                                    </div>
                                    <div class="col-md-3 mb-3">
                                        <label for="smtp_port" class="form-label">Port <span class="text-danger">*</span></label>
                                        <input type="number" class="form-control" id="smtp_port" name="smtp_port"
                                               value="${smtpSettings['smtp_port']}" placeholder="587" required>
                                    </div>
                                    <div class="col-md-3 mb-3">
                                        <label for="smtp_tls_enabled" class="form-label">TLS Enabled</label>
                                        <select class="form-select" id="smtp_tls_enabled" name="smtp_tls_enabled">
                                            <option value="true" ${smtpSettings['smtp_tls_enabled'] == 'true' ? 'selected' : ''}>Yes</option>
                                            <option value="false" ${smtpSettings['smtp_tls_enabled'] == 'false' ? 'selected' : ''}>No</option>
                                        </select>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="smtp_username" class="form-label">Username</label>
                                        <input type="text" class="form-control" id="smtp_username" name="smtp_username"
                                               value="${smtpSettings['smtp_username']}">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="smtp_password" class="form-label">Password</label>
                                        <div class="input-group">
                                            <input type="password" class="form-control" id="smtp_password" name="smtp_password"
                                                   value="${smtpSettings['smtp_password']}">
                                            <button class="btn btn-outline-secondary toggle-password" type="button" data-target="smtp_password">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="smtp_from" class="form-label">From Address <span class="text-danger">*</span></label>
                                        <input type="email" class="form-control" id="smtp_from" name="smtp_from"
                                               value="${smtpSettings['smtp_from']}" placeholder="noreply@company.com" required>
                                    </div>
                                    <div class="col-md-3 mb-3">
                                        <label for="smtp_timeout_seconds" class="form-label">Timeout (seconds)</label>
                                        <input type="number" class="form-control" id="smtp_timeout_seconds" name="smtp_timeout_seconds"
                                               min="5" max="300"
                                               value="${empty smtpSettings['smtp_timeout_seconds'] ? '20' : smtpSettings['smtp_timeout_seconds']}">
                                        <div class="form-text">A job gives up on the email after this long and still completes.</div>
                                    </div>
                                    <div class="col-md-3 mb-3">
                                    </div>
                                </div>
                                <div class="d-flex gap-2">
                                    <button type="button" class="btn btn-outline-info" onclick="testConnection('smtp')">
                                        <i class="bi bi-plug"></i> Test Connection
                                    </button>
                                    <button type="submit" class="btn btn-primary">
                                        <i class="bi bi-save"></i> Save Settings
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <!-- Graph API Tab -->
                <div class="tab-pane fade" id="graph-panel" role="tabpanel" aria-labelledby="graph-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-cloud-arrow-up"></i> Microsoft Graph API - File Conversion</h5></div>
                        <div class="card-body">
                            <div class="alert alert-info small mb-3">
                                <i class="bi bi-info-circle"></i> Used to convert non-PDF files (.docx, .xlsx, .xlsm, .msg) to PDF via OneDrive before OCR processing.
                            </div>
                            <div class="accordion mb-3" id="graphSetupGuide">
                                <div class="accordion-item">
                                    <h2 class="accordion-header">
                                        <button class="accordion-button collapsed py-2 small" type="button" data-bs-toggle="collapse" data-bs-target="#graphSetupSteps">
                                            <i class="bi bi-question-circle me-2"></i> How to configure Graph API (Setup Guide)
                                        </button>
                                    </h2>
                                    <div id="graphSetupSteps" class="accordion-collapse collapse" data-bs-parent="#graphSetupGuide">
                                        <div class="accordion-body small">
                                            <ol>
                                                <li><strong>Go to Azure Portal</strong> &rarr; Azure Active Directory &rarr; App registrations &rarr; <strong>New registration</strong></li>
                                                <li>Enter a name (e.g., "LC Recon File Converter"), select <strong>Accounts in this organizational directory only</strong>, click <strong>Register</strong></li>
                                                <li>From the Overview page, copy <strong>Application (client) ID</strong> &rarr; paste as <strong>Client ID</strong> below</li>
                                                <li>Copy <strong>Directory (tenant) ID</strong> &rarr; paste as <strong>Tenant ID</strong> below</li>
                                                <li>Go to <strong>Certificates &amp; secrets</strong> &rarr; New client secret &rarr; copy the <strong>Value</strong> (NOT the Secret ID) &rarr; paste as <strong>Client Secret</strong></li>
                                                <li>Go to <strong>API permissions</strong> &rarr; Add a permission &rarr; Microsoft Graph &rarr; <strong>Application permissions</strong></li>
                                                <li>Search and add: <strong>Files.ReadWrite.All</strong></li>
                                                <li>Click <strong>Grant admin consent</strong> for your organization</li>
                                                <li>The app will upload files to OneDrive temp folder, convert to PDF, download, then delete the temp file</li>
                                            </ol>
                                            <div class="alert alert-warning py-1 mb-0"><i class="bi bi-exclamation-triangle"></i> Make sure to use the secret <strong>Value</strong>, not the Secret ID. The value is only shown once when created.</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <form id="graphForm">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="graph_tenant_id" class="form-label">Tenant ID <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="graph_tenant_id" name="graph_tenant_id"
                                               value="${graphSettings['graph_tenant_id']}" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" required>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="graph_client_id" class="form-label">Client ID (App ID) <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="graph_client_id" name="graph_client_id"
                                               value="${graphSettings['graph_client_id']}" placeholder="xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" required>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="graph_client_secret" class="form-label">Client Secret <span class="text-danger">*</span></label>
                                        <div class="input-group">
                                            <input type="password" class="form-control" id="graph_client_secret" name="graph_client_secret"
                                                   value="${graphSettings['graph_client_secret']}" required>
                                            <button class="btn btn-outline-secondary toggle-password" type="button" data-target="graph_client_secret">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="graph_drive_id" class="form-label">Drive ID <small class="text-muted">(optional, uses default drive if empty)</small></label>
                                        <input type="text" class="form-control" id="graph_drive_id" name="graph_drive_id"
                                               value="${graphSettings['graph_drive_id']}" placeholder="Leave empty for default drive">
                                    </div>
                                </div>
                                <div class="d-flex gap-2">
                                    <button type="button" class="btn btn-outline-info" onclick="testConnection('graph')">
                                        <i class="bi bi-plug"></i> Test Connection
                                    </button>
                                    <button type="submit" class="btn btn-primary">
                                        <i class="bi bi-save"></i> Save Settings
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <!-- SAP (HSN) Tab -->
                <div class="tab-pane fade" id="sap-panel" role="tabpanel" aria-labelledby="sap-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-box-seam"></i> SAP OData API - HSN Code Verification</h5></div>
                        <div class="card-body">
                            <div class="alert alert-info small mb-3">
                                <i class="bi bi-info-circle"></i> During reconciliation, HSN codes declared in the LC (clause 45A) are verified against SAP.
                                The invoice number extracted from the Invoice document is passed to SAP as <code>xblnr</code>, and each LC HSN code must be present in the SAP response.
                            </div>
                            <form id="sapForm">
                                <div class="row">
                                    <div class="col-md-9 mb-3">
                                        <label for="sap_base_url" class="form-label">SAP OData Base URL <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="sap_base_url" name="sap_base_url"
                                               value="${sapSettings['sap_base_url']}"
                                               placeholder="https://host:44300/sap/opu/odata/sap/ZVINCSD_ZINCSDTBDPARTS_CDS/ZVINCSD_ZINCSDTBDPARTS">
                                        <small class="text-muted">Service entity URL without query string. The app appends <code>?sap-client=..&amp;$filter=xblnr eq '&lt;invoice&gt;'&amp;$format=json</code>.</small>
                                    </div>
                                    <div class="col-md-3 mb-3">
                                        <label for="sap_client" class="form-label">SAP Client</label>
                                        <input type="text" class="form-control" id="sap_client" name="sap_client"
                                               value="${sapSettings['sap_client']}" placeholder="110">
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="sap_username" class="form-label">Username <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="sap_username" name="sap_username"
                                               value="${sapSettings['sap_username']}" autocomplete="off">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="sap_password" class="form-label">Password <span class="text-danger">*</span></label>
                                        <div class="input-group">
                                            <input type="password" class="form-control" id="sap_password" name="sap_password"
                                                   value="${sapSettings['sap_password']}" autocomplete="new-password">
                                            <button class="btn btn-outline-secondary toggle-password" type="button" data-target="sap_password">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-4 mb-3">
                                        <label for="sap_enabled" class="form-label">HSN Check Enabled</label>
                                        <select class="form-select" id="sap_enabled" name="sap_enabled">
                                            <option value="true" ${sapSettings['sap_enabled'] == 'true' ? 'selected' : ''}>Yes</option>
                                            <option value="false" ${sapSettings['sap_enabled'] == 'false' ? 'selected' : ''}>No</option>
                                        </select>
                                    </div>
                                    <div class="col-md-4 mb-3">
                                        <label for="hsn_source" class="form-label">HSN Comparison Source</label>
                                        <select class="form-select" id="hsn_source" name="hsn_source">
                                            <option value="SAP" ${sapSettings['hsn_source'] == 'SAP' ? 'selected' : ''}>SAP API</option>
                                            <option value="INVOICE" ${sapSettings['hsn_source'] == 'INVOICE' ? 'selected' : ''}>Uploaded Invoice (page 2+)</option>
                                        </select>
                                        <small class="text-muted">SAP: compare LC HSN vs SAP API. Invoice: extract HSN from the uploaded invoice (page 2 onward) using open-source PDF text extraction — no OCR cost.</small>
                                    </div>
                                </div>
                                <div class="d-flex gap-2">
                                    <button type="button" class="btn btn-outline-info" onclick="testConnection('sap')">
                                        <i class="bi bi-plug"></i> Test Connection
                                    </button>
                                    <button type="submit" class="btn btn-primary">
                                        <i class="bi bi-save"></i> Save Settings
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>

                <!-- Cost Rates Tab -->
                <div class="tab-pane fade" id="cost-panel" role="tabpanel" aria-labelledby="cost-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-currency-rupee"></i> API Cost Rates (USD)</h5></div>
                        <div class="card-body">
                            <div class="alert alert-info small">
                                <i class="bi bi-info-circle"></i> Enter rates in <strong>USD</strong> as published by providers. The system will auto-convert to INR using the exchange rate below. Costs are calculated automatically from API usage data.
                            </div>
                            <form id="costForm">
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="cost_openai_input_per_1m_tokens" class="form-label">OpenAI Input Cost (USD per 1M tokens)</label>
                                        <input type="number" step="any" class="form-control" id="cost_openai_input_per_1m_tokens" name="cost_openai_input_per_1m_tokens"
                                               value="${costSettings['cost_openai_input_per_1m_tokens']}" placeholder="1.25">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="cost_openai_output_per_1m_tokens" class="form-label">OpenAI Output Cost (USD per 1M tokens)</label>
                                        <input type="number" step="any" class="form-control" id="cost_openai_output_per_1m_tokens" name="cost_openai_output_per_1m_tokens"
                                               value="${costSettings['cost_openai_output_per_1m_tokens']}" placeholder="5.00">
                                    </div>
                                </div>
                                <div class="row">
                                    <div class="col-md-6 mb-3">
                                        <label for="cost_doc_intel_per_page" class="form-label">Document Intelligence Cost (USD per page)</label>
                                        <input type="number" step="any" class="form-control" id="cost_doc_intel_per_page" name="cost_doc_intel_per_page"
                                               value="${costSettings['cost_doc_intel_per_page']}" placeholder="8.50">
                                    </div>
                                    <div class="col-md-6 mb-3">
                                        <label for="cost_usd_to_inr" class="form-label">USD to INR Exchange Rate</label>
                                        <input type="number" step="any" class="form-control" id="cost_usd_to_inr" name="cost_usd_to_inr"
                                               value="${costSettings['cost_usd_to_inr']}" placeholder="85.00">
                                    </div>
                                </div>
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save"></i> Save Cost Rates
                                </button>
                            </form>
                        </div>
                    </div>
                </div>

                <!-- Job Execution Tab -->
                <div class="tab-pane fade" id="job-panel" role="tabpanel" aria-labelledby="job-tab">
                    <div class="card">
                        <div class="card-header"><h5 class="mb-0"><i class="bi bi-hourglass-split"></i> Job Execution</h5></div>
                        <div class="card-body">
                            <div class="alert alert-info small">
                                <i class="bi bi-info-circle"></i> A reconciliation job that exceeds this limit is stopped
                                automatically, marked <strong>FAILED</strong> with a timeout reason, and its worker thread is
                                released. This is a safety net so one wedged job can never block the queue - set it comfortably
                                above the longest LC you process.
                            </div>
                            <form id="jobForm">
                                <div class="row">
                                    <div class="col-md-4 mb-3">
                                        <label for="job_timeout_minutes" class="form-label">Job Timeout (minutes)</label>
                                        <input type="number" class="form-control" id="job_timeout_minutes" name="job_timeout_minutes"
                                               min="5" max="1440"
                                               value="${empty jobSettings['job_timeout_minutes'] ? '120' : jobSettings['job_timeout_minutes']}">
                                        <div class="form-text">Default 120. Applies to jobs submitted after saving.</div>
                                    </div>
                                </div>
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save"></i> Save Job Settings
                                </button>
                            </form>
                        </div>
                    </div>
                </div>

            </div>
        </div>
        <%@ include file="layout/footer.jsp" %>
    </div>
    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    $(function() {
        // Password toggle
        $('.toggle-password').on('click', function() {
            var targetId = $(this).data('target');
            var input = $('#' + targetId);
            var icon = $(this).find('i');
            if (input.attr('type') === 'password') {
                input.attr('type', 'text');
                icon.removeClass('bi-eye').addClass('bi-eye-slash');
            } else {
                input.attr('type', 'password');
                icon.removeClass('bi-eye-slash').addClass('bi-eye');
            }
        });

        // Save OpenAI settings
        $('#openaiForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('openai', '#openaiForm');
        });

        // Save Document Intelligence settings
        $('#docIntelForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('docintel', '#docIntelForm');
        });

        // Save SMTP settings
        $('#smtpForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('smtp', '#smtpForm');
        });

        // With email switched off the SMTP details may legitimately be blank, so drop the
        // browser-side "required" validation that would otherwise block saving.
        function applySmtpEnabledState() {
            var on = $('#smtp_enabled').is(':checked');
            $('#smtp_host, #smtp_port, #smtp_from').prop('required', on);
        }
        $('#smtp_enabled').on('change', applySmtpEnabledState);
        applySmtpEnabledState();

        // Save Graph API settings
        $('#graphForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('graph', '#graphForm');
        });

        // Save SAP settings
        $('#sapForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('sap', '#sapForm');
        });

        // Save Cost Rate settings
        $('#costForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('cost', '#costForm');
        });

        // Save Job Execution settings
        $('#jobForm').on('submit', function(e) {
            e.preventDefault();
            saveSettings('job', '#jobForm');
        });
    });

    function saveSettings(category, formSelector) {
        var groupMap = { 'openai': 'OPENAI', 'docintel': 'DOC_INTELLIGENCE', 'smtp': 'SMTP', 'graph': 'GRAPH_API', 'sap': 'SAP_API', 'cost': 'COST_RATES', 'job': 'JOB' };
        var group = groupMap[category];
        var settings = {};
        $(formSelector).find('input, select').each(function() {
            var name = $(this).attr('name');
            if (!name) return;
            // A checkbox reports "on" from .val() regardless of state, so read the state itself
            settings[name] = $(this).is(':checkbox') ? ($(this).is(':checked') ? 'true' : 'false') : $(this).val();
        });

        $.ajax({
            url: '${pageContext.request.contextPath}/settings',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ group: group, settings: settings }),
            success: function(response) {
                if (response.success) {
                    showAlert('success', 'Settings saved successfully.');
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function(xhr) {
                showAlert('danger', 'Failed to save settings.');
            }
        });
    }

    function testConnection(category) {
        var formMap = {
            'openai': '#openaiForm', 'docintel': '#docIntelForm', 'smtp': '#smtpForm',
            'graph': '#graphForm', 'sap': '#sapForm'
        };
        var formSelector = formMap[category];
        if (!formSelector) return;

        // Test whatever is on screen now, so settings can be verified before they are saved.
        var settings = {};
        $(formSelector).find('input, select').each(function() {
            var name = $(this).attr('name');
            if (!name) return;
            settings[name] = $(this).is(':checkbox') ? ($(this).is(':checked') ? 'true' : 'false') : $(this).val();
        });

        var $btn = $(formSelector).find('.btn-outline-info');
        var original = $btn.html();
        $btn.prop('disabled', true).html('<span class="spinner-border spinner-border-sm"></span> Testing...');
        showAlert('info', 'Testing connection, please wait...');

        $.ajax({
            url: '${pageContext.request.contextPath}/settings/test-connection',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ type: category, settings: settings }),
            success: function(response) {
                showAlert(response.success ? 'success' : 'danger', response.message);
            },
            error: function(xhr) {
                showAlert('danger', 'Test failed: ' + (xhr.status === 403
                    ? 'you do not have permission to test connections.'
                    : 'the server did not respond (HTTP ' + xhr.status + ').'));
            },
            complete: function() {
                $btn.prop('disabled', false).html(original);
            }
        });
    }

    function showAlert(type, message) {
        var alertHtml = '<div class="alert alert-' + type + ' alert-dismissible fade show" role="alert">'
            + message
            + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
        $('#alertContainer').html(alertHtml);
        setTimeout(function() { $('#alertContainer .alert').alert('close'); }, 5000);
    }
    </script>
</body>
</html>
