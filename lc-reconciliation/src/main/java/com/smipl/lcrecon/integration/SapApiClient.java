package com.smipl.lcrecon.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client for the Suzuki SAP OData service (ZVINCSD_ZINCSDTBDPARTS).
 * Given an invoice number, fetches the list of HSN codes (zincsdhsni) SAP holds for that invoice.
 *
 * Final URL built as:
 *   {baseUrl}?sap-client={client}&$filter=xblnr eq '{invoiceNo}'&$format=json
 * Auth: HTTP Basic (username/password).
 */
public class SapApiClient {
    private static final Logger logger = LoggerFactory.getLogger(SapApiClient.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000; // TCP connect / pool lease
    private static final int SOCKET_TIMEOUT_MS = 30_000;  // wait for response data

    private final String baseUrl;
    private final String client;
    private final String username;
    private final String password;
    private final boolean enabled;

    public SapApiClient(Map<String, String> settings) {
        this.baseUrl = trim(settings.getOrDefault("sap_base_url", ""));
        this.client = trim(settings.getOrDefault("sap_client", ""));
        this.username = settings.getOrDefault("sap_username", "");
        this.password = settings.getOrDefault("sap_password", "");
        this.enabled = "true".equalsIgnoreCase(trim(settings.getOrDefault("sap_enabled", "false")));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isEmpty()
                && username != null && !username.isEmpty()
                && password != null && !password.isEmpty();
    }

    /**
     * Result of a SAP HSN lookup.
     */
    public static class SapHsnResult {
        /** Distinct HSN codes returned by SAP for the invoice (may be empty). */
        public final Set<String> hsnCodes;
        /** True when SAP returned at least one row. */
        public final boolean found;

        public SapHsnResult(Set<String> hsnCodes, boolean found) {
            this.hsnCodes = hsnCodes;
            this.found = found;
        }
    }

    /**
     * Query SAP for all HSN codes tied to the given invoice number.
     *
     * @param invoiceNo the commercial invoice number (goes into xblnr filter)
     * @return the distinct HSN code set; {@code found=false} when SAP returned no rows
     * @throws Exception on HTTP/auth/parse failure
     */
    public SapHsnResult fetchHsnCodes(String invoiceNo) throws Exception {
        if (invoiceNo == null || invoiceNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice number is required for SAP HSN lookup");
        }
        String url = buildUrl(invoiceNo.trim());
        logger.info("Calling SAP HSN API for invoice {}", invoiceNo);
        // Full request URL for troubleshooting (e.g. SAP 401/404s where the exact filter/client
        // sent needs to be compared against what SAP expects). Contains no credentials - Basic
        // Auth is sent as a header below, not in the URL.
        logger.info("SAP HSN API request URL: {}", url);

        // Timeouts so a down/hung SAP endpoint fails fast instead of blocking the worker thread
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig).build()) {
            HttpGet get = new HttpGet(url);
            String auth = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            get.setHeader("Authorization", "Basic " + encoded);
            get.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(get)) {
                int status = response.getStatusLine().getStatusCode();
                String body = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8) : "";
                if (status != 200) {
                    logger.error("SAP API error {}: {}", status, truncate(body));
                    throw new RuntimeException("SAP API returned HTTP " + status);
                }
                return parseHsnCodes(body);
            }
        }
    }

    String buildUrl(String invoiceNo) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append(baseUrl.contains("?") ? "&" : "?");
        if (client != null && !client.isEmpty()) {
            sb.append("sap-client=").append(URLEncoder.encode(client, "UTF-8")).append("&");
        }
        String filter = "xblnr eq '" + invoiceNo + "'";
        sb.append("$filter=").append(URLEncoder.encode(filter, "UTF-8").replace("+", "%20"));
        sb.append("&$format=json");
        return sb.toString();
    }

    private SapHsnResult parseHsnCodes(String body) {
        Set<String> codes = new LinkedHashSet<>();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        if (root.has("d") && root.get("d").isJsonObject()) {
            JsonObject d = root.getAsJsonObject("d");
            if (d.has("results") && d.get("results").isJsonArray()) {
                JsonArray results = d.getAsJsonArray("results");
                for (JsonElement el : results) {
                    JsonObject row = el.getAsJsonObject();
                    
                    if (row.has("zincsdhsni") && !row.get("zincsdhsni").isJsonNull()) {
//                    	String hsn = row.get("zincsdhsni").getAsString().trim();
                        // Normalize the same way SapHsnService.normalize() does for the LC-side
                        // codes (strip to digits only). Without this, SAP field padding/leading
                        // zeros/decimal formatting/non-breaking-space padding (all common with
                        // SAP fixed-length char fields, and plain .trim() does NOT strip
                        // non-ASCII whitespace like U+00A0) silently makes every code fail to
                        // match even when it's the same HSN code - which previously produced a
                        // report showing "29 HS codes not present in SAP" despite SAP genuinely
                        // returning all 29.
                        String hsn = row.get("zincsdhsni").getAsString().replaceAll("[^0-9]", "").trim();
                        if (!hsn.isEmpty()) codes.add(hsn);
                    }
                }
            }
        }
        logger.info("SAP returned {} distinct HSN code(s)", codes.size());
        logger.info("SAP HSN codes (normalized, digits-only): {}", codes);
        return new SapHsnResult(codes, !codes.isEmpty());
    }

    public boolean testConnection() {
        try {
            // A harmless probe: query with a dummy invoice; success = HTTP 200 (empty results ok)
            fetchHsnCodes("__PROBE__");
            return true;
        } catch (Exception e) {
            logger.error("SAP connection test failed: {}", e.getMessage());
            return false;
        }
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String truncate(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
