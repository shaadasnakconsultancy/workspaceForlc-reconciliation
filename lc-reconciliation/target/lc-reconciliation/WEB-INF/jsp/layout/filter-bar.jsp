<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<div class="card mb-3">
    <div class="card-body py-2">
        <form class="row g-2 align-items-end" method="get">
            <!-- Preserve any extra hidden fields set by parent page -->
            <c:if test="${not empty extraHiddenFields}">${extraHiddenFields}</c:if>
            <div class="col-md-2">
                <label class="form-label small mb-0">From Date</label>
                <input type="date" class="form-control form-control-sm" name="fromDate" value="${fromDate}">
            </div>
            <div class="col-md-2">
                <label class="form-label small mb-0">To Date</label>
                <input type="date" class="form-control form-control-sm" name="toDate" value="${toDate}">
            </div>
            <div class="col-md-2">
                <label class="form-label small mb-0">Search Job ID</label>
                <input type="text" class="form-control form-control-sm" name="searchJobId" value="${searchJobId}" placeholder="Job ID">
            </div>
            <div class="col-md-1">
                <label class="form-label small mb-0">Per Page</label>
                <select class="form-select form-select-sm" name="pageSize">
                    <option value="10" ${pageSize == 10 ? 'selected' : ''}>10</option>
                    <option value="25" ${pageSize == 25 ? 'selected' : ''}>25</option>
                    <option value="50" ${pageSize == 50 ? 'selected' : ''}>50</option>
                    <option value="100" ${pageSize == 100 ? 'selected' : ''}>100</option>
                </select>
            </div>
            <div class="col-md-2">
                <button type="submit" class="btn btn-primary btn-sm w-100"><i class="bi bi-search"></i> Filter</button>
            </div>
            <div class="col-md-1">
                <a href="${filterBasePath}" class="btn btn-outline-secondary btn-sm w-100"><i class="bi bi-x"></i> Clear</a>
            </div>
            <div class="col-md-2">
                <button type="button" class="btn btn-outline-secondary btn-sm w-100" onclick="location.reload()"><i class="bi bi-arrow-clockwise"></i> Refresh</button>
            </div>
        </form>
    </div>
</div>
