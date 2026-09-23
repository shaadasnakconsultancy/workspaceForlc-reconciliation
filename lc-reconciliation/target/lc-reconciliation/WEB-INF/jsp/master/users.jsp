<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>User Management - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
</head>
<body>
    <c:set var="pageTitle" value="User Management" scope="request" />
    <%@ include file="../layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="../layout/header.jsp" %>
        <div class="container-fluid p-4">
            <div id="alertContainer"></div>
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0"><i class="bi bi-people"></i> User Management</h5>
                    <button class="btn btn-primary btn-sm" onclick="openModal()">
                        <i class="bi bi-plus-circle"></i> Add User
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0">
                            <thead class="table-light">
                                <tr>
                                    <th>Username</th>
                                    <th>First Name</th>
                                    <th>Last Name</th>
                                    <th>Email</th>
                                    <th>Department</th>
                                    <th>Role</th>
                                    <th>Active</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="u" items="${users}">
                                    <tr>
                                        <td><strong>${u.username}</strong></td>
                                        <td>${u.firstName}</td>
                                        <td>${u.lastName}</td>
                                        <td>${u.email}</td>
                                        <td>${u.department}</td>
                                        <td><span class="badge ${u.roleBadgeClass}">${u.roleDisplay}</span></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${u.active}"><span class="badge bg-success">Active</span></c:when>
                                                <c:otherwise><span class="badge bg-secondary">Inactive</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <button class="btn btn-sm btn-outline-primary" title="Edit"
                                                    onclick="editUser(${u.id}, '${u.username}', '${u.firstName}', '${u.lastName}', '${u.email}', '${u.department}', '${u.role}', ${u.active})">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <button class="btn btn-sm btn-outline-warning" title="Reset Password"
                                                    onclick="openResetPasswordModal(${u.id}, '${u.username}')">
                                                <i class="bi bi-key"></i>
                                            </button>
                                            <button class="btn btn-sm btn-outline-danger" title="Delete"
                                                    onclick="deleteUser(${u.id}, '${u.username}')">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty users}">
                                    <tr><td colspan="8" class="text-center text-muted py-4">No users found.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
        <%@ include file="../layout/footer.jsp" %>
    </div>

    <!-- Add/Edit User Modal -->
    <div class="modal fade" id="userModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="userModalTitle">Add User</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="userId">
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label class="form-label">Username <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="username" required>
                        </div>
                        <div class="col-md-6" id="passwordGroup">
                            <label class="form-label">Password <span class="text-danger">*</span></label>
                            <input type="password" class="form-control" id="password" placeholder="Min 6 characters">
                        </div>
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label class="form-label">First Name <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="firstName" required>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Last Name <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="lastName" required>
                        </div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Email Address</label>
                        <input type="email" class="form-control" id="email">
                    </div>
                    <div class="row mb-3">
                        <div class="col-md-6">
                            <label class="form-label">Department</label>
                            <input type="text" class="form-control" id="department">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label">Role <span class="text-danger">*</span></label>
                            <select class="form-select" id="role">
                                <option value="DEPARTMENT_USER">Department User</option>
                                <option value="SUPER_ADMIN">Super Admin</option>
                                <option value="IT_ADMIN">IT Admin</option>
                            </select>
                        </div>
                    </div>
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="isActive" checked>
                        <label class="form-check-label" for="isActive">Active</label>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary" onclick="saveUser()">Save</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Reset Password Modal -->
    <div class="modal fade" id="resetPasswordModal" tabindex="-1">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Reset Password</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="resetUserId">
                    <p class="text-muted">Reset password for: <strong id="resetUsername"></strong></p>
                    <div class="mb-3">
                        <label class="form-label">New Password</label>
                        <input type="password" class="form-control" id="newResetPassword" placeholder="Min 6 characters">
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-warning" onclick="resetPassword()">Reset Password</button>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    var contextPath = '${pageContext.request.contextPath}';

    function openModal() {
        $('#userModalTitle').text('Add User');
        $('#userId').val('');
        $('#username').val('').prop('readonly', false);
        $('#password').val('');
        $('#passwordGroup').show();
        $('#firstName').val('');
        $('#lastName').val('');
        $('#email').val('');
        $('#department').val('');
        $('#role').val('DEPARTMENT_USER');
        $('#isActive').prop('checked', true);
        new bootstrap.Modal('#userModal').show();
    }

    function editUser(id, username, firstName, lastName, email, department, role, active) {
        $('#userModalTitle').text('Edit User');
        $('#userId').val(id);
        $('#username').val(username).prop('readonly', true);
        $('#passwordGroup').hide();
        $('#firstName').val(firstName);
        $('#lastName').val(lastName);
        $('#email').val(email);
        $('#department').val(department);
        $('#role').val(role);
        $('#isActive').prop('checked', active);
        new bootstrap.Modal('#userModal').show();
    }

    function saveUser() {
        var id = $('#userId').val();
        var isNew = !id;
        var data = {
            action: isNew ? 'create' : 'update',
            username: $('#username').val(),
            firstName: $('#firstName').val(),
            lastName: $('#lastName').val(),
            email: $('#email').val(),
            department: $('#department').val(),
            role: $('#role').val(),
            isActive: $('#isActive').is(':checked')
        };

        if (!data.username || !data.firstName || !data.lastName) {
            showAlert('Please fill required fields', 'danger');
            return;
        }

        if (isNew) {
            data.password = $('#password').val();
            if (!data.password || data.password.length < 6) {
                showAlert('Password must be at least 6 characters', 'danger');
                return;
            }
        } else {
            data.id = parseInt(id);
        }

        $.ajax({
            url: contextPath + '/master/users',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(res) {
                if (res.success) {
                    bootstrap.Modal.getInstance(document.getElementById('userModal')).hide();
                    showAlert(res.message, 'success');
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert(res.message, 'danger');
                }
            },
            error: function() { showAlert('Request failed', 'danger'); }
        });
    }

    function deleteUser(id, username) {
        if (!confirm('Are you sure you want to delete user "' + username + '"?')) return;
        $.ajax({
            url: contextPath + '/master/users',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'delete', id: id }),
            success: function(res) {
                if (res.success) {
                    showAlert(res.message, 'success');
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert(res.message, 'danger');
                }
            }
        });
    }

    function openResetPasswordModal(id, username) {
        $('#resetUserId').val(id);
        $('#resetUsername').text(username);
        $('#newResetPassword').val('');
        new bootstrap.Modal('#resetPasswordModal').show();
    }

    function resetPassword() {
        var newPassword = $('#newResetPassword').val();
        if (!newPassword || newPassword.length < 6) {
            showAlert('Password must be at least 6 characters', 'danger');
            return;
        }
        $.ajax({
            url: contextPath + '/master/users',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                action: 'resetPassword',
                id: parseInt($('#resetUserId').val()),
                newPassword: newPassword
            }),
            success: function(res) {
                if (res.success) {
                    bootstrap.Modal.getInstance(document.getElementById('resetPasswordModal')).hide();
                    showAlert(res.message, 'success');
                } else {
                    showAlert(res.message, 'danger');
                }
            }
        });
    }

    function showAlert(msg, type) {
        var html = '<div class="alert alert-' + type + ' alert-dismissible fade show">' + msg +
            '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
        $('#alertContainer').html(html);
        setTimeout(function() { $('#alertContainer .alert').alert('close'); }, 5000);
    }
    </script>
</body>
</html>
