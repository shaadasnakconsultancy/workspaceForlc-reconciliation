package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.LcDocumentDao;
import com.smipl.lcrecon.model.LcDocument;
import com.smipl.lcrecon.model.SupportingDocument;
import com.smipl.lcrecon.model.User;
import com.smipl.lcrecon.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/document/view")
public class DocumentViewServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DocumentViewServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User sessionUser = session != null ? (User) session.getAttribute("user") : null;
        if (sessionUser == null) {
            response.sendError(401, "Unauthorized");
            return;
        }

        String type = request.getParameter("type");
        String idStr = request.getParameter("id");
        if (type == null || idStr == null) {
            response.sendError(400, "Missing parameters");
            return;
        }

        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            response.sendError(400, "Invalid document id");
            return;
        }

        LcDocumentDao lcDocumentDao = (LcDocumentDao) getServletContext().getAttribute("lcDocumentDao");

        String filePath, fileName, uploadBatchId;

        if ("lc".equals(type)) {
            LcDocument doc = lcDocumentDao.findById(id);
            if (doc == null) {
                response.sendError(404, "Document not found");
                return;
            }
            filePath = doc.getFilePath();
            fileName = doc.getFileName();
            uploadBatchId = doc.getUploadBatchId();
        } else if ("supporting".equals(type)) {
            SupportingDocument doc = lcDocumentDao.findSupportingById(id);
            if (doc == null) {
                response.sendError(404, "Document not found");
                return;
            }
            filePath = doc.getFilePath();
            fileName = doc.getFileName();
            uploadBatchId = doc.getUploadBatchId();
        } else {
            response.sendError(400, "Invalid document type");
            return;
        }

        // Serve the file
        byte[] data;
        try {
            data = FileUtil.readFile(filePath);
        } catch (IOException e) {
            logger.error("Failed to read document file: {}", filePath, e);
            response.sendError(404, "File not found on server");
            return;
        }

        String ext = FileUtil.getFileExtension(fileName).toLowerCase();
        String contentType = resolveContentType(ext);
        boolean inline = "pdf".equals(ext);

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + fileName + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        logger.debug("Served document: user={}, type={}, id={}, file={}", sessionUser.getUsername(), type, id, fileName);
    }

    private String resolveContentType(String ext) {
        switch (ext) {
            case "pdf":  return "application/pdf";
            case "docx": return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc":  return "application/msword";
            case "xlsx": return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xlsm":
            case "xls":  return "application/vnd.ms-excel";
            case "pptx": return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "ppt":  return "application/vnd.ms-powerpoint";
            case "msg":  return "application/vnd.ms-outlook";
            default:     return "application/octet-stream";
        }
    }
}
