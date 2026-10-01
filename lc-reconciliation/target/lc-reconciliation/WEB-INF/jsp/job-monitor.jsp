<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html><html><head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Job Monitor - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
</head><body>
    <c:set var="pageTitle" value="Job Monitor" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">

            <%-- ==================== DETAIL VIEW ==================== --%>
            <c:if test="${not empty job}">

                <%-- Back button --%>
                <div class="mb-3">
                    <a href="${pageContext.request.contextPath}/job-monitor" class="btn btn-outline-secondary">
                        <i class="bi bi-arrow-left"></i> Back to Job List
                    </a>
                </div>

                <%-- Job Detail Card --%>
                <div class="card mb-4">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0"><i class="bi bi-info-circle"></i> Job #${job.id} - ${job.jobDisplayName}</h5>
                        <span class="badge ${job.statusBadgeClass} fs-6">${job.status}</span>
                    </div>
                    <div class="card-body">
                        <%-- Row 1: LC Number | Job Name (editable) | Shipment Doc Name (editable) --%>
                        <div class="row mb-3">
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">LC Number</label>
                                <div class="fw-bold">${job.lcNumber}</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Job Name</label>
                                <div class="input-group input-group-sm">
                                    <input type="text" class="form-control" id="detailJobName"
                                           value="<c:out value='${job.jobName}'/>" placeholder="Enter job name">
                                    <button class="btn btn-outline-primary" type="button"
                                            onclick="saveJobDetails(${job.id})">
                                        <i class="bi bi-check-lg"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Shipment Doc Name</label>
                                <div class="input-group input-group-sm">
                                    <input type="text" class="form-control" id="detailShipmentDocName"
                                           value="<c:out value='${job.shipmentDocName}'/>" placeholder="Enter shipment doc name">
                                    <button class="btn btn-outline-primary" type="button"
                                            onclick="saveJobDetails(${job.id})">
                                        <i class="bi bi-check-lg"></i>
                                    </button>
                                </div>
                            </div>
                        </div>

                        <%-- Row 2: Created By | Department | Email Sent --%>
                        <div class="row mb-3">
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Created By</label>
                                <div>${job.createdBy}</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Department</label>
                                <div>${job.createdByDepartment}</div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Email Sent</label>
                                <div>
                                    <c:choose>
                                        <c:when test="${job.emailSent}">
                                            <span class="badge bg-success"><i class="bi bi-check-circle"></i> Yes</span>
                                        </c:when>
                                        <c:when test="${not empty job.emailError}">
                                            <span class="badge bg-warning text-dark"><i class="bi bi-exclamation-triangle"></i> Not sent</span>
                                            <div class="small text-muted mt-1"><c:out value="${job.emailError}"/></div>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary"><i class="bi bi-x-circle"></i> No</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>

                        <%-- Row 3: Started At | Completed At | Duration --%>
                        <div class="row mb-3">
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Started At</label>
                                <div>
                                    <c:choose>
                                        <c:when test="${not empty job.startedAt}">
                                            <fmt:formatDate value="${job.startedAt}" pattern="dd-MMM-yyyy HH:mm:ss" />
                                        </c:when>
                                        <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Completed At</label>
                                <div>
                                    <c:choose>
                                        <c:when test="${not empty job.completedAt}">
                                            <fmt:formatDate value="${job.completedAt}" pattern="dd-MMM-yyyy HH:mm:ss" />
                                        </c:when>
                                        <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                            <div class="col-md-4">
                                <label class="form-label text-muted small mb-0">Duration</label>
                                <div id="jobDuration">
                                    <c:choose>
                                        <c:when test="${not empty job.startedAt and not empty job.completedAt}">
                                            <fmt:formatDate value="${job.startedAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="startISO" />
                                            <fmt:formatDate value="${job.completedAt}" pattern="yyyy-MM-dd'T'HH:mm:ss" var="endISO" />
                                            <script>
                                                document.addEventListener('DOMContentLoaded', function() {
                                                    var start = new Date('${startISO}');
                                                    var end = new Date('${endISO}');
                                                    var diff = Math.floor((end - start) / 1000);
                                                    var h = Math.floor(diff / 3600);
                                                    var m = Math.floor((diff % 3600) / 60);
                                                    var s = diff % 60;
                                                    var parts = [];
                                                    if (h > 0) parts.push(h + 'h');
                                                    if (m > 0) parts.push(m + 'm');
                                                    parts.push(s + 's');
                                                    document.getElementById('jobDuration').textContent = parts.join(' ');
                                                });
                                            </script>
                                        </c:when>
                                        <c:when test="${not empty job.startedAt and empty job.completedAt}">
                                            <span class="text-info">In progress...</span>
                                        </c:when>
                                        <c:otherwise><span class="text-muted">-</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>

                        <%-- Progress bar --%>
                        <div class="mb-3">
                            <label class="form-label text-muted small mb-0">Progress</label>
                            <div class="progress mt-1" style="height:25px" id="jobProgress">
                                <div class="progress-bar ${job.status == 'FAILED' ? 'bg-danger' : (job.status == 'RUNNING' || job.status == 'QUEUED') ? 'progress-bar-striped progress-bar-animated' : ''}"
                                     style="width:${job.progressPercent}%" id="jobProgressBar">
                                    ${job.progressPercent}%
                                </div>
                            </div>
                            <small class="text-muted mt-1 d-block" id="jobCurrentStep">
                                <c:if test="${not empty job.currentStep}">
                                    <i class="bi bi-gear"></i> ${job.currentStep}
                                </c:if>
                            </small>
                        </div>

                        <%-- Error message --%>
                        <c:if test="${not empty job.errorMessage}">
                            <div class="alert alert-danger mt-3 mb-3">
                                <i class="bi bi-exclamation-triangle-fill"></i> <strong>Error:</strong>
                                <c:out value="${job.errorMessage}" />
                            </div>
                        </c:if>

                        <%-- Action buttons --%>
                        <div class="mt-3">
                            <c:if test="${job.status == 'COMPLETED'}">
                                <a href="${pageContext.request.contextPath}/report?jobId=${job.id}" class="btn btn-success">
                                    <i class="bi bi-file-earmark-text"></i> View Full Report
                                </a>
                                <a href="${pageContext.request.contextPath}/non-compliance?jobId=${job.id}" class="btn btn-warning">
                                    <i class="bi bi-exclamation-triangle"></i> View Non-Compliance
                                </a>
                                <a href="${pageContext.request.contextPath}/report?jobId=${job.id}&format=html" class="btn btn-outline-primary">
                                    <i class="bi bi-filetype-html"></i> Download HTML
                                </a>
                                <c:if test="${not empty job.xlsxReportPath}">
                                    <a href="${pageContext.request.contextPath}/report?jobId=${job.id}&format=xlsx" class="btn btn-outline-success">
                                        <i class="bi bi-filetype-xlsx"></i> Download XLSX
                                    </a>
                                </c:if>
                            </c:if>
                            <c:if test="${job.status == 'RUNNING' || job.status == 'QUEUED'}">
                                <button class="btn btn-outline-danger" onclick="abortJob(${job.id})">
                                    <i class="bi bi-stop-circle"></i> Abort
                                </button>
                            </c:if>
                            <c:if test="${job.status == 'FAILED' || job.status == 'COMPLETED' || job.status == 'ABORTED'}">
                                <button class="btn btn-outline-info" onclick="rerunJob(${job.id})">
                                    <i class="bi bi-arrow-clockwise"></i> Rerun
                                </button>
                            </c:if>
                        </div>
                    </div>
                </div>

                <%-- Uploaded Documents Card --%>
                <c:if test="${not empty lcDocs || not empty supportingDocs}">
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-folder2-open"></i> Uploaded Documents</h5>
                        </div>
                        <div class="card-body">
                            <%-- LC Documents --%>
                            <c:if test="${not empty lcDocs}">
                                <h6 class="text-muted mb-2">LC Documents</h6>
                                <div class="table-responsive mb-3">
                                    <table class="table table-sm table-bordered mb-0">
                                        <thead class="table-light">
                                            <tr>
                                                <th>File Name</th>
                                                <th>Type</th>
                                                <th>Size</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="doc" items="${lcDocs}">
                                                <tr>
                                                    <td><i class="bi bi-file-earmark-pdf text-danger"></i> ${doc.fileName}</td>
                                                    <td>
                                                        <c:choose>
                                                            <c:when test="${doc.addendum}">
                                                                <span class="badge bg-info">Amendment</span>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <span class="badge bg-primary">Master</span>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <td>
                                                        <fmt:formatNumber value="${doc.fileSize / 1024.0}" maxFractionDigits="1" /> KB
                                                    </td>
                                                    <td>
                                                        <a href="${pageContext.request.contextPath}/document/view?type=lc&id=${doc.id}" target="_blank" class="btn btn-sm btn-outline-secondary" title="View Document">
                                                            <i class="bi bi-eye"></i> View
                                                        </a>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </c:if>

                            <%-- Supporting Documents --%>
                            <c:if test="${not empty supportingDocs}">
                                <h6 class="text-muted mb-2">Supporting Documents</h6>
                                <div class="table-responsive">
                                    <table class="table table-sm table-bordered mb-0">
                                        <thead class="table-light">
                                            <tr>
                                                <th>Document Type</th>
                                                <th>File Name</th>
                                                <th>Size</th>
                                                <th>Page Limit</th>
                                                <th>Action</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="doc" items="${supportingDocs}">
                                                <tr>
                                                    <td><span class="badge bg-secondary"><c:out value="${doc.documentTypeName}"/></span></td>
                                                    <td><i class="bi bi-file-earmark text-primary"></i> ${doc.fileName}</td>
                                                    <td>
                                                        <fmt:formatNumber value="${doc.fileSize / 1024.0}" maxFractionDigits="1" /> KB
                                                    </td>
                                                    <td>
                                                        <c:choose>
                                                            <c:when test="${doc.pageLimit > 0}">${doc.pageLimit}</c:when>
                                                            <c:otherwise><span class="text-muted">All</span></c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                    <td>
                                                        <a href="${pageContext.request.contextPath}/document/view?type=supporting&id=${doc.id}" target="_blank" class="btn btn-sm btn-outline-secondary" title="View Document">
                                                            <i class="bi bi-eye"></i> View
                                                        </a>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </c:if>

                <%-- Execution Logs Card --%>
                <c:if test="${not empty jobLogs}">
                    <div class="card mb-4">
                        <div class="card-header">
                            <h5 class="mb-0"><i class="bi bi-journal-text"></i> Execution Logs</h5>
                        </div>
                        <div class="card-body p-0">
                            <div class="table-responsive" style="max-height:400px;overflow-y:auto">
                                <table class="table table-sm mb-0">
                                    <thead class="table-light sticky-top">
                                        <tr>
                                            <th style="width:180px">Time</th>
                                            <th style="width:80px">Level</th>
                                            <th>Message</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <c:forEach var="log" items="${jobLogs}">
                                            <tr class="${log.log_level == 'ERROR' ? 'table-danger' : log.log_level == 'WARN' ? 'table-warning' : ''}">
                                                <td><small>${log.created_at}</small></td>
                                                <td>
                                                    <span class="badge ${log.log_level == 'ERROR' ? 'bg-danger' : log.log_level == 'WARN' ? 'bg-warning text-dark' : log.log_level == 'DEBUG' ? 'bg-secondary' : 'bg-info'}">
                                                        ${log.log_level}
                                                    </span>
                                                </td>
                                                <td><small>${log.message}</small></td>
                                            </tr>
                                        </c:forEach>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </c:if>

            </c:if>

            <%-- ==================== LIST VIEW ==================== --%>
            <c:if test="${empty job}">

                <c:set var="filterBasePath" value="${pageContext.request.contextPath}/job-monitor" scope="request"/>
                <%@ include file="layout/filter-bar.jsp" %>

                <%-- Jobs Table --%>
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="bi bi-list-task"></i> All Jobs <small class="text-muted fw-normal">(${totalRecords} total)</small></h5>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th>Job #</th>
                                        <th>Job Name</th>
                                        <th>LC Number</th>
                                        <th>Shipment Doc</th>
                                        <th>Status</th>
                                        <th style="width:140px">Progress</th>
                                        <th>Started</th>
                                        <th>Completed</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="j" items="${jobs}">
                                        <tr data-job-id="${j.id}" data-status="${j.status}">
                                            <td>${j.id}</td>
                                            <%-- Job Name - inline editable --%>
                                            <td>
                                                <span class="inline-display inline-display-jobName-${j.id}"
                                                      ondblclick="editInline(${j.id}, 'jobName')">
                                                    <c:choose>
                                                        <c:when test="${not empty j.jobName}"><c:out value="${j.jobName}" /></c:when>
                                                        <c:otherwise><span class="text-muted fst-italic">-</span></c:otherwise>
                                                    </c:choose>
                                                    <i class="bi bi-pencil-square text-muted ms-1" style="font-size:0.75rem;cursor:pointer"
                                                       onclick="editInline(${j.id}, 'jobName')"></i>
                                                </span>
                                                <span class="inline-edit inline-edit-jobName-${j.id}" style="display:none">
                                                    <div class="input-group input-group-sm" style="min-width:150px">
                                                        <input type="text" class="form-control form-control-sm"
                                                               id="inline-jobName-${j.id}"
                                                               value="<c:out value='${j.jobName}'/>"
                                                               onkeydown="if(event.key==='Enter') saveInline(${j.id}); if(event.key==='Escape') cancelInline(${j.id}, 'jobName');">
                                                        <button class="btn btn-outline-success btn-sm" type="button"
                                                                onclick="saveInline(${j.id})"><i class="bi bi-check"></i></button>
                                                        <button class="btn btn-outline-secondary btn-sm" type="button"
                                                                onclick="cancelInline(${j.id}, 'jobName')"><i class="bi bi-x"></i></button>
                                                    </div>
                                                </span>
                                            </td>
                                            <td><strong>${j.lcNumber}</strong></td>
                                            <%-- Shipment Doc - inline editable --%>
                                            <td>
                                                <span class="inline-display inline-display-shipmentDocName-${j.id}"
                                                      ondblclick="editInline(${j.id}, 'shipmentDocName')">
                                                    <c:choose>
                                                        <c:when test="${not empty j.shipmentDocName}"><c:out value="${j.shipmentDocName}" /></c:when>
                                                        <c:otherwise><span class="text-muted fst-italic">-</span></c:otherwise>
                                                    </c:choose>
                                                    <i class="bi bi-pencil-square text-muted ms-1" style="font-size:0.75rem;cursor:pointer"
                                                       onclick="editInline(${j.id}, 'shipmentDocName')"></i>
                                                </span>
                                                <span class="inline-edit inline-edit-shipmentDocName-${j.id}" style="display:none">
                                                    <div class="input-group input-group-sm" style="min-width:150px">
                                                        <input type="text" class="form-control form-control-sm"
                                                               id="inline-shipmentDocName-${j.id}"
                                                               value="<c:out value='${j.shipmentDocName}'/>"
                                                               onkeydown="if(event.key==='Enter') saveInline(${j.id}); if(event.key==='Escape') cancelInline(${j.id}, 'shipmentDocName');">
                                                        <button class="btn btn-outline-success btn-sm" type="button"
                                                                onclick="saveInline(${j.id})"><i class="bi bi-check"></i></button>
                                                        <button class="btn btn-outline-secondary btn-sm" type="button"
                                                                onclick="cancelInline(${j.id}, 'shipmentDocName')"><i class="bi bi-x"></i></button>
                                                    </div>
                                                </span>
                                            </td>
                                            <td><span class="badge ${j.statusBadgeClass}">${j.status}</span></td>
                                            <td>
                                                <div class="progress" style="height:18px">
                                                    <div class="progress-bar ${j.status == 'FAILED' ? 'bg-danger' : (j.status == 'RUNNING' || j.status == 'QUEUED') ? 'progress-bar-striped progress-bar-animated' : ''}"
                                                         style="width:${j.progressPercent}%">
                                                        <small>${j.progressPercent}%</small>
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <small>
                                                    <c:if test="${not empty j.startedAt}">
                                                        <fmt:formatDate value="${j.startedAt}" pattern="dd-MMM HH:mm" />
                                                    </c:if>
                                                    <c:if test="${empty j.startedAt}"><span class="text-muted">-</span></c:if>
                                                </small>
                                            </td>
                                            <td>
                                                <small>
                                                    <c:if test="${not empty j.completedAt}">
                                                        <fmt:formatDate value="${j.completedAt}" pattern="dd-MMM HH:mm" />
                                                    </c:if>
                                                    <c:if test="${empty j.completedAt}"><span class="text-muted">-</span></c:if>
                                                </small>
                                            </td>
                                            <td class="text-nowrap">
                                                <a href="${pageContext.request.contextPath}/job-monitor?jobId=${j.id}"
                                                   class="btn btn-sm btn-outline-info" title="View Details">
                                                    <i class="bi bi-eye"></i>
                                                </a>
                                                <c:if test="${j.status == 'RUNNING' || j.status == 'QUEUED'}">
                                                    <button class="btn btn-sm btn-danger" title="Abort Job"
                                                            onclick="abortJob(${j.id})">
                                                        <i class="bi bi-stop-circle"></i>
                                                    </button>
                                                </c:if>
                                                <c:if test="${j.status == 'FAILED' || j.status == 'COMPLETED' || j.status == 'ABORTED'}">
                                                    <button class="btn btn-sm btn-outline-primary" title="Rerun Job"
                                                            onclick="rerunJob(${j.id})">
                                                        <i class="bi bi-arrow-clockwise"></i>
                                                    </button>
                                                </c:if>
                                                <c:if test="${j.status != 'RUNNING'}">
                                                    <button class="btn btn-sm btn-outline-danger" title="Delete Job"
                                                            onclick="deleteJob(${j.id}, '${j.jobDisplayName}')">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty jobs}">
                                        <tr><td colspan="9" class="text-center text-muted py-4">No jobs found.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <%@ include file="layout/pagination-bar.jsp" %>
                </div>

            </c:if>

        </div>
        <%@ include file="layout/footer.jsp" %>
    </div>
    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    var contextPath = '${pageContext.request.contextPath}';

    // ========== Delete job ==========
    function deleteJob(jobId, name) {
        var label = name ? 'Job #' + jobId + ' (' + name + ')' : 'Job #' + jobId;
        if (!confirm('Are you sure you want to delete ' + label + '?\nThis will remove all results, logs, and reports for this job.')) return;

        $.ajax({
            url: contextPath + '/job-monitor',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'delete', jobId: jobId }),
            success: function(res) {
                if (res.success) {
                    showToast(res.message, 'success');
                    $('tr[data-job-id="' + jobId + '"]').fadeOut(300, function() { $(this).remove(); });
                    setTimeout(function() { location.reload(); }, 1500);
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() { showToast('Failed to delete job', 'error'); }
        });
    }

    // ========== Rerun job ==========
    function rerunJob(jobId) {
        if (!confirm('This will create a NEW job with the same documents and reprocess. Continue?')) return;

        $.ajax({
            url: contextPath + '/job-monitor',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'rerun', jobId: jobId }),
            success: function(res) {
                if (res.success) {
                    showToast(res.message, 'success');
                    var newId = res.newJobId;
                    setTimeout(function() {
                        window.location.href = contextPath + '/job-monitor?jobId=' + (newId || '');
                    }, 1000);
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() { showToast('Failed to rerun job', 'error'); }
        });
    }

    // ========== Abort job ==========
    function abortJob(jobId) {
        if (!confirm('Are you sure you want to ABORT Job #' + jobId + '? This will stop processing immediately.')) return;

        $.ajax({
            url: contextPath + '/job-monitor',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'abort', jobId: jobId }),
            success: function(res) {
                if (res.success) {
                    showToast(res.message, 'success');
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() { showToast('Failed to abort job', 'error'); }
        });
    }

    // ========== Save job details (detail view) ==========
    function saveJobDetails(jobId) {
        var jobName = $('#detailJobName').val();
        var shipmentDocName = $('#detailShipmentDocName').val();

        $.ajax({
            url: contextPath + '/job-monitor',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                action: 'updateDetails',
                jobId: jobId,
                jobName: jobName,
                shipmentDocName: shipmentDocName
            }),
            success: function(res) {
                if (res.success) {
                    showToast(res.message, 'success');
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() { showToast('Failed to update job details', 'error'); }
        });
    }

    // ========== Inline editing (list view) ==========
    function editInline(jobId, field) {
        $('.inline-display-' + field + '-' + jobId).hide();
        $('.inline-edit-' + field + '-' + jobId).show();
        $('#inline-' + field + '-' + jobId).focus();
    }

    function cancelInline(jobId, field) {
        $('.inline-edit-' + field + '-' + jobId).hide();
        $('.inline-display-' + field + '-' + jobId).show();
    }

    function saveInline(jobId) {
        var jobName = $('#inline-jobName-' + jobId).val();
        var shipmentDocName = $('#inline-shipmentDocName-' + jobId).val();

        $.ajax({
            url: contextPath + '/job-monitor',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                action: 'updateDetails',
                jobId: jobId,
                jobName: jobName,
                shipmentDocName: shipmentDocName
            }),
            success: function(res) {
                if (res.success) {
                    showToast(res.message, 'success');
                    setTimeout(function() { location.reload(); }, 800);
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() { showToast('Failed to update job details', 'error'); }
        });
    }

    // ========== Auto-poll for running/queued jobs ==========
    var hasRunningJobs = $('tr[data-status="RUNNING"], tr[data-status="QUEUED"]').length > 0;
    <c:if test="${not empty job && (job.status == 'RUNNING' || job.status == 'QUEUED')}">
        hasRunningJobs = true;
    </c:if>

    if (hasRunningJobs) {
        setInterval(function() {
            // Poll list view rows
            $('tr[data-status="RUNNING"], tr[data-status="QUEUED"]').each(function() {
                var row = $(this);
                var jobId = row.data('job-id');
                $.getJSON(contextPath + '/api/job-status?jobId=' + jobId, function(data) {
                    row.find('.badge').first().text(data.status);
                    row.find('.progress-bar').css('width', data.progressPercent + '%')
                       .find('small').text(data.progressPercent + '%');
                    if (data.status === 'COMPLETED' || data.status === 'FAILED') {
                        location.reload();
                    }
                });
            });

            // Poll detail view
            <c:if test="${not empty job}">
            $.getJSON(contextPath + '/api/job-status?jobId=${job.id}', function(data) {
                $('#jobProgressBar').css('width', data.progressPercent + '%').text(data.progressPercent + '%');
                if (data.currentStep) {
                    $('#jobCurrentStep').html('<i class="bi bi-gear"></i> ' + data.currentStep);
                }
                if (data.status === 'COMPLETED' || data.status === 'FAILED') {
                    location.reload();
                }
            });
            </c:if>
        }, 5000);
    }
    </script>
</body>
</html>
