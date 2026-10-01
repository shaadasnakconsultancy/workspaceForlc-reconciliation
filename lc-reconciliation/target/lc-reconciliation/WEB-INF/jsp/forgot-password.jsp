<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Forgot Password - LC Reconciliation System</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap-icons.css" rel="stylesheet">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { height: 100%; }
        .login-container { display: flex; height: 100vh; }
        .left-panel {
            flex: 1;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 70%, #e94560 100%);
            display: flex; flex-direction: column; justify-content: center; align-items: center;
            padding: 3rem; position: relative; overflow: hidden;
        }
        .left-panel::before {
            content: ''; position: absolute; top: -50%; left: -50%; width: 200%; height: 200%;
            background: radial-gradient(circle at 30% 70%, rgba(233,69,96,0.15) 0%, transparent 50%),
                        radial-gradient(circle at 70% 30%, rgba(15,52,96,0.3) 0%, transparent 50%);
        }
        .left-content { position: relative; z-index: 1; text-align: center; color: white; }
        .left-content .brand-logo { height: 60px; filter: brightness(0) invert(1); margin-bottom: 1.5rem; }
        .left-content h1 { font-size: 2.2rem; font-weight: 700; margin-bottom: 0.5rem; }
        .left-content .tagline { font-size: 1.1rem; color: rgba(255,255,255,0.7); }
        .shape { position: absolute; border-radius: 50%; opacity: 0.08; background: white; }
        .shape-1 { width: 300px; height: 300px; bottom: -100px; left: -50px; }
        .shape-2 { width: 200px; height: 200px; top: 50px; right: -60px; }
        .moto-icon { font-size: 5rem; color: rgba(233,69,96,0.3); margin-bottom: 1rem; }
        .right-panel {
            width: 480px; min-width: 400px; display: flex; flex-direction: column;
            justify-content: center; padding: 3rem; background: #fff;
        }
        .form-control:focus { border-color: #e94560; box-shadow: 0 0 0 0.2rem rgba(233,69,96,0.15); }
        .btn-reset {
            background: linear-gradient(135deg, #0f3460, #e94560); border: none;
            padding: 0.7rem; font-weight: 600;
        }
        .btn-reset:hover { background: linear-gradient(135deg, #0d2d52, #d63851); transform: translateY(-1px); }
        .footer-text { margin-top: 2rem; text-align: center; font-size: 0.8rem; color: #adb5bd; }
        @media (max-width: 991px) {
            .left-panel { display: none; }
            .right-panel { width: 100%; min-width: unset; }
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="left-panel">
            <div class="shape shape-1"></div>
            <div class="shape shape-2"></div>
            <div class="left-content">
                <div class="moto-icon"><i class="bi bi-key-fill"></i></div>
                <img src="${pageContext.request.contextPath}/static/img/suzuki-logo.png" alt="Suzuki" class="brand-logo">
                <h1>Password Recovery</h1>
                <p class="tagline">We'll send a new password to your registered email</p>
            </div>
        </div>

        <div class="right-panel">
            <div class="mb-4">
                <h3 class="fw-bold" style="color:#1a1a2e"><i class="bi bi-key"></i> Forgot Password</h3>
                <p class="text-muted">Enter your username and registered email address</p>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert-danger alert-dismissible fade show">${error}<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
            </c:if>
            <c:if test="${not empty success}">
                <div class="alert alert-success alert-dismissible fade show">${success}<button type="button" class="btn-close" data-bs-dismiss="alert"></button></div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/forgot-password">
                <div class="mb-3">
                    <label for="username" class="form-label fw-semibold">Username</label>
                    <div class="input-group input-group-lg">
                        <span class="input-group-text bg-light"><i class="bi bi-person"></i></span>
                        <input type="text" class="form-control" id="username" name="username"
                               value="<c:out value='${param.username}'/>" placeholder="Enter your username" required autofocus>
                    </div>
                </div>
                <div class="mb-4">
                    <label for="email" class="form-label fw-semibold">Registered Email</label>
                    <div class="input-group input-group-lg">
                        <span class="input-group-text bg-light"><i class="bi bi-envelope"></i></span>
                        <input type="email" class="form-control" id="email" name="email"
                               placeholder="Enter your registered email" required>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-reset btn-lg w-100 mb-3">
                    <i class="bi bi-send"></i> Reset Password
                </button>
                <div class="text-center">
                    <a href="${pageContext.request.contextPath}/login" class="text-decoration-none" style="color:#0f3460">
                        <i class="bi bi-arrow-left"></i> Back to Login
                    </a>
                </div>
            </form>

            <div class="footer-text">
                &copy; 2026 Suzuki Motorcycle India Pvt. Ltd.<br>LC Reconciliation System
            </div>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/static/vendor/bootstrap.bundle.min.js"></script>
</body>
</html>
