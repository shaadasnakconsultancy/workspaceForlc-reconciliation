package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.UserDao;
import com.smipl.lcrecon.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LoginServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect(request.getContextPath() + "/dashboard");
            return;
        }
        request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        // WEB_VUL_07: the browser encrypts the password (RSA-OAEP via Web Crypto) so plaintext
        // is never transmitted. Decrypt it here. Plaintext 'password' remains only as a safety net.
        String encryptedPassword = request.getParameter("encryptedPassword");
        if (encryptedPassword != null && !encryptedPassword.trim().isEmpty()) {
            try {
                password = com.smipl.lcrecon.util.CryptoUtil.decryptBase64(encryptedPassword.trim());
            } catch (Exception e) {
                logger.warn("Failed to decrypt login credentials: {}", e.getMessage());
                password = null;
            }
        }

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            request.setAttribute("error", "Please enter username and password");
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }

        // WEB_VUL_04: rate-limit authentication attempts (per client IP + username)
        String rateKey = clientIp(request) + "|" + username.trim().toLowerCase();
        long now = System.currentTimeMillis();
        if (com.smipl.lcrecon.util.LoginRateLimiter.isLocked(rateKey, now)) {
            long mins = (com.smipl.lcrecon.util.LoginRateLimiter.retryAfterSeconds(rateKey, now) + 59) / 60;
            logger.warn("Login blocked (rate limit) for user {} from {}", username, clientIp(request));
            request.setAttribute("error", "Too many failed login attempts. Please try again in about " + mins + " minute(s).");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
            return;
        }

        UserDao userDao = (UserDao) getServletContext().getAttribute("userDao");
        String passwordHash = hashPassword(password);
        User user = userDao.findByUsername(username.trim());

        if (user != null && user.isActive() && user.getPasswordHash().equals(passwordHash)) {
            com.smipl.lcrecon.util.LoginRateLimiter.reset(rateKey);
            HttpSession session = request.getSession(true);
            // WEB_VUL_09: regenerate the session id upon authentication to prevent session fixation
            request.changeSessionId();
            session.setAttribute("user", user);
            session.setAttribute("username", user.getUsername());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userRole", user.getRole());
            session.setAttribute("userDepartment", user.getDepartment());
            session.setAttribute("isItAdmin", user.isItAdmin());
            session.setAttribute("isSuperAdmin", user.isSuperAdmin());
            // Track this session so it can be invalidated on a later password change (WEB_VUL_03)
            com.smipl.lcrecon.util.SessionRegistry.register(user.getId(), session);
            logger.info("User logged in: {}", username);
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            com.smipl.lcrecon.util.LoginRateLimiter.recordFailure(rateKey, now);
            request.setAttribute("error", "Invalid username or password");
            request.setAttribute("username", username);
            request.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(request, response);
        }
    }

    /** Best-effort client IP, honoring a reverse proxy's X-Forwarded-For first hop. */
    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.trim().isEmpty()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
