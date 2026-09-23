<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="currentPath" value="${requestScope['javax.servlet.forward.request_uri']}" />
<nav class="sidebar" id="sidebar">
    <div class="sidebar-header text-center py-3">
        <img src="${pageContext.request.contextPath}/static/img/suzuki-logo.png" alt="Suzuki" style="height:36px; filter: brightness(0) invert(1);">
        <div class="text-white fw-bold mt-1" style="font-size:0.85rem; letter-spacing:0.5px;">LC Reconciliation System</div>
    </div>
    <ul class="nav flex-column">
        <li class="nav-item">
            <a class="nav-link ${currentPath.endsWith('/dashboard') ? 'active' : ''}" href="${pageContext.request.contextPath}/dashboard" title="Dashboard">
                <i class="bi bi-speedometer2"></i> <span>Dashboard</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.endsWith('/lc-upload') ? 'active' : ''}" href="${pageContext.request.contextPath}/lc-upload" title="LC Upload">
                <i class="bi bi-upload"></i> <span>LC Upload</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.endsWith('/job-monitor') ? 'active' : ''}" href="${pageContext.request.contextPath}/job-monitor" title="Job Monitor">
                <i class="bi bi-list-task"></i> <span>Job Monitor</span>
            </a>
        </li>
        <li class="nav-item mt-2">
            <span class="nav-section-title">Reports</span>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/report') && !currentPath.contains('non-compliance') && !currentPath.contains('cost') ? 'active' : ''}" href="${pageContext.request.contextPath}/report" title="Full Report">
                <i class="bi bi-file-earmark-text"></i> <span>Full Report</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/non-compliance') ? 'active' : ''}" href="${pageContext.request.contextPath}/non-compliance" title="Non-Compliance">
                <i class="bi bi-exclamation-triangle"></i> <span>Non-Compliance</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/cost-analysis') ? 'active' : ''}" href="${pageContext.request.contextPath}/cost-analysis" title="Cost Analysis">
                <i class="bi bi-currency-rupee"></i> <span>Cost Analysis</span>
            </a>
        </li>
        <c:if test="${sessionScope.isItAdmin}">
        <li class="nav-item mt-2">
            <span class="nav-section-title">Administration</span>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.endsWith('/settings') ? 'active' : ''}" href="${pageContext.request.contextPath}/settings" title="Settings">
                <i class="bi bi-gear"></i> <span>Settings</span>
            </a>
        </li>
        </c:if>
        <li class="nav-item mt-2">
            <span class="nav-section-title">Master Data</span>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/document-types') ? 'active' : ''}" href="${pageContext.request.contextPath}/master/document-types" title="Document Types">
                <i class="bi bi-file-earmark"></i> <span>Document Types</span>
            </a>
        </li>
        <c:if test="${sessionScope.isItAdmin || sessionScope.isSuperAdmin}">
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/prompts') ? 'active' : ''}" href="${pageContext.request.contextPath}/master/prompts" title="Prompt Templates">
                <i class="bi bi-chat-square-text"></i> <span>Prompt Templates</span>
            </a>
        </li>
        </c:if>
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/email-groups') ? 'active' : ''}" href="${pageContext.request.contextPath}/master/email-groups" title="Email Groups">
                <i class="bi bi-people-fill"></i> <span>Email Groups</span>
            </a>
        </li>
        <c:if test="${sessionScope.isItAdmin || sessionScope.isSuperAdmin}">
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/email-templates') ? 'active' : ''}" href="${pageContext.request.contextPath}/master/email-templates" title="Email Templates">
                <i class="bi bi-envelope-paper"></i> <span>Email Templates</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${currentPath.contains('/users') ? 'active' : ''}" href="${pageContext.request.contextPath}/master/users" title="User Management">
                <i class="bi bi-people"></i> <span>User Management</span>
            </a>
        </li>
        </c:if>
    </ul>
</nav>
