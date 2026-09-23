package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.EmailGroupDao;
import com.smipl.lcrecon.model.EmailGroup;
import com.smipl.lcrecon.model.EmailGroupMember;
import com.smipl.lcrecon.util.InputValidator;
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

@WebServlet("/master/email-groups")
public class EmailGroupServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailGroupServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EmailGroupDao dao = (EmailGroupDao) getServletContext().getAttribute("emailGroupDao");

        String format = request.getParameter("format");
        if ("json".equals(format)) {
            List<EmailGroup> groups = dao.findActive();
            JsonUtil.writeJsonResponse(response, groups);
            return;
        }

        if ("byId".equals(format)) {
            String groupIdParam = request.getParameter("groupId");
            if (groupIdParam == null || groupIdParam.isEmpty()) {
                JsonUtil.writeError(response, "groupId is required");
                return;
            }
            long groupId = Long.parseLong(groupIdParam);
            EmailGroup group = dao.findById(groupId);
            if (group == null) {
                JsonUtil.writeError(response, "Email group not found");
                return;
            }
            JsonUtil.writeJsonResponse(response, group);
            return;
        }

        List<EmailGroup> emailGroups = dao.findAll();
        request.setAttribute("emailGroups", emailGroups);
        request.getRequestDispatcher("/WEB-INF/jsp/master/email-groups.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EmailGroupDao dao = (EmailGroupDao) getServletContext().getAttribute("emailGroupDao");

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            String action = (String) body.get("action");

            if ("create".equals(action)) {
                InputValidator.rejectHtml("Group Name", (String) body.get("groupName"));
                InputValidator.rejectHtml("Description", (String) body.get("description"));
                EmailGroup group = new EmailGroup();
                group.setGroupName((String) body.get("groupName"));
                group.setDescription((String) body.get("description"));
                group.setActive(true);
                dao.create(group);
                JsonUtil.writeSuccess(response, "Email group created");
            } else if ("update".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                EmailGroup group = dao.findById(id);
                if (group == null) {
                    JsonUtil.writeError(response, "Email group not found");
                    return;
                }
                if (body.containsKey("groupName")) { InputValidator.rejectHtml("Group Name", (String) body.get("groupName")); group.setGroupName((String) body.get("groupName")); }
                if (body.containsKey("description")) { InputValidator.rejectHtml("Description", (String) body.get("description")); group.setDescription((String) body.get("description")); }
                if (body.containsKey("isActive")) group.setActive(JsonUtil.asBoolean(body.get("isActive"), false));
                dao.update(group);
                JsonUtil.writeSuccess(response, "Email group updated");
            } else if ("delete".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                dao.delete(id);
                JsonUtil.writeSuccess(response, "Email group deleted");
            } else if ("addMember".equals(action)) {
                InputValidator.rejectHtml("Member Name", (String) body.get("memberName"));
                InputValidator.rejectHtml("Email Address", (String) body.get("emailAddress"));
                EmailGroupMember member = new EmailGroupMember();
                member.setGroupId(JsonUtil.asLong(body.get("groupId"), "groupId"));
                member.setEmailAddress((String) body.get("emailAddress"));
                member.setMemberName((String) body.get("memberName"));
                member.setActive(true);
                dao.addMember(member);
                JsonUtil.writeSuccess(response, "Member added");
            } else if ("removeMember".equals(action)) {
                long memberId = JsonUtil.asLong(body.get("memberId"), "memberId");
                dao.removeMember(memberId);
                JsonUtil.writeSuccess(response, "Member removed");
            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeError(response, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Email group operation", e);
        }
    }
}
