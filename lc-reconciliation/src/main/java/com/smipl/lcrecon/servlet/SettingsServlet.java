package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.SettingsDao;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

@WebServlet("/settings")
public class SettingsServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SettingsServlet.class);
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");

        request.setAttribute("openaiSettings", settingsDao.getSettingsByGroup("OPENAI"));
        request.setAttribute("docIntelSettings", settingsDao.getSettingsByGroup("DOC_INTELLIGENCE"));
        request.setAttribute("smtpSettings", settingsDao.getSettingsByGroup("SMTP"));
        request.setAttribute("graphSettings", settingsDao.getSettingsByGroup("GRAPH_API"));
        request.setAttribute("sapSettings", settingsDao.getSettingsByGroup("SAP_API"));
        request.setAttribute("costSettings", settingsDao.getSettingsByGroup("COST_RATES"));
        request.setAttribute("jobSettings", settingsDao.getSettingsByGroup("JOB"));

        request.getRequestDispatcher("/WEB-INF/jsp/settings.jsp").forward(request, response);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SettingsDao settingsDao = (SettingsDao) getServletContext().getAttribute("settingsDao");

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            String group = (String) body.get("group");
            Map<String, String> settings = (Map<String, String>) body.get("settings");

            if (group == null || settings == null) {
                JsonUtil.writeError(response, "Missing group or settings");
                return;
            }

            settingsDao.saveSettings(settings, group);
            JsonUtil.writeSuccess(response, "Settings saved successfully");
        } catch (Exception e) {
            JsonUtil.writeServerError(response, logger, "Save settings", e);
        }
    }
}
