package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.DocumentTypeDao;
import com.smipl.lcrecon.model.DocumentType;
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

@WebServlet("/master/document-types")
public class DocumentTypeMasterServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DocumentTypeMasterServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DocumentTypeDao dao = (DocumentTypeDao) getServletContext().getAttribute("documentTypeDao");

        String format = request.getParameter("format");
        if ("json".equals(format)) {
            List<DocumentType> types = dao.findActive();
            JsonUtil.writeJsonResponse(response, types);
            return;
        }

        List<DocumentType> documentTypes = dao.findAll();
        request.setAttribute("documentTypes", documentTypes);
        request.getRequestDispatcher("/WEB-INF/jsp/master/document-types.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DocumentTypeDao dao = (DocumentTypeDao) getServletContext().getAttribute("documentTypeDao");

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
                InputValidator.rejectHtml("Type Code", (String) body.get("typeCode"));
                InputValidator.rejectHtml("Type Name", (String) body.get("typeName"));
                DocumentType dt = new DocumentType();
                dt.setTypeCode((String) body.get("typeCode"));
                dt.setTypeName((String) body.get("typeName"));
                dt.setPageLimit(JsonUtil.asInt(body.get("pageLimit"), 0));
                dt.setActive(true);
                dt.setDisplayOrder(JsonUtil.asInt(body.get("displayOrder"), 0));
                dao.create(dt);
                JsonUtil.writeSuccess(response, "Document type created");
            } else if ("update".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                DocumentType dt = dao.findById(id);
                if (dt == null) {
                    JsonUtil.writeError(response, "Document type not found");
                    return;
                }
                if (body.containsKey("typeCode")) { InputValidator.rejectHtml("Type Code", (String) body.get("typeCode")); dt.setTypeCode((String) body.get("typeCode")); }
                if (body.containsKey("typeName")) { InputValidator.rejectHtml("Type Name", (String) body.get("typeName")); dt.setTypeName((String) body.get("typeName")); }
                if (body.containsKey("pageLimit")) dt.setPageLimit(JsonUtil.asInt(body.get("pageLimit"), 0));
                if (body.containsKey("isActive")) dt.setActive(JsonUtil.asBoolean(body.get("isActive"), false));
                if (body.containsKey("displayOrder")) dt.setDisplayOrder(JsonUtil.asInt(body.get("displayOrder"), 0));
                dao.update(dt);
                JsonUtil.writeSuccess(response, "Document type updated");
            } else if ("delete".equals(action)) {
                long id = JsonUtil.asLong(body.get("id"), "id");
                dao.delete(id);
                JsonUtil.writeSuccess(response, "Document type deleted");
            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeError(response, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Document type operation", e);
        }
    }
}
