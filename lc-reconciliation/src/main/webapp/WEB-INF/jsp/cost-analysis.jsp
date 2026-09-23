<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cost Analysis - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <script src="${pageContext.request.contextPath}/static/vendor/chart.umd.min.js"></script>
</head>
<body>
    <c:set var="pageTitle" value="Cost Analysis" scope="request" />
    <%@ include file="layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="layout/header.jsp" %>
        <div class="container-fluid p-4">

            <%-- ==================== JOB COST DETAIL ==================== --%>
            <c:if test="${not empty job}">
                <div class="mb-3">
                    <a href="${pageContext.request.contextPath}/cost-analysis?view=report" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Cost Report</a>
                </div>
                <div class="card mb-3">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="bi bi-currency-rupee"></i> Job #${job.id} - ${job.jobDisplayName}</h5>
                        <span class="text-muted">LC: ${job.lcNumber}</span>
                        <c:if test="${not empty job.shipmentDocName}"><span class="ms-3">Shipment: ${job.shipmentDocName}</span></c:if>
                    </div>
                    <div class="card-body">
                        <div class="row mb-3">
                            <div class="col-md-3"><div class="card border-info border-2 text-center p-2"><div class="text-muted small">OCR Cost</div><div class="fs-5 fw-bold text-info">&#8377;<fmt:formatNumber value="${jobCostTotal.ocrCost}" pattern="#,##0.0000"/></div><div class="text-muted small">$<fmt:formatNumber value="${jobCostTotal.ocrCost / usdToInr}" pattern="#,##0.000000"/></div></div></div>
                            <div class="col-md-3"><div class="card border-warning border-2 text-center p-2"><div class="text-muted small">GPT Cost</div><div class="fs-5 fw-bold text-warning">&#8377;<fmt:formatNumber value="${jobCostTotal.gptCost}" pattern="#,##0.0000"/></div><div class="text-muted small">$<fmt:formatNumber value="${jobCostTotal.gptCost / usdToInr}" pattern="#,##0.000000"/></div></div></div>
                            <div class="col-md-3"><div class="card border-primary border-2 text-center p-2"><div class="text-muted small">Total Cost</div><div class="fs-5 fw-bold text-primary">&#8377;<fmt:formatNumber value="${jobCostTotal.totalCost}" pattern="#,##0.0000"/></div><div class="text-muted small">$<fmt:formatNumber value="${jobCostTotal.totalCost / usdToInr}" pattern="#,##0.000000"/></div></div></div>
                            <div class="col-md-3"><div class="card border-secondary border-2 text-center p-2"><div class="text-muted small">Tokens / Pages</div><div class="fs-5 fw-bold">${jobCostTotal.totalTokens} / ${jobCostTotal.totalPages}</div><div class="text-muted small">Rate: $1 = &#8377;${usdToInr}</div></div></div>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-bordered table-sm mb-0">
                                <thead class="table-light"><tr><th>#</th><th>API Type</th><th>Document</th><th class="text-end">Input Tokens</th><th class="text-end">Output Tokens</th><th class="text-end">Total Tokens</th><th class="text-end">Pages</th><th class="text-end">Cost (&#8377;)</th><th class="text-end">Cost ($)</th></tr></thead>
                                <tbody>
                                    <c:set var="seq" value="0"/>
                                    <c:forEach var="c" items="${jobCosts}"><c:set var="seq" value="${seq+1}"/>
                                        <tr style="background:${c.apiType=='OCR'?'rgba(13,202,240,0.05)':'rgba(255,193,7,0.05)'}">
                                            <td>${seq}</td>
                                            <td><span class="badge ${c.apiType=='OCR'?'bg-info':'bg-warning text-dark'}">${c.apiType}</span></td>
                                            <td>${c.documentName}</td>
                                            <td class="text-end">${c.apiType=='GPT'?c.promptTokens:'-'}</td>
                                            <td class="text-end">${c.apiType=='GPT'?c.completionTokens:'-'}</td>
                                            <td class="text-end">${c.apiType=='GPT'?c.totalTokens:'-'}</td>
                                            <td class="text-end">${c.apiType=='OCR'?c.pagesProcessed:'-'}</td>
                                            <td class="text-end"><fmt:formatNumber value="${c.costInr}" pattern="#,##0.0000"/></td>
                                            <td class="text-end"><fmt:formatNumber value="${c.costInr / usdToInr}" pattern="#,##0.000000"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                                <tfoot class="table-dark"><tr><td colspan="7" class="text-end fw-bold">Total</td><td class="text-end fw-bold">&#8377;<fmt:formatNumber value="${jobCostTotal.totalCost}" pattern="#,##0.0000"/></td><td class="text-end fw-bold">$<fmt:formatNumber value="${jobCostTotal.totalCost / usdToInr}" pattern="#,##0.000000"/></td></tr></tfoot>
                            </table>
                        </div>
                    </div>
                </div>
            </c:if>

            <%-- ==================== FULL REPORT VIEW ==================== --%>
            <c:if test="${viewMode == 'report'}">
                <div class="mb-3"><a href="${pageContext.request.contextPath}/cost-analysis" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back to Dashboard</a></div>
                <div class="row mb-3">
                    <div class="col-md-3"><div class="card border-primary border-2 text-center p-2"><div class="text-muted small">Total Cost</div><div class="fs-5 fw-bold text-primary">&#8377;<fmt:formatNumber value="${filteredSummary.totalCost}" pattern="#,##0.00"/></div><div class="small text-muted">$<fmt:formatNumber value="${filteredSummary.totalCost / usdToInr}" pattern="#,##0.00"/> | ${filteredSummary.totalJobs} jobs</div></div></div>
                    <div class="col-md-3"><div class="card border-info border-2 text-center p-2"><div class="text-muted small">OCR Cost</div><div class="fs-5 fw-bold text-info">&#8377;<fmt:formatNumber value="${filteredSummary.totalOcrCost}" pattern="#,##0.00"/></div><div class="small text-muted">${filteredSummary.totalPages} pages</div></div></div>
                    <div class="col-md-3"><div class="card border-warning border-2 text-center p-2"><div class="text-muted small">GPT Cost</div><div class="fs-5 fw-bold text-warning">&#8377;<fmt:formatNumber value="${filteredSummary.totalGptCost}" pattern="#,##0.00"/></div><div class="small text-muted"><fmt:formatNumber value="${filteredSummary.totalTokens}" pattern="#,###"/> tokens</div></div></div>
                    <div class="col-md-3"><div class="card border-secondary border-2 text-center p-2"><div class="text-muted small">Records</div><div class="fs-5 fw-bold">${totalRecords}</div><div class="small text-muted">$1 = &#8377;${usdToInr}</div></div></div>
                </div>
                <%-- Filters --%>
                <div class="card mb-3">
                    <div class="card-body py-2">
                        <form class="row g-2 align-items-end" method="get">
                            <input type="hidden" name="view" value="report">
                            <div class="col-md-2"><label class="form-label small mb-0">From Date</label><input type="date" class="form-control form-control-sm" name="fromDate" value="<c:out value='${fromDate}'/>"></div>
                            <div class="col-md-2"><label class="form-label small mb-0">To Date</label><input type="date" class="form-control form-control-sm" name="toDate" value="<c:out value='${toDate}'/>"></div>
                            <div class="col-md-2"><label class="form-label small mb-0">Search Job ID</label><input type="text" class="form-control form-control-sm" name="searchJobId" value="<c:out value='${searchJobId}'/>" placeholder="Job ID"></div>
                            <div class="col-md-1"><label class="form-label small mb-0">Per Page</label><select class="form-select form-select-sm" name="pageSize"><option value="10" ${pageSize==10?'selected':''}>10</option><option value="25" ${pageSize==25?'selected':''}>25</option><option value="50" ${pageSize==50?'selected':''}>50</option><option value="100" ${pageSize==100?'selected':''}>100</option></select></div>
                            <div class="col-md-2"><label class="form-label small mb-0">&nbsp;</label><button type="submit" class="btn btn-primary btn-sm w-100"><i class="bi bi-search"></i> Filter</button></div>
                            <div class="col-md-1"><label class="form-label small mb-0">&nbsp;</label><a href="${pageContext.request.contextPath}/cost-analysis?view=report" class="btn btn-outline-secondary btn-sm w-100"><i class="bi bi-x"></i> Clear</a></div>
                            <div class="col-md-2"><label class="form-label small mb-0">&nbsp;</label><a href="${pageContext.request.contextPath}/cost-analysis?format=xlsx&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}" class="btn btn-success btn-sm w-100"><i class="bi bi-file-earmark-excel"></i> Excel</a></div>
                        </form>
                    </div>
                </div>
                <%-- Grouped Table --%>
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h6 class="mb-0"><i class="bi bi-table"></i> API Cost Details (Grouped by Job)</h6>
                        <div>
                            <button class="btn btn-outline-secondary btn-sm me-1" onclick="$('.cost-detail-rows').hide();$('.toggle-icon').removeClass('bi-chevron-down').addClass('bi-chevron-right')"><i class="bi bi-arrows-collapse"></i> Collapse All</button>
                            <button class="btn btn-outline-secondary btn-sm" onclick="$('.cost-detail-rows').show();$('.toggle-icon').removeClass('bi-chevron-right').addClass('bi-chevron-down')"><i class="bi bi-arrows-expand"></i> Expand All</button>
                            <span class="text-muted small ms-2">Page ${currentPage} of ${totalPages} (${totalRecords} jobs)</span>
                        </div>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-sm mb-0" id="groupedCostTable">
                                <thead class="table-light">
                                    <tr><th style="width:30px"></th><th>Job ID</th><th>Job Name</th><th>LC Number</th><th>Shipment Doc</th><th class="text-end">API Calls</th><th class="text-end">Total (&#8377;)</th><th class="text-end">Total ($)</th><th>Date</th></tr>
                                </thead>
                                <tbody>
                                    <%-- Paginated by JOB - aggregates computed in SQL --%>
                                    <c:forEach var="job" items="${jobsList}">
                                        <tr class="job-group-header" style="cursor:pointer;background:#f8f9fa;font-weight:600"
                                            onclick="toggleJobDetails(${job.jobId}, this)">
                                            <td><i class="bi bi-chevron-right toggle-icon" id="icon-${job.jobId}"></i></td>
                                            <td><a href="${pageContext.request.contextPath}/cost-analysis?jobId=${job.jobId}">${job.jobId}</a></td>
                                            <td><c:out value="${job.jobName}"/></td>
                                            <td><c:out value="${job.lcNumber}"/></td>
                                            <td><c:out value="${job.shipmentDocName}"/></td>
                                            <td class="text-end"><span class="badge bg-secondary">${job.callCount}</span></td>
                                            <td class="text-end fw-bold"><fmt:formatNumber value="${job.totalCost}" pattern="#,##0.0000"/></td>
                                            <td class="text-end text-muted"><fmt:formatNumber value="${job.totalCost / usdToInr}" pattern="#,##0.0000"/></td>
                                            <td><small><fmt:formatDate value="${job.latestDate}" pattern="dd-MMM HH:mm"/></small></td>
                                        </tr>
                                        <c:forEach var="r" items="${costDetails}">
                                            <c:if test="${r.jobId == job.jobId}">
                                                <tr class="cost-detail-rows detail-${job.jobId}" style="display:none;font-size:0.85rem">
                                                    <td></td>
                                                    <td colspan="3"><small><c:out value="${r.documentName}"/></small></td>
                                                    <td><span class="badge ${r.apiType=='OCR'?'bg-info':'bg-warning text-dark'}">${r.apiType}</span></td>
                                                    <td class="text-end">${r.apiType=='GPT' ? r.promptTokens : '-'} / ${r.apiType=='GPT' ? r.completionTokens : '-'}</td>
                                                    <td class="text-end">${r.apiType=='OCR' ? r.pagesProcessed : '-'}</td>
                                                    <td class="text-end"><fmt:formatNumber value="${r.costInr}" pattern="#,##0.0000"/></td>
                                                    <td class="text-end"><fmt:formatNumber value="${r.costInr / usdToInr}" pattern="#,##0.000000"/></td>
                                                </tr>
                                            </c:if>
                                        </c:forEach>
                                    </c:forEach>
                                    <c:if test="${empty jobsList}">
                                        <tr><td colspan="9" class="text-center text-muted py-4">No cost data found.</td></tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <script>
                    function toggleJobDetails(jobId, el) {
                        $('.detail-' + jobId).toggle();
                        $('#icon-' + jobId).toggleClass('bi-chevron-right bi-chevron-down');
                    }
                    </script>
                    <c:if test="${totalPages > 1}">
                    <div class="card-footer d-flex justify-content-between align-items-center">
                        <span class="text-muted small">Total: ${totalRecords} records</span>
                        <nav><ul class="pagination pagination-sm mb-0">
                            <li class="page-item ${currentPage==1?'disabled':''}"><a class="page-link" href="?view=report&page=${currentPage-1}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}">Prev</a></li>
                            <c:forEach begin="${currentPage>3?currentPage-2:1}" end="${currentPage+2>totalPages?totalPages:currentPage+2}" var="p">
                                <li class="page-item ${p==currentPage?'active':''}"><a class="page-link" href="?view=report&page=${p}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}">${p}</a></li>
                            </c:forEach>
                            <li class="page-item ${currentPage==totalPages?'disabled':''}"><a class="page-link" href="?view=report&page=${currentPage+1}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}">Next</a></li>
                        </ul></nav>
                    </div>
                    </c:if>
                </div>
            </c:if>

            <%-- ==================== ANALYTICS DASHBOARD ==================== --%>
            <c:if test="${empty job && viewMode != 'report'}">
                <div class="row mb-4">
                    <div class="col-xl-2 col-md-4 mb-3"><div class="card stat-card border-start border-primary border-4"><div class="card-body py-2"><div class="text-muted small text-uppercase">Total Cost</div><div class="fs-4 fw-bold text-primary">&#8377;<fmt:formatNumber value="${summary.totalCost}" pattern="#,##0.00"/></div><div class="small text-muted">$<fmt:formatNumber value="${summary.totalCost / usdToInr}" pattern="#,##0.00"/></div></div></div></div>
                    <div class="col-xl-2 col-md-4 mb-3"><div class="card stat-card border-start border-info border-4"><div class="card-body py-2"><div class="text-muted small text-uppercase">OCR Cost</div><div class="fs-4 fw-bold text-info">&#8377;<fmt:formatNumber value="${summary.totalOcrCost}" pattern="#,##0.00"/></div></div></div></div>
                    <div class="col-xl-2 col-md-4 mb-3"><div class="card stat-card border-start border-warning border-4"><div class="card-body py-2"><div class="text-muted small text-uppercase">GPT Cost</div><div class="fs-4 fw-bold text-warning">&#8377;<fmt:formatNumber value="${summary.totalGptCost}" pattern="#,##0.00"/></div></div></div></div>
                    <div class="col-xl-2 col-md-4 mb-3"><div class="card stat-card border-start border-success border-4"><div class="card-body py-2"><div class="text-muted small text-uppercase">Avg/Job</div><div class="fs-4 fw-bold text-success">&#8377;<fmt:formatNumber value="${summary.avgCostPerJob}" pattern="#,##0.00"/></div></div></div></div>
                    <div class="col-xl-2 col-md-4 mb-3"><div class="card stat-card border-start border-secondary border-4"><div class="card-body py-2"><div class="text-muted small text-uppercase">Jobs</div><div class="fs-4 fw-bold">${summary.totalJobs}</div></div></div></div>
                    <div class="col-xl-2 col-md-4 mb-3"><div class="card stat-card border-start border-dark border-4"><div class="card-body py-2 text-center"><a href="${pageContext.request.contextPath}/cost-analysis?view=report" class="btn btn-outline-primary btn-sm w-100 mt-2"><i class="bi bi-table"></i> Full Report</a></div></div></div>
                </div>
                <div class="row mb-4">
                    <div class="col-md-8"><div class="card"><div class="card-header"><h6 class="mb-0"><i class="bi bi-graph-up"></i> Monthly Cost Trend</h6></div><div class="card-body"><canvas id="monthlyChart" height="100"></canvas></div></div></div>
                    <div class="col-md-4"><div class="card"><div class="card-header"><h6 class="mb-0"><i class="bi bi-pie-chart"></i> Cost Distribution</h6></div><div class="card-body"><canvas id="distChart" height="200"></canvas></div></div></div>
                </div>
                <div class="card">
                    <div class="card-header"><h6 class="mb-0"><i class="bi bi-sort-down"></i> Top Costly Jobs</h6></div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover table-sm mb-0">
                                <thead class="table-light"><tr><th>#</th><th>Job ID</th><th>Job Name</th><th>Shipment Doc</th><th>LC Number</th><th class="text-end">OCR (&#8377;)</th><th class="text-end">GPT (&#8377;)</th><th class="text-end">Total (&#8377;)</th><th class="text-end">Total ($)</th><th>Date</th></tr></thead>
                                <tbody>
                                    <c:forEach var="j" items="${topJobs}" varStatus="loop">
                                        <tr>
                                            <td>${loop.index+1}</td>
                                            <td><a href="${pageContext.request.contextPath}/cost-analysis?jobId=${j.jobId}">${j.jobId}</a> <a href="${pageContext.request.contextPath}/job-monitor?jobId=${j.jobId}" class="text-muted"><i class="bi bi-box-arrow-up-right"></i></a></td>
                                            <td>${j.jobName}</td><td>${j.shipmentDocName}</td><td>${j.lcNumber}</td>
                                            <td class="text-end"><fmt:formatNumber value="${j.ocrCost}" pattern="#,##0.0000"/></td>
                                            <td class="text-end"><fmt:formatNumber value="${j.gptCost}" pattern="#,##0.0000"/></td>
                                            <td class="text-end fw-bold"><fmt:formatNumber value="${j.totalCost}" pattern="#,##0.0000"/></td>
                                            <td class="text-end text-muted"><fmt:formatNumber value="${j.totalCost / usdToInr}" pattern="#,##0.0000"/></td>
                                            <td><small><fmt:formatDate value="${j.createdAt}" pattern="dd-MMM-yy"/></small></td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty topJobs}"><tr><td colspan="10" class="text-center text-muted py-4">No cost data yet.</td></tr></c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </c:if>
        </div>
        <%@ include file="layout/footer.jsp" %>
    </div>
    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/vendor/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <c:if test="${empty job && viewMode != 'report'}">
    <script>
    $.getJSON('${pageContext.request.contextPath}/cost-analysis?format=json&type=monthly', function(data) {
        if (!data || !data.length) return;
        new Chart(document.getElementById('monthlyChart'), {
            type:'line', data:{labels:data.map(function(d){return d.month;}),datasets:[
                {label:'OCR (INR)',data:data.map(function(d){return d.ocrCost;}),borderColor:'#0dcaf0',backgroundColor:'rgba(13,202,240,0.1)',fill:true},
                {label:'GPT (INR)',data:data.map(function(d){return d.gptCost;}),borderColor:'#ffc107',backgroundColor:'rgba(255,193,7,0.1)',fill:true}
            ]}, options:{responsive:true,plugins:{legend:{position:'bottom'}},scales:{y:{beginAtZero:true}}}
        });
    });
    var ocrT=${summary.totalOcrCost!=null?summary.totalOcrCost:0}, gptT=${summary.totalGptCost!=null?summary.totalGptCost:0};
    if(ocrT>0||gptT>0){new Chart(document.getElementById('distChart'),{type:'doughnut',data:{labels:['OCR','GPT'],datasets:[{data:[ocrT,gptT],backgroundColor:['#0dcaf0','#ffc107']}]},options:{responsive:true,plugins:{legend:{position:'bottom'}}}});}
    </script>
    </c:if>
</body>
</html>
