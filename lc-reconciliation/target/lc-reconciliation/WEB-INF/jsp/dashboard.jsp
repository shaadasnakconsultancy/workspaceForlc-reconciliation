<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Dashboard - LC Reconciliation</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
</head>
<body>
    <c:set var="pageTitle" value="Dashboard" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">
            <div class="row mb-4">
                <div class="col-xl-3 col-md-6 mb-3">
                    <div class="card stat-card border-start border-primary border-4">
                        <div class="card-body">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="text-muted small text-uppercase">Total Jobs</div>
                                    <div class="fs-3 fw-bold">${totalJobs}</div>
                                </div>
                                <div class="text-primary fs-1"><i class="bi bi-clipboard-data"></i></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xl-3 col-md-6 mb-3">
                    <div class="card stat-card border-start border-success border-4">
                        <div class="card-body">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="text-muted small text-uppercase">Completed</div>
                                    <div class="fs-3 fw-bold text-success">${completedJobs}</div>
                                </div>
                                <div class="text-success fs-1"><i class="bi bi-check-circle"></i></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xl-3 col-md-6 mb-3">
                    <div class="card stat-card border-start border-info border-4">
                        <div class="card-body">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="text-muted small text-uppercase">Running</div>
                                    <div class="fs-3 fw-bold text-info">${runningJobs}</div>
                                </div>
                                <div class="text-info fs-1"><i class="bi bi-arrow-repeat"></i></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-xl-3 col-md-6 mb-3">
                    <div class="card stat-card border-start border-danger border-4">
                        <div class="card-body">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="text-muted small text-uppercase">Failed</div>
                                    <div class="fs-3 fw-bold text-danger">${failedJobs}</div>
                                </div>
                                <div class="text-danger fs-1"><i class="bi bi-x-circle"></i></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <c:set var="filterBasePath" value="${pageContext.request.contextPath}/dashboard" scope="request"/>
            <%@ include file="layout/filter-bar.jsp" %>

            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0"><i class="bi bi-clock-history"></i> Reconciliation Jobs <small class="text-muted fw-normal">(${totalRecords} total)</small></h5>
                    <div class="d-flex align-items-center gap-2">
                        <a href="${pageContext.request.contextPath}/lc-upload" class="btn btn-primary btn-sm">
                            <i class="bi bi-plus-circle"></i> New Upload
                        </a>
                    </div>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Job #</th>
                                    <th>LC Number</th>
                                    <th>Job Name</th>
                                    <th>Shipment Doc</th>
                                    <th>Status</th>
                                    <th>Progress</th>
                                    <th>Created</th>
                                    <th>Started</th>
                                    <th>Completed</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="job" items="${jobs}">
                                    <tr>
                                        <td>${job.id}</td>
                                        <td><strong>${job.lcNumber}</strong></td>
                                        <td>${job.jobName}</td>
                                        <td>${job.shipmentDocName}</td>
                                        <td><span class="badge ${job.statusBadgeClass}">${job.status}</span></td>
                                        <td style="width:150px">
                                            <div class="progress" style="height:20px">
                                                <div class="progress-bar ${job.status == 'FAILED' ? 'bg-danger' : ''}"
                                                     style="width:${job.progressPercent}%">${job.progressPercent}%</div>
                                            </div>
                                        </td>
                                        <td><fmt:formatDate value="${job.createdAt}" pattern="dd-MMM-yyyy HH:mm" /></td>
                                        <td><fmt:formatDate value="${job.startedAt}" pattern="dd-MMM HH:mm" /></td>
                                        <td><fmt:formatDate value="${job.completedAt}" pattern="dd-MMM HH:mm" /></td>
                                        <td>
                                            <c:if test="${job.status == 'COMPLETED'}">
                                                <a href="${pageContext.request.contextPath}/report?jobId=${job.id}" class="btn btn-sm btn-outline-success" title="View Report">
                                                    <i class="bi bi-file-earmark-text"></i>
                                                </a>
                                                <a href="${pageContext.request.contextPath}/non-compliance?jobId=${job.id}" class="btn btn-sm btn-outline-warning" title="Non-Compliance">
                                                    <i class="bi bi-exclamation-triangle"></i>
                                                </a>
                                            </c:if>
                                            <a href="${pageContext.request.contextPath}/job-monitor?jobId=${job.id}" class="btn btn-sm btn-outline-info" title="View Details">
                                                <i class="bi bi-eye"></i>
                                            </a>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty jobs}">
                                    <tr><td colspan="10" class="text-center text-muted py-4">No reconciliation jobs yet. <a href="${pageContext.request.contextPath}/lc-upload">Upload LC documents</a> to get started.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
                <%@ include file="layout/pagination-bar.jsp" %>
            </div>
        </div>
        <%@ include file="layout/footer.jsp" %>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
</body>
</html>
