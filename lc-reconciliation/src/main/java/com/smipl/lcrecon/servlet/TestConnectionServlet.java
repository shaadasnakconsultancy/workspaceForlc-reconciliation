package com.smipl.lcrecon.servlet;

import com.smipl.lcrecon.dao.SettingsDao;
import com.smipl.lcrecon.integration.AzureDocIntelligenceClient;
import com.smipl.lcrecon.integration.AzureOpenAIClient;
import com.smipl.lcrecon.integration.GraphApiClient;
import com.smipl.lcrecon.integration.SapApiClient;
import com.smipl.lcrecon.service.EmailService;
import com.smipl.lcrecon.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backs the "Test Connection" buttons on the Settings page. The values typed into the form are
 * posted here, so a configuration can be verified before it is saved. Any field left blank falls
 * back to the currently stored value - that keeps masked password fields working.
 *
 * Access is restricted to IT_ADMIN by AuthFilter, which guards everything under /settings.
 */
@WebServlet("/settings/test-connection")
public class TestConnectionServlet extends HttpServlet {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TestConnectionServlet.class);

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

        String type = null;
        try {
            Map<String, Object> body = JsonUtil.fromJson(sb.toString(), Map.class);
            if (body == null) {
                JsonUtil.writeError(response, "Empty request.");
                return;
            }
            type = (String) body.get("type");
            Map<String, String> posted = toStringMap((Map<String, Object>) body.get("settings"));

            if (type == null) {
                JsonUtil.writeError(response, "No connection type supplied.");
                return;
            }

            switch (type) {
                case "smtp":
                    testSmtp(response, merge(settingsDao, "SMTP", posted));
                    break;
                case "openai":
                    testOpenAi(response, merge(settingsDao, "OPENAI", posted));
                    break;
                case "docintel":
                    testDocIntel(response, merge(settingsDao, "DOC_INTELLIGENCE", posted));
                    break;
                case "graph":
                    testGraph(response, merge(settingsDao, "GRAPH_API", posted));
                    break;
                case "sap":
                    testSap(response, merge(settingsDao, "SAP_API", posted));
                    break;
                default:
                    JsonUtil.writeError(response, "Unknown connection type: " + type);
            }
        } catch (Exception e) {
            logger.error("Connection test failed for type: {}", type, e);
            JsonUtil.writeError(response, "Test could not be run: " + rootMessage(e));
        }
    }

    private void testSmtp(HttpServletResponse response, Map<String, String> settings) throws IOException {
        EmailService.EmailResult result = new EmailService().testConnection(settings);
        if (result.isSent()) {
            JsonUtil.writeSuccess(response, "Connected to " + settings.getOrDefault("smtp_host", "")
                    + ":" + settings.getOrDefault("smtp_port", "587") + " and authenticated successfully.");
        } else {
            JsonUtil.writeError(response, result.message);
        }
    }

    private void testOpenAi(HttpServletResponse response, Map<String, String> settings) throws IOException {
        String url = settings.getOrDefault("openai_endpoint", "");
        String key = settings.getOrDefault("openai_api_key", "");
        String model = settings.getOrDefault("openai_model", "gpt-4o-mini");
        if (url.trim().isEmpty() || key.trim().isEmpty()) {
            JsonUtil.writeError(response, "Endpoint and API key are both required.");
            return;
        }
        String error = new AzureOpenAIClient(url, key, model).testConnection();
        if (error == null) {
            JsonUtil.writeSuccess(response, "OpenAI responded successfully using model '" + model + "'.");
        } else {
            JsonUtil.writeError(response, "OpenAI test failed: " + error);
        }
    }

    private void testDocIntel(HttpServletResponse response, Map<String, String> settings) throws IOException {
        String endpoint = settings.getOrDefault("doc_intel_endpoint", "");
        String key = settings.getOrDefault("doc_intel_api_key", "");
        if (endpoint.trim().isEmpty() || key.trim().isEmpty()) {
            JsonUtil.writeError(response, "Endpoint and API key are both required.");
            return;
        }
        String error = new AzureDocIntelligenceClient(endpoint, key).testConnection();
        if (error == null) {
            JsonUtil.writeSuccess(response, "Document Intelligence endpoint reachable and the key was accepted.");
        } else {
            JsonUtil.writeError(response, "Document Intelligence test failed: " + error);
        }
    }

    private void testGraph(HttpServletResponse response, Map<String, String> settings) throws IOException {
        GraphApiClient client = new GraphApiClient(settings);
        if (!client.isConfigured()) {
            JsonUtil.writeError(response, "Tenant ID, Client ID and Client Secret are all required.");
            return;
        }
        if (client.testConnection()) {
            JsonUtil.writeSuccess(response, "Access token obtained from login.microsoftonline.com successfully.");
        } else {
            JsonUtil.writeError(response, "Could not obtain an access token. Check the tenant/client/secret values and that "
                    + "login.microsoftonline.com is reachable from this server (the server log has the full error).");
        }
    }

    private void testSap(HttpServletResponse response, Map<String, String> settings) throws IOException {
        SapApiClient client = new SapApiClient(settings);
        if (!client.isConfigured()) {
            JsonUtil.writeError(response, "Base URL, username and password are all required.");
            return;
        }
        if (client.testConnection()) {
            JsonUtil.writeSuccess(response, "SAP OData service responded successfully.");
        } else {
            JsonUtil.writeError(response, "SAP did not respond as expected. Check the base URL, client, credentials and "
                    + "network access to the SAP host (the server log has the full error).");
        }
    }

    /**
     * Overlay the posted values on top of what is stored, ignoring blanks. A blank password field
     * therefore keeps testing against the saved password instead of failing on an empty credential.
     */
    private Map<String, String> merge(SettingsDao settingsDao, String group, Map<String, String> posted) {
        Map<String, String> merged = new LinkedHashMap<>(settingsDao.getSettingsByGroup(group));
        if (posted != null) {
            for (Map.Entry<String, String> entry : posted.entrySet()) {
                String value = entry.getValue();
                if (value != null && !value.trim().isEmpty()) {
                    merged.put(entry.getKey(), value);
                }
            }
        }
        return merged;
    }

    private Map<String, String> toStringMap(Map<String, Object> raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw != null) {
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                out.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
            }
        }
        return out;
    }

    private String rootMessage(Exception e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();
    }
}
