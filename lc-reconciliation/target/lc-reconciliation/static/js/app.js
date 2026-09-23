// LC Reconciliation - Common JavaScript

$(document).ready(function() {
    // Sidebar collapse/expand toggle
    var sidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
    if (sidebarCollapsed && $(window).width() >= 768) {
        $('#sidebar').addClass('collapsed');
        $('body').addClass('sidebar-collapsed');
        $('#sidebarCollapseIcon').removeClass('bi-layout-sidebar-inset').addClass('bi-layout-sidebar');
    }

    $('#sidebarCollapseBtn').on('click', function() {
        if ($(window).width() < 768) {
            // Mobile: show/hide
            $('#sidebar').toggleClass('show');
        } else {
            // Desktop: collapse/expand
            $('#sidebar').toggleClass('collapsed');
            $('body').toggleClass('sidebar-collapsed');
            var isCollapsed = $('#sidebar').hasClass('collapsed');
            localStorage.setItem('sidebarCollapsed', isCollapsed);
            $('#sidebarCollapseIcon').toggleClass('bi-layout-sidebar-inset bi-layout-sidebar');
        }
    });

    // Close sidebar when clicking outside on mobile
    $(document).on('click', function(e) {
        if ($(window).width() < 768) {
            if (!$(e.target).closest('#sidebar, #sidebarCollapseBtn').length) {
                $('#sidebar').removeClass('show');
            }
        }
    });
});

// Toast notification
function showToast(message, type) {
    type = type || 'success';
    var bgClass = type === 'error' ? 'bg-danger' : type === 'warning' ? 'bg-warning text-dark' : 'bg-success';
    var toastHtml = '<div class="toast align-items-center text-white ' + bgClass + ' border-0" role="alert">' +
        '<div class="d-flex">' +
        '<div class="toast-body">' + message + '</div>' +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>' +
        '</div></div>';

    var container = $('.toast-container');
    if (container.length === 0) {
        container = $('<div class="toast-container"></div>');
        $('body').append(container);
    }

    var toastEl = $(toastHtml);
    container.append(toastEl);
    var toast = new bootstrap.Toast(toastEl[0], { delay: 4000 });
    toast.show();

    toastEl.on('hidden.bs.toast', function() {
        $(this).remove();
    });
}

// Confirm dialog
function confirmAction(message, callback) {
    if (confirm(message)) {
        callback();
    }
}

// Format file size
function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

// Theme toggle
function toggleTheme() {
    var body = document.body;
    var icon = document.getElementById('themeIcon');
    if (body.classList.contains('dark-theme')) {
        body.classList.remove('dark-theme');
        icon.className = 'bi bi-moon-fill';
        localStorage.setItem('theme', 'light');
    } else {
        body.classList.add('dark-theme');
        icon.className = 'bi bi-sun-fill';
        localStorage.setItem('theme', 'dark');
    }
}

// Apply saved theme on load
(function() {
    var saved = localStorage.getItem('theme');
    if (saved === 'dark') {
        document.body.classList.add('dark-theme');
        var icon = document.getElementById('themeIcon');
        if (icon) icon.className = 'bi bi-sun-fill';
    }
})();
