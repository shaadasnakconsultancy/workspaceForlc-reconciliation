<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>500 - Server Error</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/static/img/favicon.png">
    <link href="${pageContext.request.contextPath}/static/vendor/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light d-flex align-items-center justify-content-center" style="height:100vh">
    <div class="text-center">
        <h1 class="display-1 text-danger">500</h1>
        <h4>Internal Server Error</h4>
        <p class="text-muted">An unexpected error occurred. Please try again later.</p>
        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary">Go to Dashboard</a>
    </div>
</body>
</html>
