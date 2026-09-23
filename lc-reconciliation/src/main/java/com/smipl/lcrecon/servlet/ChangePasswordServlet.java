package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.SettingsDao;
import com.smipl.lcrecon.dao.UserDao;
import com.smipl.lcrecon.model.User;
import com.smipl.lcrecon.service.UserEmailService;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet("/change-password")
public class ChangePasswordServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ChangePasswordServlet.class);

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UserDao userDao = (UserDao) getServletContext().getAttribute("userDao");
        User sessionUser = (User) request.getSession().getAttribute("user");

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            String currentPassword = (String) body.get("currentPassword");
            String newPassword = (String) body.get("newPassword");
            String confirmPassword = (String) body.get("confirmPassword");

            if (currentPassword == null || newPassword == null || confirmPassword == null) {
                JsonUtil.writeError(response, "All password fields are required");
                return;
            }

            if (newPassword.trim().length() < 6) {
                JsonUtil.writeError(response, "New password must be at least 6 characters");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JsonUtil.writeError(response, "New password and confirmation do not match");
                return;
            }

            // Verify current password
            String currentHash = LoginServlet.hashPassword(currentPassword);
            User dbUser = userDao.findById(sessionUser.getId());
            if (!dbUser.getPasswordHash().equals(currentHash)) {
                JsonUtil.writeError(response, "Current password is incorrect");
                return;
            }

            userDao.updatePassword(sessionUser.getId(), LoginServlet.hashPassword(newPassword));

            // Send the confirmation email before the session goes away - it only needs dbUser.
            if (dbUser.getEmail() != null && !dbUser.getEmail().isEmpty()) {
                SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");
                UserEmailService.sendSelfPasswordResetEmail(settingsDao.getSettingsByGroup("SMTP"),
                        dbUser.getEmail(), dbUser.getFirstName(), dbUser.getUsername());
            }

            // WEB_VUL_03: invalidate every session for this user, including the one that made the
            // change, so the new password must be used to get back in. The response is written
            // after this; the "logout" flag tells the page to redirect to the login screen.
            com.smipl.lcrecon.util.SessionRegistry.invalidateAllForUser(sessionUser.getId(), null);
            try {
                request.getSession(false).invalidate();
            } catch (Exception ignored) {
                // already invalidated by the registry sweep
            }

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("logout", true);
            JsonUtil.writeSuccess(response, "Password changed successfully. Please sign in again.", data);
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Change password", e);
        }
    }
}
