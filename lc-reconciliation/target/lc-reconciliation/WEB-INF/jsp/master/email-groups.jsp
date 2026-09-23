<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Email Groups - LC Reconciliation</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/app.css" rel="stylesheet">
    <style>
        .group-row { cursor: pointer; }
        .group-row:hover { background-color: #f0f4ff !important; }
        .group-row.selected { background-color: #e8f0fe !important; border-left: 3px solid #0d6efd; }
        .members-section { display: none; }
        .members-section.active { display: block; }
    </style>
</head>
<body>
    <c:set var="pageTitle" value="Email Groups" scope="request" />
    <%@ include file="../layout/sidebar.jsp" %>
    <div class="main-content">
        <%@ include file="../layout/header.jsp" %>
        <div class="container-fluid p-4">

            <div id="alertContainer"></div>

            <!-- Groups Table -->
            <div class="card">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0"><i class="bi bi-people-fill"></i> Email Groups</h5>
                    <button class="btn btn-primary btn-sm" onclick="openGroupModal()">
                        <i class="bi bi-plus-circle"></i> Add Group
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0" id="groupsTable">
                            <thead class="table-light">
                                <tr>
                                    <th>Group Name</th>
                                    <th>Description</th>
                                    <th>Members Count</th>
                                    <th>Active</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="g" items="${emailGroups}">
                                    <tr class="group-row" data-group-id="${g.id}" onclick="selectGroup(${g.id}, '${g.groupName}')">
                                        <td><i class="bi bi-people me-1"></i> <strong>${g.groupName}</strong></td>
                                        <td><span class="text-muted">${g.description}</span></td>
                                        <td>
                                            <span class="badge bg-info">${g.memberCount}</span>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${g.active}">
                                                    <span class="badge bg-success">Active</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-secondary">Inactive</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-nowrap" onclick="event.stopPropagation();">
                                            <button class="btn btn-sm btn-outline-info" title="Manage Members"
                                                    onclick="selectGroup(${g.id}, '${g.groupName}')">
                                                <i class="bi bi-person-lines-fill"></i>
                                            </button>
                                            <button class="btn btn-sm btn-outline-primary" title="Edit"
                                                    onclick="editGroup(${g.id}, '${g.groupName}', '${g.description}', ${g.active})">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <button class="btn btn-sm btn-outline-danger" title="Delete"
                                                    onclick="deleteGroup(${g.id}, '${g.groupName}')">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty emailGroups}">
                                    <tr><td colspan="5" class="text-center text-muted py-4">No email groups configured. Click "Add Group" to create one.</td></tr>
                                </c:if>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- Members Section -->
            <div class="card mt-3 members-section" id="membersSection">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <h5 class="mb-0">
                        <i class="bi bi-person-lines-fill"></i>
                        Members of: <span id="selectedGroupName" class="text-primary"></span>
                    </h5>
                    <button class="btn btn-primary btn-sm" onclick="openMemberModal()">
                        <i class="bi bi-plus-circle"></i> Add Member
                    </button>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover mb-0" id="membersTable">
                            <thead class="table-light">
                                <tr>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Active</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="membersTableBody">
                                <tr><td colspan="4" class="text-center text-muted py-4">Select a group to view members.</td></tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

        </div>
        <%@ include file="../layout/footer.jsp" %>
    </div>

    <!-- Add/Edit Group Modal -->
    <div class="modal fade" id="groupModal" tabindex="-1" aria-labelledby="groupModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="groupModalLabel">Add Email Group</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form id="groupForm">
                    <div class="modal-body">
                        <input type="hidden" id="groupId" name="id">
                        <div class="mb-3">
                            <label for="groupName" class="form-label">Group Name <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="groupName" name="groupName"
                                   placeholder="e.g. Finance Team" required maxlength="100">
                        </div>
                        <div class="mb-3">
                            <label for="groupDescription" class="form-label">Description</label>
                            <input type="text" class="form-control" id="groupDescription" name="description"
                                   placeholder="e.g. Finance department email recipients" maxlength="255">
                        </div>
                        <div class="mb-3 form-check">
                            <input type="checkbox" class="form-check-input" id="groupActive" name="active" checked>
                            <label class="form-check-label" for="groupActive">Active</label>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary" id="saveGroupBtn">
                            <i class="bi bi-save"></i> Save
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Add Member Modal -->
    <div class="modal fade" id="memberModal" tabindex="-1" aria-labelledby="memberModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="memberModalLabel">Add Member</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <form id="memberForm">
                    <div class="modal-body">
                        <div class="mb-3">
                            <label for="memberName" class="form-label">Member Name <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" id="memberName" name="memberName"
                                   placeholder="e.g. John Doe" required maxlength="100">
                        </div>
                        <div class="mb-3">
                            <label for="memberEmail" class="form-label">Email Address <span class="text-danger">*</span></label>
                            <input type="email" class="form-control" id="memberEmail" name="emailAddress"
                                   placeholder="e.g. john.doe@suzuki.co.in" required maxlength="255">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-person-plus"></i> Add Member
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>
    <script>
    var contextPath = '${pageContext.request.contextPath}';
    var groupModal = null;
    var memberModal = null;
    var selectedGroupId = null;

    $(function() {
        groupModal = new bootstrap.Modal(document.getElementById('groupModal'));
        memberModal = new bootstrap.Modal(document.getElementById('memberModal'));

        $('#groupForm').on('submit', function(e) {
            e.preventDefault();
            saveGroup();
        });

        $('#memberForm').on('submit', function(e) {
            e.preventDefault();
            addMember();
        });
    });

    // ---- Group CRUD ----

    function openGroupModal() {
        $('#groupModalLabel').text('Add Email Group');
        $('#groupForm')[0].reset();
        $('#groupId').val('');
        $('#groupActive').prop('checked', true);
        groupModal.show();
    }

    function editGroup(id, name, description, active) {
        $('#groupModalLabel').text('Edit Email Group');
        $('#groupId').val(id);
        $('#groupName').val(name);
        $('#groupDescription').val(description);
        $('#groupActive').prop('checked', active);
        groupModal.show();
    }

    function saveGroup() {
        var id = $('#groupId').val();
        var data = {
            action: id ? 'update' : 'create',
            groupName: $('#groupName').val().trim(),
            description: $('#groupDescription').val().trim(),
            isActive: $('#groupActive').is(':checked')
        };
        if (!data.groupName) {
            showAlert('danger', 'Group name is required.');
            return;
        }
        if (id) data.id = parseInt(id);

        $.ajax({
            url: contextPath + '/master/email-groups',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                if (response.success) {
                    groupModal.hide();
                    showAlert('success', response.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to save group.');
            }
        });
    }

    function deleteGroup(id, name) {
        if (!confirm('Are you sure you want to delete email group "' + name + '"? All members will also be removed.')) return;

        $.ajax({
            url: contextPath + '/master/email-groups',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'delete', id: id }),
            success: function(response) {
                if (response.success) {
                    showAlert('success', response.message);
                    setTimeout(function() { location.reload(); }, 1000);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to delete group.');
            }
        });
    }

    // ---- Members ----

    function selectGroup(groupId, groupName) {
        selectedGroupId = groupId;
        $('#selectedGroupName').text(groupName);
        $('#membersSection').addClass('active');

        // Highlight selected row
        $('.group-row').removeClass('selected');
        $('.group-row[data-group-id="' + groupId + '"]').addClass('selected');

        loadGroupMembers(groupId);
    }

    function loadGroupMembers(groupId) {
        $.ajax({
            url: contextPath + '/master/email-groups?format=byId&groupId=' + groupId,
            type: 'GET',
            success: function(data) {
                var members = data.members || [];
                var tbody = $('#membersTableBody');
                tbody.empty();

                if (members.length === 0) {
                    tbody.html('<tr><td colspan="4" class="text-center text-muted py-4">No members in this group. Click "Add Member" to add one.</td></tr>');
                    return;
                }

                $.each(members, function(i, m) {
                    var activeHtml = m.active
                        ? '<span class="badge bg-success">Active</span>'
                        : '<span class="badge bg-secondary">Inactive</span>';
                    var row = '<tr>' +
                        '<td><i class="bi bi-person me-1"></i> ' + escapeHtml(m.memberName) + '</td>' +
                        '<td><a href="mailto:' + escapeHtml(m.emailAddress) + '">' + escapeHtml(m.emailAddress) + '</a></td>' +
                        '<td>' + activeHtml + '</td>' +
                        '<td>' +
                            '<button class="btn btn-sm btn-outline-danger" title="Remove" onclick="removeMember(' + m.id + ', \'' + escapeHtml(m.memberName) + '\')">' +
                                '<i class="bi bi-person-x"></i> Remove' +
                            '</button>' +
                        '</td>' +
                        '</tr>';
                    tbody.append(row);
                });
            },
            error: function() {
                showAlert('danger', 'Failed to load group members.');
            }
        });
    }

    function openMemberModal() {
        if (!selectedGroupId) {
            showAlert('warning', 'Please select a group first.');
            return;
        }
        $('#memberModalLabel').text('Add Member');
        $('#memberForm')[0].reset();
        memberModal.show();
    }

    function addMember() {
        var data = {
            action: 'addMember',
            groupId: selectedGroupId,
            emailAddress: $('#memberEmail').val().trim(),
            memberName: $('#memberName').val().trim()
        };

        if (!data.emailAddress || !data.memberName) {
            showAlert('danger', 'Name and email are required.');
            return;
        }

        $.ajax({
            url: contextPath + '/master/email-groups',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify(data),
            success: function(response) {
                if (response.success) {
                    memberModal.hide();
                    showAlert('success', response.message);
                    loadGroupMembers(selectedGroupId);
                    // Update members count badge in groups table
                    setTimeout(function() { location.reload(); }, 1500);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to add member.');
            }
        });
    }

    function removeMember(memberId, memberName) {
        if (!confirm('Remove member "' + memberName + '" from this group?')) return;

        $.ajax({
            url: contextPath + '/master/email-groups',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ action: 'removeMember', memberId: memberId }),
            success: function(response) {
                if (response.success) {
                    showAlert('success', response.message);
                    loadGroupMembers(selectedGroupId);
                    // Update members count badge in groups table
                    setTimeout(function() { location.reload(); }, 1500);
                } else {
                    showAlert('danger', response.message);
                }
            },
            error: function() {
                showAlert('danger', 'Failed to remove member.');
            }
        });
    }

    // ---- Utilities ----

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
                  .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function showAlert(type, message) {
        var alertHtml = '<div class="alert alert-' + type + ' alert-dismissible fade show" role="alert">'
            + message
            + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>';
        $('#alertContainer').html(alertHtml);
        setTimeout(function() { $('#alertContainer .alert').alert('close'); }, 5000);
    }
    </script>
</body>
</html>
