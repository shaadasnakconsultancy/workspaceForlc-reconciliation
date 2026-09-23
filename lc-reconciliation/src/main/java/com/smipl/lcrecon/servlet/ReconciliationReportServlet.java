package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.model.ReconciliationJob;
import com.smipl.lcrecon.model.ReconciliationResult;
import com.smipl.lcrecon.util.FileUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

@WebServlet("/report")
public class ReconciliationReportServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String jobIdStr = request.getParameter("jobId");
        if (jobIdStr == null) {
            JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");
            HttpSession session = request.getSession();

            String fromDate = request.getParameter("fromDate");
            String toDate = request.getParameter("toDate");
            String searchJobId = request.getParameter("searchJobId");
            int page = 1;
            int pageSize = 25;
            try { page = Integer.parseInt(request.getParameter("page")); } catch (Exception ignored) {}
            try { pageSize = Integer.parseInt(request.getParameter("pageSize")); } catch (Exception ignored) {}

            Boolean isSuperAdmin = (Boolean) session.getAttribute("isSuperAdmin");
            String department = Boolean.TRUE.equals(isSuperAdmin) ? null : (String) session.getAttribute("userDepartment");

            List<ReconciliationJob> jobs = jobDao.findFiltered(fromDate, toDate, searchJobId, "COMPLETED", department, page, pageSize);
            int totalRecords = jobDao.countFiltered(fromDate, toDate, searchJobId, "COMPLETED", department);
            int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

            request.setAttribute("jobs", jobs);
            request.setAttribute("totalRecords", totalRecords);
            request.setAttribute("totalPages", totalPages);
            request.setAttribute("currentPage", page);
            request.setAttribute("pageSize", pageSize);
            request.setAttribute("fromDate", fromDate);
            request.setAttribute("toDate", toDate);
            request.setAttribute("searchJobId", searchJobId);
            request.setAttribute("reportType", "full");
            request.getRequestDispatcher("/WEB-INF/jsp/report-list.jsp").forward(request, response);
            return;
        }

        long jobId = Long.parseLong(jobIdStr);
        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");

        ReconciliationJob job = jobDao.findById(jobId);
        if (job == null) { response.sendError(404, "Job not found"); return; }

        String format = request.getParameter("format");
        if ("html".equals(format) && job.getReportPath() != null) {
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=Report_" + job.getLcNumber() + ".html");
            response.getOutputStream().write(FileUtil.readFile(job.getReportPath()));
            return;
        }
        if ("xlsx".equals(format) && job.getXlsxReportPath() != null) {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=Report_" + job.getLcNumber() + ".xlsx");
            response.getOutputStream().write(FileUtil.readFile(job.getXlsxReportPath()));
            return;
        }

        List<ReconciliationResult> results = jobDao.getResults(jobId);

        // Get unique document type names in order
        LinkedHashSet<String> docTypeSet = new LinkedHashSet<>();
        for (ReconciliationResult r : results) {
            if (r.getDocumentTypeName() != null) docTypeSet.add(r.getDocumentTypeName());
        }

        request.setAttribute("job", job);
        request.setAttribute("results", results);
        request.setAttribute("uploadedDocTypes", new ArrayList<>(docTypeSet));
        request.setAttribute("reportType", "full");
        request.getRequestDispatcher("/WEB-INF/jsp/reconciliation-report.jsp").forward(request, response);
    }
}
