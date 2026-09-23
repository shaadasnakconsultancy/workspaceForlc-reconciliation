package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.model.ReconciliationJob;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");
        HttpSession session = request.getSession();
        Boolean isSuperAdmin = (Boolean) session.getAttribute("isSuperAdmin");
        String department = Boolean.TRUE.equals(isSuperAdmin) ? null : (String) session.getAttribute("userDepartment");

        // Pagination and filter parameters (sanitized - reflected back into the page)
        String fromDate = com.smipl.lcrecon.util.InputValidator.safeDate(request.getParameter("fromDate"));
        String toDate = com.smipl.lcrecon.util.InputValidator.safeDate(request.getParameter("toDate"));
        String searchJobId = com.smipl.lcrecon.util.InputValidator.stripUnsafe(request.getParameter("searchJobId"));
        int page = 1;
        int pageSize = 25;
        try { page = Integer.parseInt(request.getParameter("page")); } catch (Exception ignored) {}
        try { pageSize = Integer.parseInt(request.getParameter("pageSize")); } catch (Exception ignored) {}

        // Status counts for summary cards
        Map<String, Integer> counts;
        if (Boolean.TRUE.equals(isSuperAdmin)) {
            counts = jobDao.getStatusCounts();
        } else {
            counts = jobDao.getStatusCountsByDepartment((String) session.getAttribute("userDepartment"));
        }

        int total = 0;
        for (int v : counts.values()) total += v;
        request.setAttribute("totalJobs", total);
        request.setAttribute("completedJobs", counts.getOrDefault("COMPLETED", 0));
        request.setAttribute("runningJobs", counts.getOrDefault("RUNNING", 0) + counts.getOrDefault("QUEUED", 0));
        request.setAttribute("failedJobs", counts.getOrDefault("FAILED", 0));

        // Filtered and paginated job list
        List<ReconciliationJob> recentJobs = jobDao.findFiltered(fromDate, toDate, searchJobId, null, department, page, pageSize);
        int totalRecords = jobDao.countFiltered(fromDate, toDate, searchJobId, null, department);
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);

        request.setAttribute("jobs", recentJobs);
        request.setAttribute("totalRecords", totalRecords);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("fromDate", fromDate);
        request.setAttribute("toDate", toDate);
        request.setAttribute("searchJobId", searchJobId);

        request.getRequestDispatcher("/WEB-INF/jsp/dashboard.jsp").forward(request, response);
    }
}
