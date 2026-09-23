<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Email Templates - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <style>
        .subject-preview { max-width: 300px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        #bodyTemplate { font-family: 'Courier New', monospace; font-size: 0.88rem; }
        .placeholder-code { font-family: monospace; font-size: 0.85rem; color: #0d6efd; background: #f0f4ff; padding: 1px 5px; border-radius: 3px; }
    </style>
</head>
<body>
    <c:set var="pageTitle" value="Email Templates" scope="request" />
    <%@ include file="../layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="../layout/header.jsp" %>
        <div class="container-fluid p-4">

            <div id="alertContainer"></div>

            <!-- Templates Table -->
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0"><i class="bi bi-envelope-paper"></i> Email Templates</h5>
                    <button class="btn btn-primary btn-sm" onclick="openTemplateModal()">
                        <i class="bi bi-plus-circle"></i> Add Template
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0" id="templatesTable">
                            <thead class="table-light">
                                <tr>
                                    <th>Template Name</th>
                                    <th>Subject Preview</th>
                                    <th>Default</th>
                                    <th>Active</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="t" items="${emailTemplates}">
                                    <tr class="${t.active ? '' : 'table-secondary'}">
                                        <td>
                                            <strong>${t.templateName}</strong>
                                            <c:if test="${t.defaultTemplate}">
                                                <span class="badge bg-warning text-dark ms-1"><i class="bi bi-star-fill"></i> Default</span>
                                            </c:if>
                                        </td>
                                        <td><div class="subject-preview text-muted">${t.subjectTemplate}</div></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${t.defaultTemplate}">
                                                    <span class="badge bg-warning text-dark"><i class="bi bi-star-fill"></i> Yes</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-light text-muted">No</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${t.active}">
                                                    <span class="badge bg-success">Active</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">Inactive</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-nowrap">
                                            <button class="btn btn-sm btn-outline-primary" title="Edit"
                                                    onclick="editTemplate(${t.id})">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <c:if test="${!t.defaultTemplate && t.active}">
                                                <button class="btn btn-sm btn-outline-warning" title="Set as Default"
                                                        onclick="setDefault(${t.id}, '${t.templateName}')">
                                                    <i class="bi bi-star"></i>
                                                </button>
                                            </c:if>
                                            <button class="btn btn-sm btn-outline-danger" title="Delete"
                                                    onclick="deleteTemplate(${t.id}, '${t.templateName}')">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty emailTemplates}">
                                    <tr><td colspan="5" class="text-center text-muted py-4">No email templates configured. Click "Add Template" to create one.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

        </div>
        <%@ include file="../layout/footer.jsp" %>
    </div>

    <!-- Add/Edit Template Modal -->
    <div class="modal fade" id="templateModal" tabindex="-1" aria-labelledby="templateModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="templateModalLabel">Add Email Template</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form id="templateForm">
                    <div class="modal-body">
                        <input type="hidden" id="templateId" name="id">
                        <div class="row">
                            <div class="col-md-8">
                                <div class="mb-3">
                                    <label for="templateName" class="form-label">Template Name <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control" id="templateName" name="templateName"
                                           placeholder="e.g. LC Non-Compliance Notification" required maxlength="100">
                                </div>
                                <div class="mb-3">
                                    <label for="subjectTemplate" class="form-label">Subject Template <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control" id="subjectTemplate" name="subjectTemplate"
                                           placeholder="e.g. LC Reconciliation Report - {{LC_NUMBER}} - {{STATUS}}" required maxlength="500">
                                    <div class="form-text">Use placeholders like <code>{{LC_NUMBER}}</code> in the subject line.</div>
                                </div>
                                <div class="mb-3">
                                    <label for="bodyTemplate" class="form-label">Body Template <span class="text-danger">*</span></label>
                                    <textarea class="form-control" id="bodyTemplate" name="bodyTemplate" rows="15"
                                              placeholder="Enter the email body template with placeholders..." required></textarea>
                                </div>
                                <div class="mb-3 form-check">
                                    <input type="checkbox" class="form-check-input" id="templateActive" name="active" checked>
                                    <label class="form-check-label" for="templateActive">Active</label>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <!-- Available Placeholders -->
                                <div class="card bg-light">
                                    <div class="card-header py-2">
                                        <h6 class="mb-0"><i class="bi bi-braces"></i> Available Placeholders</h6>
                                    </div>
                                    <div class="card-body py-2">
                                        <table class="table table-sm table-borderless mb-0" style="font-size: 0.82rem;">
                                            <tbody>
                                                <tr>
                                                    <td><span class="placeholder-code">{{JOB_ID}}</span></td>
                                                    <td class="text-muted">Job ID</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{JOB_NAME}}</span></td>
                                                    <td class="text-muted">Job Name</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{LC_NUMBER}}</span></td>
                                                    <td class="text-muted">LC Number</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{SHIPMENT_DOC_NAME}}</span></td>
                                                    <td class="text-muted">Shipment Document Name</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{STATUS}}</span></td>
                                                    <td class="text-muted">Job Status</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{COMPLIED_COUNT}}</span></td>
                                                    <td class="text-muted">Complied Parameters Count</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{NON_COMPLIED_COUNT}}</span></td>
                                                    <td class="text-muted">Non-Complied Count</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{TOTAL_COUNT}}</span></td>
                                                    <td class="text-muted">Total Parameters</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{START_DATE}}</span></td>
                                                    <td class="text-muted">Job Start Date</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{END_DATE}}</span></td>
                                                    <td class="text-muted">Job End Date</td>
                                                </tr>
                                                <tr>
                                                    <td><span class="placeholder-code">{{CREATED_BY}}</span></td>
                                                    <td class="text-muted">Created By User</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                        <div class="text-muted small mt-2">
                                            <i class="bi bi-info-circle"></i> Click a placeholder to copy it.
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary" id="saveTemplateBtn">
                            <i class="bi bi-save"></i> Save
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    var contextPath = '${pageContext.request.contextPath}';
    var templateModal = null;

    $(function() {
        templateModal = new bootstrap.Modal(document.getElementById('templateModal'));

        $('#templateForm').on('submit', function(e) {
            e.preventDefault();
            saveTemplate();
        });

        // Click placeholder to copy
        $('.placeholder-code').on('click', function() {
            var text = $(this).text();
            navigator.clipboard.writeText(text).then(function() {
                showAlert('info', 'Copied: ' + text);
            });
        }).css('cursor', 'pointer');
    });

    // ---- Template CRUD ----

    function openTemplateModal() {
        $('#templateModalLabel').text('Add Email Template');
        $('#templateForm')[0].reset();
        $('#templateId').val('');
        $('#templateActive').prop('checked', true);
        templateModal.show();
    }

    function editTemplate(id) {
        $.ajax({
            url: contextPath + '/master/email-templates?format=byId&templateId=' + id,
            type: 'GET',
            success: function(data) {
                if (data) {
                    $('#templateModalLabel').text('Edit Email Template');
                    $('#templateId').val(data.id);
                    $('#templateName').val(data.templateName);
                    $('#subjectTemplate').val(data.subjectTemplate);
                    $('#bodyTemplate').val(data.bodyTemplate);
                    $('#templateActive').prop('checked', data.active);
                    templateModal.show();
                } else {
                    showAlert('danger', 'Template not found.');
                }
            },
            error: function() {
                showAlert('danger', 'Failed to load template.');
            }
        });
    }

    function saveTemplate() {
        var id = $('#templateId').val();
        var data = {
            action: id ? 'update' : 'create',
            templateName: $('#templateName').val().trim(),
            subjectTemplate: $('#subjectTemplate').val().trim(),
            bodyTemplate: $('#bodyTemplate').val().trim(),
            isActive: $('#templateActive').is(':checked')
        };

        if (!data.templateName || !data.subjectTemplate || !data.bodyTemplate) {
            showAlert('danger', 'Please fill all required fields.');
            return;
        }

        if (id) data.id = parseInt(id);

        $.ajax({
            url: contextPath + '/master/email-templates',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                if (response.success) {
                    templateModal.hide();
                    showAlert('success', response.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to save template.');
            }
        });
    }

    function setDefault(id, name) {
        if (!confirm('Set "' + name + '" as the default email template? The current default will be unset.')) return;

        $.ajax({
            url: contextPath + '/master/email-templates',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'setDefault', id: id }),
            success: function(response) {
                if (response.success) {
                    showAlert('success', response.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to set default template.');
            }
        });
    }

    function deleteTemplate(id, name) {
        if (!confirm('Are you sure you want to delete email template "' + name + '"?')) return;

        $.ajax({
            url: contextPath + '/master/email-templates',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'delete', id: id }),
            success: function(response) {
                if (response.success) {
                    showAlert('success', response.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to delete template.');
            }
        });
    }

    // ---- Utilities ----

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
