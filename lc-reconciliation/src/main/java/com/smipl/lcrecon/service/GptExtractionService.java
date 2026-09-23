package com.smipl.lcrecon.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smipl.lcrecon.integration.AzureOpenAIClient;
import com.smipl.lcrecon.integration.AzureOpenAIClient.GptResponse;
import com.smipl.lcrecon.model.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GptExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(GptExtractionService.class);

    /**
     * Call GPT and return the response wrapper with text and token usage.
     */
    public GptResponse callGpt(String userContent, String promptTemplate,
                           String apiUrl, String apiKey, String model,
                           String responseSchema, String schemaName) throws Exception {
        logger.info("Calling GPT (model={}), content length: {}", model, userContent.length());

        AzureOpenAIClient client = new AzureOpenAIClient(apiUrl, apiKey, model);

        // Truncate if needed
        String content = userContent.length() > 30000 ? userContent.substring(0, 30000) : userContent;

        GptResponse response = client.chatCompletion(promptTemplate, content, responseSchema, schemaName);
        logger.info("GPT response received, length: {}, tokens: {}", response.text.length(), response.totalTokens);
        return response;
    }

    /**
     * Extract LC parameters from GPT response.
     * Response format: {rows: [{Parameter, LCClauseNo, LCClauseDescription}]}
     * Returns Map of Parameter -> LCClauseDescription
     */
    public Map<String, String> parseLcParameters(String responseJson) {
        Map<String, String> params = new LinkedHashMap<>();
        try {
            JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
            if (obj.has("rows") && obj.get("rows").isJsonArray()) {
                for (JsonElement rowEl : obj.getAsJsonArray("rows")) {
                    JsonObject row = rowEl.getAsJsonObject();
                    String param = getStr(row, "Parameter");
                    String value = getStr(row, "LCClauseDescription");
                    if (param != null && !param.isEmpty()) {
                        params.put(param, value != null ? value : "");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse LC parameters: {}", e.getMessage());
        }
        logger.info("Parsed {} LC parameters", params.size());
        return params;
    }

    /**
     * Parse document comparison results from GPT response.
     * Response format: {rows: [{Parameter, LCClauseNo, LCClauseDescription, <DocData>, Status, ReasonOfNonCompliance}]}
     * The <DocData> field name varies per document (InvoiceData, BLData, etc.)
     */
    public List<ReconciliationResult> parseDocumentResults(String responseJson, String docTypeCode, String docTypeName) {
        List<ReconciliationResult> results = new ArrayList<>();
        try {
            JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
            if (obj.has("rows") && obj.get("rows").isJsonArray()) {
                JsonArray rows = obj.getAsJsonArray("rows");
                int order = 0;
                for (JsonElement rowEl : rows) {
                    JsonObject row = rowEl.getAsJsonObject();
                    ReconciliationResult r = new ReconciliationResult();
                    r.setParameterName(getStr(row, "Parameter"));
                    r.setLcClauseNo(getStr(row, "LCClauseNo"));
                    r.setLcValue(getStr(row, "LCClauseDescription"));
                    r.setStatus(getStr(row, "Status"));
                    r.setReason(getStr(row, "ReasonOfNonCompliance"));
                    r.setDocumentTypeCode(docTypeCode);
                    r.setDocumentTypeName(docTypeName);
                    r.setDisplayOrder(order++);

                    // Find the document data field (any field that's not a known field)
                    String docValue = findDocumentDataValue(row);
                    r.setDocumentValue(docValue);

                    results.add(r);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse document results: {}", e.getMessage());
        }
        logger.info("Parsed {} results for {}", results.size(), docTypeName);
        return results;
    }

    /**
     * Build a deduplicated LC parameter JSON from the raw GPT response.
     * Uses the parsed unique map (LinkedHashMap deduplicates by keeping last occurrence)
     * to rebuild a clean {rows:[...]} JSON with no duplicate parameters.
     */
    public String buildDeduplicatedLcJson(String rawResponseJson, Map<String, String> uniqueParams) {
        try {
            JsonObject rawObj = JsonParser.parseString(rawResponseJson).getAsJsonObject();
            if (!rawObj.has("rows") || !rawObj.get("rows").isJsonArray()) {
                return rawResponseJson;
            }

            // Build a map of Parameter -> full row object from raw response (last wins)
            Map<String, JsonObject> rowMap = new LinkedHashMap<>();
            for (JsonElement rowEl : rawObj.getAsJsonArray("rows")) {
                JsonObject row = rowEl.getAsJsonObject();
                String param = getStr(row, "Parameter");
                if (param != null && !param.isEmpty()) {
                    rowMap.put(param, row);
                }
            }

            // Rebuild rows array with unique entries only
            JsonArray deduplicatedRows = new JsonArray();
            for (Map.Entry<String, JsonObject> entry : rowMap.entrySet()) {
                deduplicatedRows.add(entry.getValue());
            }

            JsonObject result = new JsonObject();
            result.add("rows", deduplicatedRows);
            String deduped = result.toString();
            logger.info("LC params deduplicated: {} raw rows -> {} unique rows", rawObj.getAsJsonArray("rows").size(), deduplicatedRows.size());
            return deduped;
        } catch (Exception e) {
            logger.warn("Failed to deduplicate LC JSON, using raw: {}", e.getMessage());
            return rawResponseJson;
        }
    }

    /**
     * Extract invoice_summary from GPT response.
     * Returns the invoice_summary JSON string for passing to subsequent documents.
     */
    public String extractInvoiceSummary(String responseJson) {
        try {
            JsonObject obj = JsonParser.parseString(responseJson).getAsJsonObject();
            if (obj.has("invoice_summary")) {
                return obj.getAsJsonObject("invoice_summary").toString();
            }
        } catch (Exception e) {
            logger.error("Failed to extract invoice_summary: {}", e.getMessage());
        }
        // Fallback: return the full response as summary
        return responseJson;
    }

    /**
     * Find the document-specific data field value from a row.
     * It's the field that isn't Parameter, LCClauseNo, LCClauseDescription, Status, or ReasonOfNonCompliance.
     */
    private String findDocumentDataValue(JsonObject row) {
        for (Map.Entry<String, JsonElement> entry : row.entrySet()) {
            String key = entry.getKey();
            if (!"Parameter".equals(key) && !"LCClauseNo".equals(key) &&
                !"LCClauseDescription".equals(key) && !"Status".equals(key) &&
                !"ReasonOfNonCompliance".equals(key)) {
                return entry.getValue().isJsonNull() ? "" : entry.getValue().getAsString();
            }
        }
        return "";
    }

    private String getStr(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return null;
    }
}
