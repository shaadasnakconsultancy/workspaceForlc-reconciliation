<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${reportType == 'non-compliance' ? 'Non-Compliance Reports' : 'Full Reports'} - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
</head>
<body>
    <c:set var="pageTitle" value="${reportType == 'non-compliance' ? 'Non-Compliance Reports' : 'Full Reports'}" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">
            <c:set var="filterBasePath" value="${pageContext.request.contextPath}/${reportType == 'non-compliance' ? 'non-compliance' : 'report'}" scope="request"/>
            <%@ include file="layout/filter-bar.jsp" %>

            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">
                        <c:choose>
                            <c:when test="${reportType == 'non-compliance'}">
                                <i class="bi bi-exclamation-triangle text-warning"></i> Non-Compliance Reports
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-file-earmark-text text-success"></i> Full Reconciliation Reports
                            </c:otherwise>
                        </c:choose>
                        <small class="text-muted fw-normal">(${totalRecords} total)</small>
                    </h5>
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
                                    <th>Started</th>
                                    <th>Completed</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="j" items="${jobs}">
                                    <tr>
                                        <td>${j.id}</td>
                                        <td><strong>${j.lcNumber}</strong></td>
                                        <td>${j.jobName}</td>
                                        <td>${j.shipmentDocName}</td>
                                        <td><fmt:formatDate value="${j.startedAt}" pattern="dd-MMM-yyyy HH:mm" /></td>
                                        <td><fmt:formatDate value="${j.completedAt}" pattern="dd-MMM-yyyy HH:mm" /></td>
                                        <td>
                                            <c:if test="${reportType == 'non-compliance'}">
                                                <a href="${pageContext.request.contextPath}/non-compliance?jobId=${j.id}" class="btn btn-sm btn-warning">
                                                    <i class="bi bi-exclamation-triangle"></i> View Report
                                                </a>
                                            </c:if>
                                            <c:if test="${reportType != 'non-compliance'}">
                                                <a href="${pageContext.request.contextPath}/report?jobId=${j.id}" class="btn btn-sm btn-success">
                                                    <i class="bi bi-file-earmark-text"></i> View Report
                                                </a>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty jobs}">
                                    <tr><td colspan="7" class="text-center text-muted py-4">No completed jobs found.</td></tr>
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
