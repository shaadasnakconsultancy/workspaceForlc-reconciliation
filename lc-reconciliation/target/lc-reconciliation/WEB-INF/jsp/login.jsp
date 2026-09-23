<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login - LC Reconciliation System</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        html, body { height: 100%; }
        .login-container { display: flex; height: 100vh; }

        /* Left Panel - Branding */
        .left-panel {
            flex: 1;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 40%, #0f3460 70%, #e94560 100%);
            display: flex;
            flex-direction: column;
            justify-content: center;
            align-items: center;
            padding: 3rem;
            position: relative;
            overflow: hidden;
        }
        .left-panel::before {
            content: '';
            position: absolute;
            top: -50%;
            left: -50%;
            width: 200%;
            height: 200%;
            background: radial-gradient(circle at 30% 70%, rgba(233,69,96,0.15) 0%, transparent 50%),
                        radial-gradient(circle at 70% 30%, rgba(15,52,96,0.3) 0%, transparent 50%);
            animation: float 15s ease-in-out infinite;
        }
        @keyframes float {
            0%, 100% { transform: translate(0, 0) rotate(0deg); }
            33% { transform: translate(20px, -20px) rotate(2deg); }
            66% { transform: translate(-10px, 15px) rotate(-1deg); }
        }
        .left-content {
            position: relative;
            z-index: 1;
            text-align: center;
            color: white;
        }
        .left-content .brand-logo {
            height: 60px;
            filter: brightness(0) invert(1);
            margin-bottom: 1.5rem;
        }
        .left-content h1 {
            font-size: 2.2rem;
            font-weight: 700;
            margin-bottom: 0.5rem;
            letter-spacing: 1px;
        }
        .left-content .tagline {
            font-size: 1.1rem;
            color: rgba(255,255,255,0.7);
            margin-bottom: 2rem;
        }
        .feature-list {
            text-align: left;
            list-style: none;
            padding: 0;
        }
        .feature-list li {
            color: rgba(255,255,255,0.8);
            padding: 0.5rem 0;
            font-size: 0.95rem;
        }
        .feature-list li i {
            color: #e94560;
            margin-right: 0.75rem;
            font-size: 1.1rem;
        }
        /* Decorative shapes */
        .shape {
            position: absolute;
            border-radius: 50%;
            opacity: 0.08;
            background: white;
        }
        .shape-1 { width: 300px; height: 300px; bottom: -100px; left: -50px; }
        .shape-2 { width: 200px; height: 200px; top: 50px; right: -60px; }
        .shape-3 { width: 150px; height: 150px; top: 40%; left: 10%; opacity: 0.05; }

        /* Motorcycle silhouette using CSS */
        .moto-icon {
            font-size: 5rem;
            color: rgba(233,69,96,0.3);
            margin-bottom: 1rem;
        }

        /* Right Panel - Login Form */
        .right-panel {
            width: 480px;
            min-width: 400px;
            display: flex;
            flex-direction: column;
            justify-content: center;
            padding: 3rem;
            background: #fff;
        }
        .login-header {
            margin-bottom: 2rem;
        }
        .login-header h3 {
            font-weight: 700;
            color: #1a1a2e;
        }
        .login-header p {
            color: #6c757d;
        }
        .form-control:focus {
            border-color: #e94560;
            box-shadow: 0 0 0 0.2rem rgba(233,69,96,0.15);
        }
        .btn-login {
            background: linear-gradient(135deg, #e94560, #0f3460);
            border: none;
            padding: 0.7rem;
            font-weight: 600;
            letter-spacing: 0.5px;
        }
        .btn-login:hover {
            background: linear-gradient(135deg, #d63851, #0d2d52);
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(233,69,96,0.3);
        }
        .footer-text {
            margin-top: 2rem;
            text-align: center;
            font-size: 0.8rem;
            color: #adb5bd;
        }

        /* Responsive */
        @media (max-width: 991px) {
            .left-panel { display: none; }
            .right-panel { width: 100%; min-width: unset; }
            .login-container { justify-content: center; background: #f5f6fa; }
        }
    </style>
</head>
<body>
    <div class="login-container">
        <!-- Left Panel -->
        <div class="left-panel">
            <div class="shape shape-1"></div>
            <div class="shape shape-2"></div>
            <div class="shape shape-3"></div>
            <div class="left-content">
                <div class="moto-icon"><i class="bi bi-speedometer2"></i></div>
                <img src="${pageContext.request.contextPath}/static/img/suzuki-logo.png" alt="Suzuki" class="brand-logo">
                <h1>LC Reconciliation</h1>
                <p class="tagline">Automated Letter of Credit Document Compliance</p>
                <ul class="feature-list">
                    <li><i class="bi bi-check-circle-fill"></i> AI-Powered Document OCR & Extraction</li>
                    <li><i class="bi bi-check-circle-fill"></i> Multi-Document Compliance Verification</li>
                    <li><i class="bi bi-check-circle-fill"></i> Automated Report Generation</li>
                    <li><i class="bi bi-check-circle-fill"></i> Real-time Job Monitoring & Analytics</li>
                    <li><i class="bi bi-check-circle-fill"></i> Cost Tracking & Management Dashboard</li>
                </ul>
            </div>
        </div>

        <!-- Right Panel -->
        <div class="right-panel">
            <div class="login-header">
                <h3><i class="bi bi-shield-lock"></i> Welcome Back</h3>
                <p>Sign in to your account to continue</p>
            </div>

            <c:if test="${not empty error}">
                <div class="alert alert-danger alert-dismissible fade show" role="alert">
                    ${error}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login">
                <div class="mb-3">
                    <label for="username" class="form-label fw-semibold">Username</label>
                    <div class="input-group input-group-lg">
                        <span class="input-group-text bg-light"><i class="bi bi-person"></i></span>
                        <input type="text" class="form-control" id="username" name="username"
                               value="${username}" placeholder="Enter your username" required autofocus>
                    </div>
                </div>
                <div class="mb-4">
                    <label for="password" class="form-label fw-semibold">Password</label>
                    <div class="input-group input-group-lg">
                        <span class="input-group-text bg-light"><i class="bi bi-lock"></i></span>
                        <input type="password" class="form-control" id="password" name="password"
                               placeholder="Enter your password" required>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary btn-login btn-lg w-100 mb-3">
                    <i class="bi bi-box-arrow-in-right"></i> Sign In
                </button>
                <div class="text-center">
                    <a href="${pageContext.request.contextPath}/forgot-password" class="text-decoration-none" style="color:#e94560">
                        <i class="bi bi-key"></i> Forgot Password?
                    </a>
                </div>
            </form>

            <div class="footer-text">
                &copy; 2026 Suzuki Motorcycle India Pvt. Ltd.<br>
                LC Reconciliation System
            </div>
        </div>
    </div>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
