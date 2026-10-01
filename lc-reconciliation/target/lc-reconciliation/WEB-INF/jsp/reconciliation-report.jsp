<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${reportType == 'non-compliance' ? 'Non-Compliance' : 'Reconciliation'} Report - LC Reconciliation</title>
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/report.css" rel="stylesheet">
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
</head>
<body>
    <c:set var="pageTitle" value="${reportType == 'non-compliance' ? 'Non-Compliance Report' : 'Full Reconciliation Report'}" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">
            <%-- Back button --%>
            <div class="mb-3">
                <c:choose>
                    <c:when test="${reportType == 'non-compliance'}">
                        <a href="${pageContext.request.contextPath}/non-compliance" class="btn btn-outline-secondary">
                            <i class="bi bi-arrow-left"></i> Back to Non-Compliance Reports
                        </a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/report" class="btn btn-outline-secondary">
                            <i class="bi bi-arrow-left"></i> Back to Full Reports
                        </a>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <div>
                        <h5 class="mb-0">
                            <c:choose>
                                <c:when test="${reportType == 'non-compliance'}">
                                    <i class="bi bi-exclamation-triangle text-warning"></i> Non-Compliance Report
                                </c:when>
                                <c:otherwise>
                                    <i class="bi bi-file-earmark-text text-success"></i> LC Document Reconciliation Report
                                </c:otherwise>
                            </c:choose>
                        </h5>
                        <span class="text-muted">LC Number: <strong>${job.lcNumber}</strong></span>
                        <c:if test="${not empty job.jobName}">
                            <span class="ms-3">Job: <strong>${job.jobName}</strong></span>
                        </c:if>
                        <c:if test="${not empty job.shipmentDocName}">
                            <span class="ms-3">Shipment: <strong>${job.shipmentDocName}</strong></span>
                        </c:if>
                        <span class="ms-3 text-muted small">Job #${job.id}</span>
                        <c:if test="${not empty job.startedAt}">
                            <span class="ms-3 text-muted small"><i class="bi bi-clock"></i> <fmt:formatDate value="${job.startedAt}" pattern="dd-MMM-yyyy HH:mm" /> - <fmt:formatDate value="${job.completedAt}" pattern="HH:mm" /></span>
                        </c:if>
                        <div class="mt-1">
                            <c:set var="compliedCount" value="0" /><c:set var="notCompliedCount" value="0" /><c:set var="naCount" value="0" />
                            <c:forEach var="r" items="${results}">
                                <c:if test="${r.status == 'Complied'}"><c:set var="compliedCount" value="${compliedCount + 1}" /></c:if>
                                <c:if test="${r.status == 'Not Complied'}"><c:set var="notCompliedCount" value="${notCompliedCount + 1}" /></c:if>
                                <c:if test="${r.status == 'Not Applicable'}"><c:set var="naCount" value="${naCount + 1}" /></c:if>
                            </c:forEach>
                            <span class="badge bg-success"><i class="bi bi-check-circle"></i> Complied: ${compliedCount}</span>
                            <span class="badge bg-danger"><i class="bi bi-x-circle"></i> Not Complied: ${notCompliedCount}</span>
                            <span class="badge bg-secondary"><i class="bi bi-dash-circle"></i> N/A: ${naCount}</span>
                            <span class="badge bg-dark">Total: ${compliedCount + notCompliedCount + naCount}</span>
                        </div>
                    </div>
                    <div>
                        <%-- Navigation between report types --%>
                        <c:if test="${reportType == 'non-compliance'}">
                            <a href="${pageContext.request.contextPath}/report?jobId=${job.id}" class="btn btn-sm btn-outline-success mb-1">
                                <i class="bi bi-file-earmark-text"></i> View Full Report
                            </a>
                        </c:if>
                        <c:if test="${reportType != 'non-compliance'}">
                            <a href="${pageContext.request.contextPath}/non-compliance?jobId=${job.id}" class="btn btn-sm btn-outline-warning mb-1">
                                <i class="bi bi-exclamation-triangle"></i> View Non-Compliance
                            </a>
                        </c:if>
                        <a href="${pageContext.request.contextPath}/job-monitor?jobId=${job.id}" class="btn btn-sm btn-outline-info mb-1">
                            <i class="bi bi-eye"></i> Job Details
                        </a>
                        <br>
                        <%-- Downloads --%>
                        <c:if test="${reportType == 'non-compliance'}">
                            <a href="${pageContext.request.contextPath}/non-compliance?jobId=${job.id}&format=html" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-download"></i> HTML
                            </a>
                            <a href="${pageContext.request.contextPath}/non-compliance?jobId=${job.id}&format=xlsx" class="btn btn-sm btn-outline-success">
                                <i class="bi bi-file-earmark-excel"></i> XLSX
                            </a>
                        </c:if>
                        <c:if test="${reportType != 'non-compliance'}">
                            <a href="${pageContext.request.contextPath}/report?jobId=${job.id}&format=html" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-download"></i> HTML
                            </a>
                            <a href="${pageContext.request.contextPath}/report?jobId=${job.id}&format=xlsx" class="btn btn-sm btn-outline-success">
                                <i class="bi bi-file-earmark-excel"></i> Download XLSX
                            </a>
                        </c:if>
                    </div>
                </div>
                <div class="card-body">
                    <!-- Legend -->
                    <div class="report-legend">
                        <div class="legend-item"><span class="cell-indicator tick">&#10003;</span><span>Complied</span></div>
                        <div class="legend-item"><span class="cell-indicator cross">&#10007;</span><span>Not Complied</span></div>
                        <div class="legend-item"><span class="cell-indicator no-compare">&#8856;</span><span>Not Applicable</span></div>
                    </div>

                    <!-- Document filter tabs -->
                    <ul class="nav nav-pills mb-3" id="docFilterTabs">
                        <li class="nav-item"><a class="nav-link active" data-filter="all" href="#">All Documents</a></li>
                        <c:forEach var="docType" items="${uploadedDocTypes}">
                            <li class="nav-item"><a class="nav-link" data-filter="${docType}" href="#">${docType}</a></li>
                        </c:forEach>
                    </ul>

                    <!-- Per-document tables -->
                    <c:forEach var="docType" items="${uploadedDocTypes}">
                        <div data-doctype="${docType}">
                        <h5 class="mt-4 mb-2"><i class="bi bi-file-earmark-text"></i> ${docType}</h5>
                        <div class="table-responsive">
                            <table class="report-table">
                                <thead>
                                    <tr>
                                        <th style="width:40px">#</th>
                                        <th class="param-col">Parameter</th>
                                        <th style="width:80px">LC Clause</th>
                                        <th>LC Data</th>
                                        <th>${docType} Data</th>
                                        <th style="width:110px">Status</th>
                                        <th>Reason</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:set var="hasRows" value="false" />
                                    <c:set var="rowNum" value="0" />
                                    <c:forEach var="row" items="${results}">
                                        <c:if test="${row.documentTypeName == docType}">
                                            <c:set var="hasRows" value="true" />
                                            <c:set var="rowNum" value="${rowNum + 1}" />
                                            <tr>
                                                <td>${rowNum}</td>
                                                <td class="param-col">${row.parameterName}</td>
                                                <td>${row.lcClauseNo}</td>
                                                <td class="data-cell">${row.lcValue}</td>
                                                <td class="data-cell">
                                                    ${row.documentValue}
                                                    <c:choose>
                                                        <c:when test="${row.status == 'Complied'}"><span class="cell-indicator tick">&#10003;</span></c:when>
                                                        <c:when test="${row.status == 'Not Complied'}"><span class="cell-indicator cross">&#10007;</span></c:when>
                                                        <c:otherwise><span class="cell-indicator no-compare">&#8856;</span></c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="${row.resultCssClass}">${row.status}</td>
                                                <td>${row.reason}</td>
                                            </tr>
                                        </c:if>
                                    </c:forEach>
                                    <c:if test="${!hasRows}">
                                        <tr><td colspan="7" class="text-center text-muted py-3">No results for this document.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                        </div>
                    </c:forEach>

                    <c:if test="${empty results}">
                        <div class="text-center text-muted py-4">No results found.</div>
                    </c:if>
                </div>
            </div>
        </div>
        <%@ include file="layout/footer.jsp" %>
    </div>
    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
        $(document).ready(function() {
            $('#docFilterTabs a').on('click', function(e) {
                e.preventDefault();
                $('#docFilterTabs a').removeClass('active');
                $(this).addClass('active');
                var filter = $(this).data('filter');
                if (filter === 'all') { $('[data-doctype]').show(); }
                else { $('[data-doctype]').hide(); $('[data-doctype="' + filter + '"]').show(); }
            });
        });
    </script>
</body>
</html>
