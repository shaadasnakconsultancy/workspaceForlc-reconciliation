package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.DocumentTypeDao;
import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.dao.LcDocumentDao;
import com.smipl.lcrecon.job.JobManager;
import com.smipl.lcrecon.model.DocumentType;
import com.smipl.lcrecon.model.LcDocument;
import com.smipl.lcrecon.model.ReconciliationJob;
import com.smipl.lcrecon.model.SupportingDocument;
import com.smipl.lcrecon.util.FileUtil;
import com.smipl.lcrecon.util.JsonUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@WebServlet("/lc-upload")
public class LcUploadServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(LcUploadServlet.class);

    // WEB_VUL_08: server-side allowlist of permitted upload file types (client 'accept' is bypassable)
    private static final Set<String> ALLOWED_UPLOAD_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "docx", "doc", "xlsx", "xlsm", "xls", "msg", "pptx", "ppt"));

    /**
     * Reject the request if any uploaded file has a disallowed extension. Returns true (and writes
     * an error response) when a violation is found, so the caller should stop processing.
     */
    private boolean hasDisallowedFile(HttpServletResponse response, List<FileItem> fileItems) throws IOException {
        for (FileItem item : fileItems) {
            if (item.isFormField() || item.getSize() <= 0) continue;
            String name = item.getName();
            if (name == null) continue;
            name = name.replace('\\', '/');
            if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
            String ext = FileUtil.getFileExtension(name);
            if (!ALLOWED_UPLOAD_EXTENSIONS.contains(ext)) {
                logger.warn("Rejected upload with disallowed file type: {}", name);
                JsonUtil.writeError(response, "File type not allowed: " + name
                        + ". Permitted types: PDF, Word, Excel, PowerPoint, MSG.");
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        DocumentTypeDao docTypeDao = (DocumentTypeDao) getServletContext().getAttribute("documentTypeDao");
        List<DocumentType> documentTypes = docTypeDao.findActive();
        // Filter out MASTER_LC and LC_ADDENDUM from supporting doc types
        List<DocumentType> supportingTypes = new ArrayList<>();
        for (DocumentType dt : documentTypes) {
            if (!"MASTER_LC".equals(dt.getTypeCode()) && !"LC_ADDENDUM".equals(dt.getTypeCode())) {
                supportingTypes.add(dt);
            }
        }
        request.setAttribute("documentTypes", supportingTypes);
        request.getRequestDispatcher("/WEB-INF/jsp/lc-upload.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!ServletFileUpload.isMultipartContent(request)) {
            JsonUtil.writeError(response, "Request must be multipart");
            return;
        }

        try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(1024 * 1024); // 1MB threshold
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setFileSizeMax(50 * 1024 * 1024); // 50MB per file

            List<FileItem> items = upload.parseRequest(request);
            String action = null;
            String batchId = null;
            String notificationEmail = null;
            Map<String, String> formFields = new HashMap<>();
            List<FileItem> fileItems = new ArrayList<>();

            for (FileItem item : items) {
                if (item.isFormField()) {
                    formFields.put(item.getFieldName(), item.getString("UTF-8"));
                } else {
                    fileItems.add(item);
                }
            }

            action = formFields.get("action");
            batchId = formFields.get("batchId");
            notificationEmail = formFields.get("notificationEmail");

            if ("uploadLC".equals(action)) {
                handleUploadLC(request, response, fileItems, formFields);
            } else if ("uploadSupporting".equals(action)) {
                handleUploadSupporting(request, response, fileItems, formFields, batchId);
            } else if ("startJob".equals(action)) {
                handleStartJob(request, response, formFields);
            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (Exception e) {
            logger.error("Upload failed", e);
            JsonUtil.writeError(response, "Upload failed. Please try again.");
        }
    }

    private void handleUploadLC(HttpServletRequest request, HttpServletResponse response,
                                List<FileItem> fileItems, Map<String, String> formFields) throws IOException {
        if (hasDisallowedFile(response, fileItems)) return;

        LcDocumentDao lcDocDao = (LcDocumentDao) getServletContext().getAttribute("lcDocumentDao");
        String batchId = UUID.randomUUID().toString();
        String subDir = batchId + "/lc";

        for (FileItem item : fileItems) {
            if (item.getSize() <= 0) continue;
            String fileName = item.getName();
            if (fileName.contains("\\")) fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

            try {
                String filePath = FileUtil.saveFile(item.getInputStream(), subDir, fileName);
                boolean isAddendum = "addendum".equals(item.getFieldName());

                LcDocument doc = new LcDocument();
                doc.setFileName(fileName);
                doc.setFilePath(filePath);
                doc.setFileSize(item.getSize());
                doc.setAddendum(isAddendum);
                doc.setUploadBatchId(batchId);
                doc.setOcrStatus("PENDING");
                lcDocDao.create(doc);
            } catch (Exception e) {
                logger.error("Failed to save LC file: {}", fileName, e);
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("batchId", batchId);
        JsonUtil.writeSuccess(response, "LC documents uploaded", data);
    }

    private void handleUploadSupporting(HttpServletRequest request, HttpServletResponse response,
                                        List<FileItem> fileItems, Map<String, String> formFields,
                                        String batchId) throws IOException {
        if (hasDisallowedFile(response, fileItems)) return;

        LcDocumentDao lcDocDao = (LcDocumentDao) getServletContext().getAttribute("lcDocumentDao");
        DocumentTypeDao docTypeDao = (DocumentTypeDao) getServletContext().getAttribute("documentTypeDao");
        String subDir = batchId + "/supporting";

        for (FileItem item : fileItems) {
            if (item.getSize() <= 0) continue;
            String fileName = item.getName();
            if (fileName.contains("\\")) fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            if (fileName.contains("/")) fileName = fileName.substring(fileName.lastIndexOf("/") + 1);

            // Get document type ID from field name pattern: file_<docTypeId>
            String fieldName = item.getFieldName();
            String docTypeIdStr = formFields.get("docTypeId_" + fieldName.replace("file_", ""));
            if (docTypeIdStr == null) {
                docTypeIdStr = formFields.get("documentTypeId");
            }
            long docTypeId = Long.parseLong(docTypeIdStr);
            DocumentType dt = docTypeDao.findById(docTypeId);

            try {
                String filePath = FileUtil.saveFile(item.getInputStream(), subDir, fileName);

                SupportingDocument doc = new SupportingDocument();
                doc.setUploadBatchId(batchId);
                doc.setDocumentTypeId(docTypeId);
                doc.setFileName(fileName);
                doc.setFilePath(filePath);
                doc.setFileSize(item.getSize());
                doc.setPageLimit(dt != null ? dt.getPageLimit() : 0);
                doc.setOcrStatus("PENDING");
                lcDocDao.createSupportingDoc(doc);
            } catch (Exception e) {
                logger.error("Failed to save supporting file: {}", fileName, e);
            }
        }

        JsonUtil.writeSuccess(response, "Supporting documents uploaded");
    }

    private void handleStartJob(HttpServletRequest request, HttpServletResponse response,
                                Map<String, String> formFields) throws IOException {
        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");
        JobManager jobManager = (JobManager) getServletContext().getAttribute("jobManager");

        String batchId = formFields.get("batchId");
        String notificationEmail = formFields.get("notificationEmail");
        String username = (String) request.getSession().getAttribute("username");

        String department = (String) request.getSession().getAttribute("userDepartment");

        String jobName = formFields.get("jobName");
        String shipmentDocName = formFields.get("shipmentDocName");
        String emailGroupIdStr = formFields.get("emailGroupId");
        long emailGroupId = 0;
        try { if (emailGroupIdStr != null) emailGroupId = Long.parseLong(emailGroupIdStr); } catch (Exception ignored) {}

        ReconciliationJob job = new ReconciliationJob();
        job.setUploadBatchId(batchId);
        job.setStatus("QUEUED");
        job.setNotificationEmail(notificationEmail);
        job.setCreatedBy(username);
        job.setCreatedByDepartment(department);
        job.setJobName(jobName);
        job.setShipmentDocName(shipmentDocName);
        job.setEmailGroupId(emailGroupId);

        long jobId = jobDao.create(job);
        jobManager.submitJob(jobId, getServletContext());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("jobId", jobId);
        JsonUtil.writeSuccess(response, "Reconciliation job started", data);
    }
}
