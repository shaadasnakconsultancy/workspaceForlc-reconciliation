package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.SettingsDao;
import com.smipl.lcrecon.dao.UserDao;
import com.smipl.lcrecon.model.User;
import com.smipl.lcrecon.service.UserEmailService;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.SecureRandom;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");

        if (username == null || email == null || username.trim().isEmpty() || email.trim().isEmpty()) {
            request.setAttribute("error", "Please enter both username and email address");
            request.getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp").forward(request, response);
            return;
        }

        UserDao userDao = (UserDao) getServletContext().getAttribute("userDao");
        User user = userDao.findByUsername(username.trim());

        if (user == null || !user.isActive() || user.getEmail() == null || !user.getEmail().equalsIgnoreCase(email.trim())) {
            // Don't reveal whether user exists - show generic message
            request.setAttribute("success", "If the username and email match, a new password has been sent to your email.");
            request.getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp").forward(request, response);
            return;
        }

        // Generate random password
        String newPassword = generateRandomPassword(10);
        userDao.updatePassword(user.getId(), LoginServlet.hashPassword(newPassword));
        // WEB_VUL_03: invalidate any active sessions for this account after the password reset
        com.smipl.lcrecon.util.SessionRegistry.invalidateAllForUser(user.getId(), null);

        // Send email
        SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");
        UserEmailService.sendForgotPasswordEmail(settingsDao.getSettingsByGroup("SMTP"),
                user.getEmail(), user.getFirstName(), user.getUsername(), newPassword);

        request.setAttribute("success", "A new password has been sent to your registered email address.");
        request.getRequestDispatcher("/WEB-INF/jsp/forgot-password.jsp").forward(request, response);
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
