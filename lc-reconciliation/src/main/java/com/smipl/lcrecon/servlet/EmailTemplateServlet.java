package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.EmailTemplateDao;
import com.smipl.lcrecon.model.EmailTemplate;
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

@WebServlet("/master/email-templates")
public class EmailTemplateServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(EmailTemplateServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EmailTemplateDao dao = (EmailTemplateDao) getServletContext().getAttribute("emailTemplateDao");

        String format = request.getParameter("format");
        if ("byId".equals(format)) {
            String templateIdStr = request.getParameter("templateId");
            if (templateIdStr != null) {
                EmailTemplate template = dao.findById(Long.parseLong(templateIdStr));
                if (template != null) {
                    JsonUtil.writeJsonResponse(response, template);
                } else {
                    JsonUtil.writeError(response, "Template not found");
                }
            }
            return;
        }
        if ("json".equals(format)) {
            List<EmailTemplate> templates = dao.findAll();
            JsonUtil.writeJsonResponse(response, templates);
            return;
        }

        List<EmailTemplate> emailTemplates = dao.findAll();
        request.setAttribute("emailTemplates", emailTemplates);
        request.getRequestDispatcher("/WEB-INF/jsp/master/email-templates.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        EmailTemplateDao dao = (EmailTemplateDao) getServletContext().getAttribute("emailTemplateDao");

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
                // Body template intentionally allows HTML (email body); name & subject are plain text.
                InputValidator.rejectHtml("Template Name", (String) body.get("templateName"));
                InputValidator.rejectHtml("Subject Template", (String) body.get("subjectTemplate"));
                EmailTemplate template = new EmailTemplate();
                template.setTemplateName((String) body.get("templateName"));
                template.setSubjectTemplate((String) body.get("subjectTemplate"));
                template.setBodyTemplate((String) body.get("bodyTemplate"));
                template.setDefaultTemplate(false);
                template.setActive(true);
                dao.create(template);
                JsonUtil.writeSuccess(response, "Email template created");
            } else if ("update".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                EmailTemplate template = dao.findById(id);
                if (template == null) {
                    JsonUtil.writeError(response, "Email template not found");
                    return;
                }
                if (body.containsKey("templateName")) { InputValidator.rejectHtml("Template Name", (String) body.get("templateName")); template.setTemplateName((String) body.get("templateName")); }
                if (body.containsKey("subjectTemplate")) { InputValidator.rejectHtml("Subject Template", (String) body.get("subjectTemplate")); template.setSubjectTemplate((String) body.get("subjectTemplate")); }
                if (body.containsKey("bodyTemplate")) template.setBodyTemplate((String) body.get("bodyTemplate"));
                if (body.containsKey("isActive")) template.setActive(JsonUtil.asBoolean(body.get("isActive"), false));
                dao.update(template);
                JsonUtil.writeSuccess(response, "Email template updated");
            } else if ("delete".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                dao.delete(id);
                JsonUtil.writeSuccess(response, "Email template deleted");
            } else if ("setDefault".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                dao.setDefault(id);
                JsonUtil.writeSuccess(response, "Default template updated");
            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeError(response, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Email template operation", e);
        }
    }
}
