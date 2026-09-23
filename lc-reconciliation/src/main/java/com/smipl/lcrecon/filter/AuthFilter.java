package com.smipl.lcrecon.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class AuthFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI().substring(request.getContextPath().length());

        if (path.equals("/login") || path.equals("/login-key") || path.equals("/forgot-password") || path.startsWith("/static/") || path.equals("/favicon.ico")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        boolean itAdmin = Boolean.TRUE.equals(session.getAttribute("isItAdmin"));
        boolean superAdmin = Boolean.TRUE.equals(session.getAttribute("isSuperAdmin"));

        // Settings: IT_ADMIN only
        if (path.startsWith("/settings")) {
            if (!itAdmin) {
                response.sendError(403, "Access denied. IT Admin role required.");
                return;
            }
        }

        // User Management, Prompt Templates, Email Templates: IT_ADMIN or SUPER_ADMIN
        if (path.startsWith("/master/users") || path.startsWith("/master/prompts") || path.startsWith("/master/email-templates")) {
            if (!itAdmin && !superAdmin) {
                response.sendError(403, "Access denied.");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
