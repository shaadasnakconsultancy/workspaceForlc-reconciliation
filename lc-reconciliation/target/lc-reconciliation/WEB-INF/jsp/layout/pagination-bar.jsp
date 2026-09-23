<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${totalRecords != null}">
<div class="card-footer d-flex justify-content-between align-items-center">
    <span class="text-muted small">
        Showing ${(currentPage - 1) * pageSize + 1} - ${currentPage * pageSize > totalRecords ? totalRecords : currentPage * pageSize}
        of ${totalRecords} records
        <c:if test="${totalPages > 1}"> | Page ${currentPage} of ${totalPages}</c:if>
    </span>
    <c:if test="${totalPages > 1}">
    <nav>
        <ul class="pagination pagination-sm mb-0">
            <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                <a class="page-link" href="?page=1&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}${extraPageParams}"><i class="bi bi-chevron-double-left"></i></a>
            </li>
            <li class="page-item ${currentPage == 1 ? 'disabled' : ''}">
                <a class="page-link" href="?page=${currentPage-1}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}${extraPageParams}">Prev</a>
            </li>
            <c:forEach begin="${currentPage > 3 ? currentPage - 2 : 1}" end="${currentPage + 2 > totalPages ? totalPages : currentPage + 2}" var="p">
                <li class="page-item ${p == currentPage ? 'active' : ''}">
                    <a class="page-link" href="?page=${p}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}${extraPageParams}">${p}</a>
                </li>
            </c:forEach>
            <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                <a class="page-link" href="?page=${currentPage+1}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}${extraPageParams}">Next</a>
            </li>
            <li class="page-item ${currentPage == totalPages ? 'disabled' : ''}">
                <a class="page-link" href="?page=${totalPages}&pageSize=${pageSize}&fromDate=${fromDate}&toDate=${toDate}&searchJobId=${searchJobId}${extraPageParams}"><i class="bi bi-chevron-double-right"></i></a>
            </li>
        </ul>
    </nav>
    </c:if>
</div>
</c:if>
