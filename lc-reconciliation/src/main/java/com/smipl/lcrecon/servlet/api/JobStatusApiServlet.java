package com.smipl.lcrecon.servlet.api;

import com.smipl.lcrecon.dao.JobDao;
import com.smipl.lcrecon.model.ReconciliationJob;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/job-status")
public class JobStatusApiServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String jobIdStr = request.getParameter("jobId");
        if (jobIdStr == null) {
            JsonUtil.writeError(response, "jobId is required");
            return;
        }

        JobDao jobDao = (JobDao) getServletContext().getAttribute("jobDao");
        ReconciliationJob job = jobDao.findById(Long.parseLong(jobIdStr));

        if (job == null) {
            JsonUtil.writeError(response, "Job not found");
            return;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", job.getId());
        data.put("lcNumber", job.getLcNumber());
        data.put("status", job.getStatus());
        data.put("progressPercent", job.getProgressPercent());
        data.put("currentStep", job.getCurrentStep());
        data.put("completedSteps", job.getCompletedSteps());
        data.put("totalSteps", job.getTotalSteps());
        JsonUtil.writeJsonResponse(response, data);
    }
}
