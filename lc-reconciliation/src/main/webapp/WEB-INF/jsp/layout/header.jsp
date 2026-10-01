<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="main-header">
    <div class="d-flex justify-content-between align-items-center px-3 py-2">
        <div class="d-flex align-items-center">
            <button class="btn btn-link text-dark me-2" id="sidebarCollapseBtn" title="Toggle sidebar" style="font-size:1.2rem">
                <i class="bi bi-layout-sidebar-inset" id="sidebarCollapseIcon"></i>
            </button>
            <img src="${pageContext.request.contextPath}/static/img/suzuki-logo-xs.png" alt="Suzuki" style="height:24px; margin-right:10px; opacity:0.7;">
            <h5 class="mb-0 text-muted">${pageTitle}</h5>
        </div>
        <div class="d-flex align-items-center">
            <button class="btn btn-link text-dark me-2" id="themeToggle" title="Toggle theme">
                <i class="bi bi-moon-fill" id="themeIcon"></i>
            </button>
            <script>
            (function(){
                // Apply saved theme immediately
                var s=localStorage.getItem('theme');
                if(s==='dark'){document.body.classList.add('dark-theme');var ic=document.getElementById('themeIcon');if(ic)ic.className='bi bi-sun-fill';}
                // Bind click
                document.getElementById('themeToggle').addEventListener('click',function(){
                    var b=document.body,ic=document.getElementById('themeIcon');
                    if(b.classList.contains('dark-theme')){b.classList.remove('dark-theme');ic.className='bi bi-moon-fill';localStorage.setItem('theme','light');}
                    else{b.classList.add('dark-theme');ic.className='bi bi-sun-fill';localStorage.setItem('theme','dark');}
                });
            })();
            </script>
            <div class="dropdown me-2">
                <button class="btn btn-link text-dark dropdown-toggle text-decoration-none" type="button" data-bs-toggle="dropdown">
                    <i class="bi bi-person-circle"></i> ${sessionScope.fullName}
                    <c:if test="${sessionScope.isItAdmin}"><span class="badge bg-dark ms-1" style="font-size:0.65rem">IT Admin</span></c:if>
                    <c:if test="${sessionScope.isSuperAdmin}"><span class="badge bg-danger ms-1" style="font-size:0.65rem">Admin</span></c:if>
                </button>
                <ul class="dropdown-menu dropdown-menu-end">
                    <li><span class="dropdown-item-text text-muted small">${sessionScope.userRole} | ${sessionScope.userDepartment}</span></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><a class="dropdown-item" href="#" data-bs-toggle="modal" data-bs-target="#changePasswordModal"><i class="bi bi-key me-2"></i>Change Password</a></li>
                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/logout"><i class="bi bi-box-arrow-right me-2"></i>Logout</a></li>
                </ul>
            </div>
        </div>
    </div>
</header>

<!-- Change Password Modal -->
<div class="modal fade" id="changePasswordModal" tabindex="-1">
    <div class="modal-dialog modal-sm">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title"><i class="bi bi-key"></i> Change Password</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div id="cpAlert"></div>
                <div class="mb-3">
                    <label class="form-label">Current Password</label>
                    <input type="password" class="form-control" id="cpCurrentPassword" required>
                </div>
                <div class="mb-3">
                    <label class="form-label">New Password</label>
                    <input type="password" class="form-control" id="cpNewPassword" placeholder="Min 6 characters" required>
                </div>
                <div class="mb-3">
                    <label class="form-label">Confirm New Password</label>
                    <input type="password" class="form-control" id="cpConfirmPassword" required>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                <button type="button" class="btn btn-primary" onclick="changePassword()">Change Password</button>
            </div>
        </div>
    </div>
</div>
<script>
function changePassword() {
    var data = {
        currentPassword: document.getElementById('cpCurrentPassword').value,
        newPassword: document.getElementById('cpNewPassword').value,
        confirmPassword: document.getElementById('cpConfirmPassword').value
    };
    if (!data.currentPassword || !data.newPassword || !data.confirmPassword) {
        document.getElementById('cpAlert').innerHTML = '<div class="alert alert-danger py-1 small">All fields required</div>';
        return;
    }
    if (data.newPassword.length < 6) {
        document.getElementById('cpAlert').innerHTML = '<div class="alert alert-danger py-1 small">Min 6 characters</div>';
        return;
    }
    if (data.newPassword !== data.confirmPassword) {
        document.getElementById('cpAlert').innerHTML = '<div class="alert alert-danger py-1 small">Passwords do not match</div>';
        return;
    }
    var contextPath = document.querySelector('img[alt="Suzuki"]').src.split('/static/')[0];
    var xhr = new XMLHttpRequest();
    xhr.open('POST', contextPath + '/change-password');
    xhr.setRequestHeader('Content-Type', 'application/json');
    xhr.onload = function() {
        var res = JSON.parse(xhr.responseText);
        if (res.success) {
            document.getElementById('cpAlert').innerHTML = '<div class="alert alert-success py-1 small">' + res.message + '</div>';
            if (res.logout) {
                // The session was invalidated server-side; send the user to the login screen.
                setTimeout(function() { window.location.href = contextPath + '/login'; }, 1500);
            } else {
                setTimeout(function() { bootstrap.Modal.getInstance(document.getElementById('changePasswordModal')).hide(); }, 1500);
            }
        } else {
            document.getElementById('cpAlert').innerHTML = '<div class="alert alert-danger py-1 small">' + res.message + '</div>';
        }
    };
    xhr.send(JSON.stringify(data));
}
</script>
