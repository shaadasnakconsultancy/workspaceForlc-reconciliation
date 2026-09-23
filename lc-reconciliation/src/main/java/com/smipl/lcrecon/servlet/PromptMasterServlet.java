package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.DocumentTypeDao;
import com.smipl.lcrecon.dao.PromptTemplateDao;
import com.smipl.lcrecon.model.DocumentType;
import com.smipl.lcrecon.model.PromptTemplate;
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

@WebServlet("/master/prompts")
public class PromptMasterServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PromptMasterServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DocumentTypeDao docTypeDao = (DocumentTypeDao) getServletContext().getAttribute("documentTypeDao");
        PromptTemplateDao promptDao = (PromptTemplateDao) getServletContext().getAttribute("promptTemplateDao");

        String format = request.getParameter("format");

        // Return single prompt by ID (for edit modal)
        if ("byId".equals(format)) {
            String promptIdStr = request.getParameter("promptId");
            if (promptIdStr != null) {
                long promptId = Long.parseLong(promptIdStr);
                // Find across all prompts
                List<PromptTemplate> all = promptDao.findAll();
                for (PromptTemplate pt : all) {
                    if (pt.getId() == promptId) {
                        JsonUtil.writeJsonResponse(response, pt);
                        return;
                    }
                }
            }
            response.setStatus(404);
            JsonUtil.writeError(response, "Prompt not found");
            return;
        }

        // Return all prompts as JSON
        if ("allJson".equals(format)) {
            List<PromptTemplate> prompts = promptDao.findAll();
            JsonUtil.writeJsonResponse(response, prompts);
            return;
        }

        // Return prompts for a specific doc type
        if ("json".equals(format)) {
            String docTypeIdStr = request.getParameter("documentTypeId");
            if (docTypeIdStr != null) {
                long docTypeId = Long.parseLong(docTypeIdStr);
                List<PromptTemplate> prompts = promptDao.findByDocumentTypeId(docTypeId);
                JsonUtil.writeJsonResponse(response, prompts);
                return;
            }
        }

        // Page view - list all prompts
        List<DocumentType> documentTypes = docTypeDao.findAll();
        List<PromptTemplate> prompts = promptDao.findAll();
        request.setAttribute("documentTypes", documentTypes);
        request.setAttribute("prompts", prompts);
        request.getRequestDispatcher("/WEB-INF/jsp/master/prompt-templates.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PromptTemplateDao promptDao = (PromptTemplateDao) getServletContext().getAttribute("promptTemplateDao");

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            String action = (String) body.get("action");

            if ("create".equals(action)) {
                long docTypeId = JsonUtil.asLong(body.get("documentTypeId"), "documentTypeId");
                PromptTemplate pt = new PromptTemplate();
                pt.setDocumentTypeId(docTypeId);
                pt.setPromptName((String) body.get("promptName"));
                pt.setPromptText((String) body.get("promptText"));
                pt.setResponseSchema((String) body.get("responseSchema"));
                pt.setActive(true);
                pt.setVersion(JsonUtil.asInt(body.get("version"), 1));
                promptDao.create(pt);
                JsonUtil.writeSuccess(response, "Prompt template created");

            } else if ("createVersion".equals(action)) {
                long docTypeId = JsonUtil.asLong(body.get("documentTypeId"), "documentTypeId");
                promptDao.deactivateAllForDocType(docTypeId);
                PromptTemplate pt = new PromptTemplate();
                pt.setDocumentTypeId(docTypeId);
                pt.setPromptName((String) body.get("promptName"));
                pt.setPromptText((String) body.get("promptText"));
                pt.setResponseSchema((String) body.get("responseSchema"));
                pt.setActive(true);
                pt.setVersion(JsonUtil.asInt(body.get("version"), 1));
                promptDao.create(pt);
                JsonUtil.writeSuccess(response, "New version saved and activated");

            } else if ("update".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                PromptTemplate pt = new PromptTemplate();
                pt.setId(id);
                pt.setPromptName((String) body.get("promptName"));
                pt.setPromptText((String) body.get("promptText"));
                pt.setResponseSchema((String) body.get("responseSchema"));
                promptDao.update(pt);
                JsonUtil.writeSuccess(response, "Prompt template updated");

            } else if ("activate".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                long docTypeId = JsonUtil.asLong(body.get("documentTypeId"), "documentTypeId");
                // Deactivate all for this doc type, then activate this one
                promptDao.deactivateAllForDocType(docTypeId);
                promptDao.activate(id);
                JsonUtil.writeSuccess(response, "Prompt activated");

            } else if ("deactivate".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                promptDao.deactivateById(id);
                JsonUtil.writeSuccess(response, "Prompt deactivated");

            } else if ("delete".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                promptDao.delete(id);
                JsonUtil.writeSuccess(response, "Prompt deleted");

            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeError(response, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Prompt operation", e);
        }
    }
}
