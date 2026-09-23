package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.dao.LcDocumentDao;
import com.smipl.lcrecon.job.JobManager;
import com.smipl.lcrecon.model.LcDocument;
import com.smipl.lcrecon.model.ReconciliationJob;
import com.smipl.lcrecon.model.SupportingDocument;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/job-monitor")
public class JobMonitorServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JobMonitorServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");
        LcDocumentDao lcDocumentDao = (LcDocumentDao) getServletContext().getAttribute("lcDocumentDao");
        HttpSession session = request.getSession();
        Boolean isSuperAdmin = (Boolean) session.getAttribute("isSuperAdmin");
        String department = (String) session.getAttribute("userDepartment");

        String jobIdStr = request.getParameter("jobId");
        if (jobIdStr != null) {
            long jobId = Long.parseLong(jobIdStr);
            ReconciliationJob job = jobDao.findById(jobId);
            List<Map<String, String>> logs = jobDao.getLogs(jobId);
            request.setAttribute("job", job);
            request.setAttribute("jobLogs", logs);

            if (job != null && job.getUploadBatchId() != null) {
                List<LcDocument> lcDocs = lcDocumentDao.findByBatchId(job.getUploadBatchId());
                List<SupportingDocument> supportingDocs = lcDocumentDao.findSupportingByBatchId(job.getUploadBatchId());
                request.setAttribute("lcDocs", lcDocs);
                request.setAttribute("supportingDocs", supportingDocs);
            }
        }

        // Pagination and filter parameters (sanitized - these are reflected back into the page)
        String fromDate = com.smipl.lcrecon.util.InputValidator.safeDate(request.getParameter("fromDate"));
        String toDate = com.smipl.lcrecon.util.InputValidator.safeDate(request.getParameter("toDate"));
        String searchJobId = com.smipl.lcrecon.util.InputValidator.stripUnsafe(request.getParameter("searchJobId"));
        int page = 1;
        int pageSize = 25;
        try { page = Integer.parseInt(request.getParameter("page")); } catch (Exception ignored) {}
        try { pageSize = Integer.parseInt(request.getParameter("pageSize")); } catch (Exception ignored) {}

        String deptFilter = Boolean.TRUE.equals(isSuperAdmin) ? null : department;

        List<ReconciliationJob> jobs = jobDao.findFiltered(fromDate, toDate, searchJobId, null, deptFilter, page, pageSize);
        int totalRecords = jobDao.countFiltered(fromDate, toDate, searchJobId, null, deptFilter);
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        request.setAttribute("jobs", jobs);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("fromDate", fromDate);
        request.setAttribute("toDate", toDate);
        request.setAttribute("searchJobId", searchJobId);
        request.getRequestDispatcher("/WEB-INF/jsp/job-monitor.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);

        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            String action = (String) body.get("action");

            if ("delete".equals(action)) {
                long id = JsonUtil.asLong(body.get("jobId"), "jobId");
                ReconciliationJob job = jobDao.findById(id);
                if (job == null) {
                    JsonUtil.writeError(response, "Job not found");
                    return;
                }
                if ("RUNNING".equals(job.getStatus())) {
                    JsonUtil.writeError(response, "Cannot delete a running job. Wait for it to complete or fail.");
                    return;
                }
                jobDao.deleteJob(id);
                JsonUtil.writeSuccess(response, "Job #" + id + " deleted successfully");
            } else if ("rerun".equals(action)) {
                long oldJobId = JsonUtil.asLong(body.get("jobId"), "jobId");
                ReconciliationJob oldJob = jobDao.findById(oldJobId);
                if (oldJob == null) {
                    JsonUtil.writeError(response, "Job not found");
                    return;
                }
                // Create a NEW job copying details from the old one
                ReconciliationJob newJob = new ReconciliationJob();
                newJob.setUploadBatchId(oldJob.getUploadBatchId());
                newJob.setStatus("QUEUED");
                newJob.setJobName(oldJob.getJobName() != null ? oldJob.getJobName() + " (Rerun)" : "Rerun of Job #" + oldJobId);
                newJob.setShipmentDocName(oldJob.getShipmentDocName());
                newJob.setNotificationEmail(oldJob.getNotificationEmail());
                newJob.setEmailGroupId(oldJob.getEmailGroupId());
                newJob.setCreatedBy((String) request.getSession().getAttribute("username"));
                newJob.setCreatedByDepartment((String) request.getSession().getAttribute("userDepartment"));
                long newJobId = jobDao.create(newJob);
                JobManager jobManager = (JobManager) getServletContext().getAttribute("jobManager");
                jobManager.submitJob(newJobId, getServletContext());
                Map<String, Object> data = new java.util.LinkedHashMap<>();
                data.put("newJobId", newJobId);
                JsonUtil.writeSuccess(response, "New Job #" + newJobId + " created from Job #" + oldJobId, data);
            } else if ("abort".equals(action)) {
                long jobId = JsonUtil.asLong(body.get("jobId"), "jobId");
                ReconciliationJob job = jobDao.findById(jobId);
                if (job == null) {
                    JsonUtil.writeError(response, "Job not found");
                    return;
                }
                if (!"RUNNING".equals(job.getStatus()) && !"QUEUED".equals(job.getStatus())) {
                    JsonUtil.writeError(response, "Only running or queued jobs can be aborted");
                    return;
                }
                JobManager jobManager = (JobManager) getServletContext().getAttribute("jobManager");
                jobManager.abortJob(jobId);
                jobDao.updateCompleted(jobId, "ABORTED", null, null, null, null, "Aborted by user");
                jobDao.addLog(jobId, "WARN", "Job aborted by user: " + request.getSession().getAttribute("username"));
                JsonUtil.writeSuccess(response, "Job #" + jobId + " aborted successfully");
            } else if ("updateDetails".equals(action)) {
                long id = JsonUtil.asLong(body.get("jobId"), "jobId");
                String jobName = (String) body.get("jobName");
                String shipmentDocName = (String) body.get("shipmentDocName");
                jobDao.updateJobDetails(id, jobName, shipmentDocName);
                JsonUtil.writeSuccess(response, "Job #" + id + " details updated successfully");
            } else {
                JsonUtil.writeError(response, "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            JsonUtil.writeError(response, e.getMessage());
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Job monitor operation", e);
        }
    }
}
