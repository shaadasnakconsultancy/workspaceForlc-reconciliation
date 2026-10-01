<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Document Types - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
</head>
<body>
    <c:set var="pageTitle" value="Document Types" scope="request" />
    <%@ include file="../layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="../layout/header.jsp" %>
        <div class="container-fluid p-4">

            <div id="alertContainer"></div>

            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0"><i class="bi bi-file-earmark-ruled"></i> Document Types</h5>
                    <button class="btn btn-primary btn-sm" onclick="openModal()">
                        <i class="bi bi-plus-circle"></i> Add Document Type
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0" id="docTypesTable">
                            <thead class="table-light">
                                <tr>
                                    <th>Code</th>
                                    <th>Name</th>
                                    <th>Page Limit</th>
                                    <th>Active</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="dt" items="${documentTypes}">
                                    <tr data-id="${dt.id}">
                                        <td><code><c:out value="${dt.typeCode}"/></code></td>
                                        <td><c:out value="${dt.typeName}"/></td>
                                        <td>${dt.pageLimit}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${dt.active}">
                                                    <span class="badge bg-success">Active</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">Inactive</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <button type="button" class="btn btn-sm btn-outline-primary btn-edit-dt" title="Edit"
                                                    data-id="${dt.id}" data-code="<c:out value='${dt.typeCode}'/>"
                                                    data-name="<c:out value='${dt.typeName}'/>" data-pagelimit="${dt.pageLimit}"
                                                    data-order="${dt.displayOrder}" data-active="${dt.active}">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <button type="button" class="btn btn-sm btn-outline-danger btn-delete-dt" title="Delete"
                                                    data-id="${dt.id}" data-name="<c:out value='${dt.typeName}'/>">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty documentTypes}">
                                    <tr><td colspan="5" class="text-center text-muted py-4">No document types configured. Click "Add Document Type" to create one.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
        <%@ include file="../layout/footer.jsp" %>
    </div>

    <!-- Add/Edit Modal -->
    <div class="modal fade" id="docTypeModal" tabindex="-1" aria-labelledby="docTypeModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="docTypeModalLabel">Add Document Type</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form id="docTypeForm">
                    <div class="modal-body">
                        <input type="hidden" id="docTypeId" name="id">
                        <div class="mb-3">
                            <label for="typeCode" class="form-label">Type Code <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="typeCode" name="typeCode"
                                   placeholder="e.g. BILL_OF_LADING" required maxlength="50">
                            <div class="form-text">Unique identifier code (uppercase with underscores recommended)</div>
                        </div>
                        <div class="mb-3">
                            <label for="typeName" class="form-label">Type Name <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="typeName" name="typeName"
                                   placeholder="e.g. Bill of Lading" required maxlength="100">
                        </div>
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label for="pageLimit" class="form-label">Page Limit</label>
                                <input type="number" class="form-control" id="pageLimit" name="pageLimit"
                                       value="10" min="1" max="100">
                                <div class="form-text">Maximum pages expected for this document type</div>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label for="displayOrder" class="form-label">Display Order</label>
                                <input type="number" class="form-control" id="displayOrder" name="displayOrder"
                                       value="0" min="0">
                            </div>
                        </div>
                        <div class="mb-3 form-check">
                            <input type="checkbox" class="form-check-input" id="activeFlag" name="active" checked>
                            <label class="form-check-label" for="activeFlag">Active</label>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary" id="saveBtn">
                            <i class="bi bi-save"></i> Save
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    var docTypeModal = null;

    $(function() {
        docTypeModal = new bootstrap.Modal(document.getElementById('docTypeModal'));

        $('#docTypeForm').on('submit', function(e) {
            e.preventDefault();
            saveDocType();
        });

        // Delegated handlers read user data from data-* attributes (inert strings, never executed as HTML/JS)
        $('#docTypesTable').on('click', '.btn-edit-dt', function() {
            var d = this.dataset;
            editDocType(d.id, d.code, d.name, d.pagelimit, d.order, d.active === 'true');
        });
        $('#docTypesTable').on('click', '.btn-delete-dt', function() {
            deleteDocType(this.dataset.id, this.dataset.name);
        });
    });

    function openModal() {
        $('#docTypeModalLabel').text('Add Document Type');
        $('#docTypeForm')[0].reset();
        $('#docTypeId').val('');
        $('#typeCode').prop('readonly', false);
        $('#activeFlag').prop('checked', true);
        $('#pageLimit').val(10);
        $('#displayOrder').val(0);
        docTypeModal.show();
    }

    function editDocType(id, typeCode, typeName, pageLimit, displayOrder, active) {
        $('#docTypeModalLabel').text('Edit Document Type');
        $('#docTypeId').val(id);
        $('#typeCode').val(typeCode).prop('readonly', true);
        $('#typeName').val(typeName);
        $('#pageLimit').val(pageLimit);
        $('#displayOrder').val(displayOrder);
        $('#activeFlag').prop('checked', active);
        docTypeModal.show();
    }

    function saveDocType() {
        var id = $('#docTypeId').val();
        var data = {
            action: id ? 'update' : 'create',
            typeCode: $('#typeCode').val(),
            typeName: $('#typeName').val(),
            pageLimit: parseInt($('#pageLimit').val()),
            displayOrder: parseInt($('#displayOrder').val()),
            isActive: $('#activeFlag').is(':checked')
        };
        if (id) data.id = parseInt(id);

        $.ajax({
            url: '${pageContext.request.contextPath}/master/document-types',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                if (response.success) {
                    docTypeModal.hide();
                    showAlert('success', response.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function(xhr) {
                showAlert('danger', 'Failed to save.');
            }
        });
    }

    function deleteDocType(id, name) {
        if (!confirm('Are you sure you want to delete document type "' + name + '"?')) return;

        $.ajax({
            url: '${pageContext.request.contextPath}/master/document-types',
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
            error: function(xhr) {
                showAlert('danger', 'Failed to delete.');
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
