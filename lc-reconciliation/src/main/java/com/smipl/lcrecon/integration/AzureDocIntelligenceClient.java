package com.smipl.lcrecon.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureDocIntelligenceClient {
    private static final Logger logger = LoggerFactory.getLogger(AzureDocIntelligenceClient.class);
    private static final String API_VERSION = "2024-11-30";

    private final String endpoint;
    private final String apiKey;

    public AzureDocIntelligenceClient(String endpoint, String apiKey) {
        this.endpoint = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        // Strip "Bearer " prefix if user included it
        this.apiKey = apiKey != null && apiKey.startsWith("Bearer ") ? apiKey.substring(7) : apiKey;
    }

    /**
     * Response wrapper that includes extracted text and page count for cost tracking.
     */
    public static class OcrResponse {
        public final String text;
        public final int pagesProcessed;

        public OcrResponse(String text, int pagesProcessed) {
            this.text = text;
            this.pagesProcessed = pagesProcessed;
        }
    }

    public OcrResponse analyzeDocument(byte[] pdfBytes, int pageLimit) throws Exception {
        String url;
        // If user saved the full URL with analyze path, use it directly
        if (endpoint.contains("/documentModels/")) {
            url = endpoint;
            // Add pages parameter if needed
            if (pageLimit > 0) {
                url += (url.contains("?") ? "&" : "?") + "pages=1-" + pageLimit;
            }
        } else {
            url = endpoint + "/documentintelligence/documentModels/prebuilt-read:analyze?api-version=" + API_VERSION;
            if (pageLimit > 0) {
                url += "&pages=1-" + pageLimit;
            }
        }

        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createLongRunning()) {
            // Submit analysis
            HttpPost post = new HttpPost(url);
            post.setHeader("Ocp-Apim-Subscription-Key", apiKey);
            post.setEntity(new ByteArrayEntity(pdfBytes, ContentType.APPLICATION_OCTET_STREAM));

            String operationLocation;
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 202) {
                    String body = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException("Document Intelligence API returned " + statusCode + ": " + body);
                }
                operationLocation = response.getFirstHeader("Operation-Location").getValue();
            }

            logger.info("Document analysis submitted, polling: {}", operationLocation);

            // Poll for result
            String resultJson = null;
            for (int attempt = 0; attempt < 60; attempt++) {
                Thread.sleep(2000);

                HttpGet get = new HttpGet(operationLocation);
                get.setHeader("Ocp-Apim-Subscription-Key", apiKey);

                try (CloseableHttpResponse pollResponse = httpClient.execute(get)) {
                    resultJson = EntityUtils.toString(pollResponse.getEntity());
                    JsonObject result = JsonParser.parseString(resultJson).getAsJsonObject();
                    String status = result.get("status").getAsString();

                    if ("succeeded".equals(status)) {
                        String content = extractContent(result);
                        int pageCount = extractPageCount(result);
                        return new OcrResponse(content, pageCount);
                    } else if ("failed".equals(status)) {
                        throw new RuntimeException("Document analysis failed: " + resultJson);
                    }
                    // "running" or "notStarted" - continue polling
                }
            }
            throw new RuntimeException("Document analysis timed out after 120 seconds");
        }
    }

    private String extractContent(JsonObject result) {
        JsonObject analyzeResult = result.getAsJsonObject("analyzeResult");
        if (analyzeResult == null) return "";

        // Get full content text
        if (analyzeResult.has("content")) {
            return analyzeResult.get("content").getAsString();
        }

        // Fallback: concatenate page content
        StringBuilder sb = new StringBuilder();
        JsonArray pages = analyzeResult.getAsJsonArray("pages");
        if (pages != null) {
            for (JsonElement page : pages) {
                JsonArray lines = page.getAsJsonObject().getAsJsonArray("lines");
                if (lines != null) {
                    for (JsonElement line : lines) {
                        sb.append(line.getAsJsonObject().get("content").getAsString()).append("\n");
                    }
                }
                sb.append("\n--- Page Break ---\n");
            }
        }
        return sb.toString();
    }

    private int extractPageCount(JsonObject result) {
        try {
            JsonObject analyzeResult = result.getAsJsonObject("analyzeResult");
            if (analyzeResult != null && analyzeResult.has("pages")) {
                return analyzeResult.getAsJsonArray("pages").size();
            }
        } catch (Exception e) {
            logger.warn("Could not extract page count: {}", e.getMessage());
        }
        return 1; // default to 1 page
    }

    /**
     * The service root, derived from whatever form the endpoint was saved in.
     *
     * The endpoint may be stored either as the bare resource URL or as the full analyze URL
     * (analyzeDocument accepts both). Appending a path to the full analyze form produces a
     * nonsense URL, so anything from the API path onwards is trimmed off here.
     */
    private String serviceRoot() {
        String root = endpoint;
        for (String marker : new String[]{"/documentintelligence/", "/formrecognizer/"}) {
            int idx = root.indexOf(marker);
            if (idx > 0) {
                root = root.substring(0, idx);
                break;
            }
        }
        int query = root.indexOf('?');
        if (query > 0) root = root.substring(0, query);
        return root.endsWith("/") ? root.substring(0, root.length() - 1) : root;
    }

    /**
     * Verify the endpoint and key by listing the available models.
     *
     * @return null when the connection is good, otherwise a description of what went wrong.
     */
    public String testConnection() {
        String url = serviceRoot() + "/documentintelligence/documentModels?api-version=" + API_VERSION;
        try (CloseableHttpClient httpClient = com.smipl.lcrecon.util.HttpClientFactory.createDefault()) {
            HttpGet get = new HttpGet(url);
            get.setHeader("Ocp-Apim-Subscription-Key", apiKey);
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                int status = response.getStatusLine().getStatusCode();
                if (status == 200) return null;
                String body = response.getEntity() != null ? EntityUtils.toString(response.getEntity()) : "";
                logger.error("Document Intelligence test failed: HTTP {} from {} - {}", status, url, body);
                if (status == 401 || status == 403) {
                    return "Endpoint reachable but the API key was rejected (HTTP " + status + ").";
                }
                if (status == 404) {
                    return "Endpoint not found (HTTP 404) at " + url + " - check the Document Intelligence endpoint URL.";
                }
                return "Document Intelligence returned HTTP " + status + ": " + truncate(body);
            }
        } catch (Exception e) {
            logger.error("Document Intelligence connection test failed for {}", url, e);
            return "Could not reach " + url + ": " + e.getMessage();
        }
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
