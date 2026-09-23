package com.smipl.lcrecon.integration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Microsoft Graph API client for converting documents to PDF via OneDrive.
 * Flow: Upload file -> Download as PDF -> Delete temp file
 */
public class GraphApiClient {
    private static final Logger logger = LoggerFactory.getLogger(GraphApiClient.class);
    private static final String TOKEN_URL = "https://login.microsoftonline.com/%s/oauth2/v2.0/token";
    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String driveId;

    public GraphApiClient(Map<String, String> settings) {
        this.tenantId = settings.getOrDefault("graph_tenant_id", "");
        this.clientId = settings.getOrDefault("graph_client_id", "");
        this.clientSecret = settings.getOrDefault("graph_client_secret", "");
        this.driveId = settings.getOrDefault("graph_drive_id", "");
    }

    public boolean isConfigured() {
        return !tenantId.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    /**
     * Convert a file to PDF using OneDrive.
     * 1. Get access token
     * 2. Upload file to OneDrive temp folder
     * 3. Download as PDF
     * 4. Delete temp file
     */
    public byte[] convertToPdf(byte[] fileBytes, String fileName) throws Exception {
        logger.info("Converting {} ({} bytes) to PDF via Graph API", fileName, fileBytes.length);

        String accessToken = getAccessToken();
        String tempPath = "LCRecon_Temp/" + System.currentTimeMillis() + "_" + fileName;

        try {
            // Step 1: Upload file to OneDrive
            uploadFile(accessToken, tempPath, fileBytes);
            logger.info("File uploaded to OneDrive: {}", tempPath);

            // Step 2: Wait briefly for processing
            Thread.sleep(2000);

            // Step 3: Download as PDF
            byte[] pdfBytes = downloadAsPdf(accessToken, tempPath);
            logger.info("PDF downloaded: {} bytes", pdfBytes.length);

            return pdfBytes;
        } finally {
            // Step 4: Delete temp file (best effort)
            try {
                deleteFile(accessToken, tempPath);
                logger.info("Temp file deleted: {}", tempPath);
            } catch (Exception e) {
                logger.warn("Failed to delete temp file: {}", e.getMessage());
            }
        }
    }

    private String getAccessToken() throws Exception {
        String tokenUrl = String.format(TOKEN_URL, tenantId);
        String body = "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&scope=" + URLEncoder.encode("https://graph.microsoft.com/.default", "UTF-8") +
                "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") +
                "&grant_type=client_credentials";

        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createDefault()) {
            HttpPost post = new HttpPost(tokenUrl);
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            post.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(post)) {
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    throw new RuntimeException("Token request failed (" + status + "): " + responseBody);
                }

                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                return json.get("access_token").getAsString();
            }
        }
    }

    private String getDrivePath() {
        if (driveId != null && !driveId.isEmpty()) {
            return GRAPH_BASE + "/drives/" + driveId + "/root:/";
        }
        // Use the application's drive (requires Sites.ReadWrite.All or Files.ReadWrite.All)
        return GRAPH_BASE + "/drive/root:/";
    }

    private String encodePath(String path) {
        // Encode each segment of the path separately, preserving /
        String[] parts = path.split("/");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("/");
            try { sb.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20")); }
            catch (Exception e) { sb.append(parts[i]); }
        }
        return sb.toString();
    }

    private void uploadFile(String accessToken, String remotePath, byte[] fileBytes) throws Exception {
        String url = getDrivePath() + encodePath(remotePath) + ":/content";

        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createDefault()) {
            HttpPut put = new HttpPut(url);
            put.setHeader("Authorization", "Bearer " + accessToken);
            put.setEntity(new ByteArrayEntity(fileBytes, ContentType.APPLICATION_OCTET_STREAM));

            try (CloseableHttpResponse response = httpClient.execute(put)) {
                int status = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                if (status != 200 && status != 201) {
                    throw new RuntimeException("Upload failed (" + status + "): " + responseBody);
                }
            }
        }
    }

    private byte[] downloadAsPdf(String accessToken, String remotePath) throws Exception {
        String url = getDrivePath() + encodePath(remotePath) + ":/content?format=pdf";

        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Authorization", "Bearer " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(get)) {
                int status = response.getStatusLine().getStatusCode();

                if (status == 302) {
                    // Follow redirect for download
                    String redirectUrl = response.getFirstHeader("Location").getValue();
                    return downloadFromUrl(redirectUrl);
                }

                if (status != 200) {
                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    throw new RuntimeException("PDF download failed (" + status + "): " + body);
                }

                // Read binary response
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        }
    }

    private byte[] downloadFromUrl(String url) throws Exception {
        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createDefault()) {
            HttpGet get = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                InputStream is = response.getEntity().getContent();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        }
    }

    private void deleteFile(String accessToken, String remotePath) throws Exception {
        String url = getDrivePath() + encodePath(remotePath);

        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createDefault()) {
            HttpDelete delete = new HttpDelete(url);
            delete.setHeader("Authorization", "Bearer " + accessToken);

            try (CloseableHttpResponse response = httpClient.execute(delete)) {
                // 204 No Content = success
                int status = response.getStatusLine().getStatusCode();
                if (status != 204 && status != 200) {
                    logger.warn("Delete returned status {}", status);
                }
            }
        }
    }

    public boolean testConnection() {
        try {
            String token = getAccessToken();
            return token != null && !token.isEmpty();
        } catch (Exception e) {
            logger.error("Graph API connection test failed", e);
            return false;
        }
    }
}
