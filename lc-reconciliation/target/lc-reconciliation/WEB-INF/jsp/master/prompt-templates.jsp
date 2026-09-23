<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Prompt Templates - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <style>
        .prompt-preview { max-height: 60px; overflow: hidden; font-size: 0.8rem; color: #666; font-family: monospace; white-space: pre-wrap; }
        #editPromptText, #editResponseSchema { font-family: 'Courier New', monospace; font-size: 0.88rem; }
    </style>
</head>
<body>
    <c:set var="pageTitle" value="Prompt Templates" scope="request" />
    <%@ include file="../layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="../layout/header.jsp" %>
        <div class="container-fluid p-4">
            <div id="alertContainer"></div>

            <!-- Prompt List -->
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0"><i class="bi bi-chat-square-text"></i> Prompt Templates</h5>
                    <button class="btn btn-primary btn-sm" onclick="openAddModal()">
                        <i class="bi bi-plus-circle"></i> Add Prompt
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Document Type</th>
                                    <th>Prompt Name</th>
                                    <th>Version</th>
                                    <th>Status</th>
                                    <th>Schema</th>
                                    <th>Prompt Preview</th>
                                    <th>Updated</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="p" items="${prompts}">
                                    <tr class="${p.active ? '' : 'table-secondary'}">
                                        <td><strong>${p.documentTypeName}</strong></td>
                                        <td>${p.promptName}</td>
                                        <td><span class="badge bg-secondary">v${p.version}</span></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${p.active}"><span class="badge bg-success">Active</span></c:when>
                                                <c:otherwise><span class="badge bg-warning text-dark">Inactive</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty p.responseSchema}"><span class="badge bg-info">Defined</span></c:when>
                                                <c:otherwise><span class="badge bg-light text-muted">None</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td><div class="prompt-preview">${p.promptText}</div></td>
                                        <td><small><fmt:formatDate value="${p.updatedAt}" pattern="dd-MMM-yy HH:mm" /></small></td>
                                        <td class="text-nowrap">
                                            <button class="btn btn-sm btn-outline-primary" title="Edit" onclick="editPrompt(${p.id})">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <c:if test="${!p.active}">
                                                <button class="btn btn-sm btn-outline-success" title="Activate" onclick="toggleStatus(${p.id}, ${p.documentTypeId}, true)">
                                                    <i class="bi bi-check-circle"></i>
                                                </button>
                                            </c:if>
                                            <c:if test="${p.active}">
                                                <button class="btn btn-sm btn-outline-warning" title="Deactivate" onclick="toggleStatus(${p.id}, ${p.documentTypeId}, false)">
                                                    <i class="bi bi-x-circle"></i>
                                                </button>
                                            </c:if>
                                            <button class="btn btn-sm btn-outline-danger" title="Delete" onclick="deletePrompt(${p.id}, '${p.promptName}')">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty prompts}">
                                    <tr><td colspan="8" class="text-center text-muted py-4">No prompt templates found.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
        <%@ include file="../layout/footer.jsp" %>
    </div>

    <!-- Add/Edit Prompt Modal -->
    <div class="modal fade" id="promptModal" tabindex="-1">
        <div class="modal-dialog modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="promptModalTitle">Add Prompt Template</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="editId">
                    <div class="row mb-3">
                        <div class="col-md-4">
                            <label class="form-label">Document Type <span class="text-danger">*</span></label>
                            <select class="form-select" id="editDocTypeId">
                                <option value="">-- Select --</option>
                                <c:forEach var="dt" items="${documentTypes}">
                                    <option value="${dt.id}">${dt.typeName} (${dt.typeCode})</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-md-5">
                            <label class="form-label">Prompt Name <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="editPromptName">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label">Version</label>
                            <input type="number" class="form-control" id="editVersion" value="1" min="1" readonly>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold"><i class="bi bi-chat-square-text"></i> System Prompt <span class="text-danger">*</span></label>
                        <textarea class="form-control" id="editPromptText" rows="15" placeholder="Enter the system prompt text..."></textarea>
                        <div class="text-end text-muted small mt-1"><span id="editCharCount">0</span> characters</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-bold"><i class="bi bi-braces"></i> Response Schema (JSON) <small class="text-muted fw-normal">- Structured output format for OpenAI</small></label>
                        <textarea class="form-control" id="editResponseSchema" rows="6" placeholder='{"type":"object","properties":{"rows":{"type":"array","items":{...}}}}'></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary" onclick="savePrompt()"><i class="bi bi-save"></i> Save</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    var contextPath = '${pageContext.request.contextPath}';
    var allPrompts = [];

    // Cache all prompts data on page load
    <c:forEach var="dt" items="${documentTypes}">
    </c:forEach>

    $(function() {
        $('#editPromptText').on('input', function() {
            $('#editCharCount').text($(this).val().length);
        });

        // Load all prompts into cache for edit
        $.ajax({
            url: contextPath + '/master/prompts?format=allJson',
            type: 'GET',
            success: function(data) { allPrompts = data || []; },
            error: function() { /* will fetch individually on edit */ }
        });
    });

    function openAddModal() {
        $('#promptModalTitle').text('Add Prompt Template');
        $('#editId').val('');
        $('#editDocTypeId').val('').prop('disabled', false);
        $('#editPromptName').val('');
        $('#editVersion').val('1');
        $('#editPromptText').val('');
        $('#editResponseSchema').val('');
        $('#editCharCount').text('0');
        new bootstrap.Modal('#promptModal').show();
    }

    function editPrompt(promptId) {
        // Fetch prompt by loading all for its doc type
        $.ajax({
            url: contextPath + '/master/prompts?format=byId&promptId=' + promptId,
            type: 'GET',
            success: function(data) {
                if (data) {
                    $('#promptModalTitle').text('Edit Prompt Template');
                    $('#editId').val(data.id);
                    $('#editDocTypeId').val(data.documentTypeId).prop('disabled', true);
                    $('#editPromptName').val(data.promptName);
                    $('#editVersion').val(data.version);
                    $('#editPromptText').val(data.promptText);
                    $('#editResponseSchema').val(data.responseSchema || '');
                    $('#editCharCount').text(data.promptText ? data.promptText.length : 0);
                    new bootstrap.Modal('#promptModal').show();
                } else {
                    showAlert('danger', 'Prompt not found.');
                }
            },
            error: function() { showAlert('danger', 'Failed to load prompt.'); }
        });
    }

    function savePrompt() {
        var id = $('#editId').val();
        var isNew = !id;
        var data = {
            action: isNew ? 'create' : 'update',
            documentTypeId: parseInt($('#editDocTypeId').val()),
            promptName: $('#editPromptName').val(),
            promptText: $('#editPromptText').val(),
            responseSchema: $('#editResponseSchema').val().trim() || null,
            version: parseInt($('#editVersion').val())
        };

        if (!data.documentTypeId || !data.promptName || !data.promptText) {
            showAlert('danger', 'Please fill all required fields.');
            return;
        }

        if (!isNew) data.id = parseInt(id);

        $.ajax({
            url: contextPath + '/master/prompts',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(res) {
                if (res.success) {
                    bootstrap.Modal.getInstance(document.getElementById('promptModal')).hide();
                    showAlert('success', res.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', res.message);
                }
            },
            error: function() { showAlert('danger', 'Failed to save.'); }
        });
    }

    function toggleStatus(promptId, docTypeId, activate) {
        var action = activate ? 'activate' : 'deactivate';
        var msg = activate
            ? 'This will activate this prompt and deactivate other versions for this document type. Continue?'
            : 'This will deactivate this prompt. Continue?';
        if (!confirm(msg)) return;

        $.ajax({
            url: contextPath + '/master/prompts',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: action, id: promptId, documentTypeId: docTypeId }),
            success: function(res) {
                if (res.success) {
                    showAlert('success', res.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', res.message);
                }
            },
            error: function() { showAlert('danger', 'Failed to update status.'); }
        });
    }

    function deletePrompt(promptId, name) {
        if (!confirm('Delete prompt "' + name + '"? This cannot be undone.')) return;

        $.ajax({
            url: contextPath + '/master/prompts',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'delete', id: promptId }),
            success: function(res) {
                if (res.success) {
                    showAlert('success', res.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', res.message);
                }
            },
            error: function() { showAlert('danger', 'Failed to delete.'); }
        });
    }

    function showAlert(type, message) {
        var html = '<div class="alert alert-' + type + ' alert-dismissible fade show">' + message +
            '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
        $('#alertContainer').html(html);
        setTimeout(function() { $('#alertContainer .alert').alert('close'); }, 5000);
    }
    </script>
</body>
</html>
