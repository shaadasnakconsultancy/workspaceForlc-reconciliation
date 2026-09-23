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
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/master/users")
public class UserManagementServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserManagementServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDao userDao = (UserDao) getServletContext().getAttribute("userDao");

        String format = request.getParameter("format");
        if ("json".equals(format)) {
            List<User> users = userDao.findAll();
            // Clear password hashes before sending
            for (User u : users) u.setPasswordHash(null);
            JsonUtil.writeJsonResponse(response, users);
            return;
        }

        List<User> users = userDao.findAll();
        request.setAttribute("users", users);
        request.getRequestDispatcher("/WEB-INF/jsp/master/users.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserDao userDao = (UserDao) getServletContext().getAttribute("userDao");

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            String action = (String) body.get("action");

            if ("create".equals(action)) {
                String username = (String) body.get("username");
                // Check duplicate username
                if (userDao.findByUsername(username) != null) {
                    JsonUtil.writeError(response, "Username '" + username + "' already exists");
                    return;
                }
                String password = (String) body.get("password");
                if (password == null || password.trim().length() < 6) {
                    JsonUtil.writeError(response, "Password must be at least 6 characters");
                    return;
                }

                User user = new User();
                user.setUsername(username);
                user.setPasswordHash(LoginServlet.hashPassword(password));
                user.setFirstName((String) body.get("firstName"));
                user.setLastName((String) body.get("lastName"));
                user.setEmail((String) body.get("email"));
                user.setDepartment((String) body.get("department"));
                user.setRole((String) body.getOrDefault("role", "DEPARTMENT_USER"));
                user.setActive(body.containsKey("isActive") ? JsonUtil.asBoolean(body.get("isActive"), false) : true);
                userDao.create(user);
                // Send welcome email with credentials
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");
                    UserEmailService.sendNewUserEmail(settingsDao.getSettingsByGroup("SMTP"),
                            user.getEmail(), user.getFirstName(), username, password);
                }
                JsonUtil.writeSuccess(response, "User created successfully");

            } else if ("update".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                User user = userDao.findById(id);
                if (user == null) {
                    JsonUtil.writeError(response, "User not found");
                    return;
                }
                if (body.containsKey("firstName")) user.setFirstName((String) body.get("firstName"));
                if (body.containsKey("lastName")) user.setLastName((String) body.get("lastName"));
                if (body.containsKey("email")) user.setEmail((String) body.get("email"));
                if (body.containsKey("department")) user.setDepartment((String) body.get("department"));
                if (body.containsKey("role")) user.setRole((String) body.get("role"));
                if (body.containsKey("isActive")) user.setActive(JsonUtil.asBoolean(body.get("isActive"), false));
                userDao.update(user);
                JsonUtil.writeSuccess(response, "User updated successfully");

            } else if ("delete".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                // Prevent deleting self
                User sessionUser = (User) request.getSession().getAttribute("user");
                if (sessionUser.getId() == id) {
                    JsonUtil.writeError(response, "Cannot delete your own account");
                    return;
                }
                // WEB_VUL_06: verify the user exists before reporting success
                if (userDao.findById(id) == null) {
                    logger.warn("Delete user requested for non-existent id {} by {}", id, sessionUser.getUsername());
                    JsonUtil.writeError(response, "User not found");
                    return;
                }
                userDao.delete(id);
                JsonUtil.writeSuccess(response, "User deleted successfully");

            } else if ("resetPassword".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                String newPassword = (String) body.get("newPassword");
                if (newPassword == null || newPassword.trim().length() < 6) {
                    JsonUtil.writeError(response, "Password must be at least 6 characters");
                    return;
                }
                // WEB_VUL_06: verify the user exists before performing the reset
                User targetUser = userDao.findById(id);
                if (targetUser == null) {
                    User admin = (User) request.getSession().getAttribute("user");
                    logger.warn("Reset password requested for non-existent id {} by {}", id,
                            admin != null ? admin.getUsername() : "unknown");
                    JsonUtil.writeError(response, "User not found");
                    return;
                }
                User adminUser = (User) request.getSession().getAttribute("user");
                boolean resettingOwnPassword = adminUser != null && adminUser.getId() == id;

                userDao.updatePassword(id, LoginServlet.hashPassword(newPassword));

                // Send the email before any session teardown - it only needs targetUser.
                if (targetUser.getEmail() != null && !targetUser.getEmail().isEmpty()) {
                    SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");
                    String adminName = adminUser != null ? adminUser.getFullName() : "Administrator";
                    UserEmailService.sendPasswordResetByAdminEmail(settingsDao.getSettingsByGroup("SMTP"),
                            targetUser.getEmail(), targetUser.getFirstName(), targetUser.getUsername(), newPassword, adminName);
                }

                // WEB_VUL_03: force the target user to re-authenticate by invalidating all their sessions
                com.smipl.lcrecon.util.SessionRegistry.invalidateAllForUser(id, null);

                if (resettingOwnPassword) {
                    // An admin resetting their own password must be signed out too. The registry
                    // sweep above normally covers it, but the current session is invalidated
                    // explicitly in case it was never registered (for example after a redeploy).
                    try {
                        javax.servlet.http.HttpSession session = request.getSession(false);
                        if (session != null) session.invalidate();
                    } catch (IllegalStateException ignored) {
                        // already invalidated
                    }
                    Map<String, Object> data = new java.util.LinkedHashMap<>();
                    data.put("logout", true);
                    JsonUtil.writeSuccess(response, "Your password was reset. Please sign in again.", data);
                } else {
                    JsonUtil.writeSuccess(response, "Password reset successfully");
                }

            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeError(response, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "User management operation", e);
        }
    }
}
