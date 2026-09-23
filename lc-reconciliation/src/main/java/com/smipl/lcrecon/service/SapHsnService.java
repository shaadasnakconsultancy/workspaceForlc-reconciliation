package com.smipl.lcrecon.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.smipl.lcrecon.integration.AzureOpenAIClient.GptResponse;
import com.smipl.lcrecon.model.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * HSN-code compliance check: extract the HSN codes declared in the LC (clause 45A -
 * Description of Goods) and verify each one exists in the SAP response for the invoice.
 *
 * Compliance rule: every HSN code declared in the LC must be present in the SAP set.
 */
public class SapHsnService {
    private static final Logger logger = LoggerFactory.getLogger(SapHsnService.class);

    public static final String DOC_TYPE_CODE = "SAP_HSN";
    public static final String DOC_TYPE_NAME = "HSN Code Verification";

    private static final String HSN_PROMPT =
            "You are a trade-finance data extractor. From the provided Letter of Credit (LC) text, "
            + "extract every HSN / HS / tariff code declared for the goods. HSN codes are numeric "
            + "(usually 8 digits, sometimes 6). They are most commonly listed under clause 45A "
            + "(Description of Goods and/or Services), but include any HSN codes found elsewhere in the LC. "
            + "Return ONLY the digits of each code (strip dots/spaces). Do not invent codes. "
            + "If none are present, return an empty list.";

    private static final String HSN_SCHEMA =
            "{\"type\":\"object\",\"additionalProperties\":false,"
            + "\"properties\":{\"hsn_codes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}},"
            + "\"required\":[\"hsn_codes\"]}";

    /**
     * Run the focused GPT extraction to pull HSN codes from the LC text.
     * Returned GptResponse is exposed so the caller can record token cost.
     */
    public GptResponse extractHsnGpt(String combinedLcText, GptExtractionService gptService,
                                     String openaiUrl, String openaiKey, String openaiModel) throws Exception {
        String userContent = "LC_TEXT: " + combinedLcText;
        return gptService.callGpt(userContent, HSN_PROMPT, openaiUrl, openaiKey, openaiModel,
                HSN_SCHEMA, "lc_hsn_codes");
    }

    /**
     * Parse the {@code {"hsn_codes":[...]}} response into a distinct, normalized set.
     */
    public Set<String> parseLcHsnCodes(String gptResponseText) {
        Set<String> codes = new LinkedHashSet<>();
        try {
            JsonObject obj = JsonParser.parseString(gptResponseText).getAsJsonObject();
            if (obj.has("hsn_codes") && obj.get("hsn_codes").isJsonArray()) {
                JsonArray arr = obj.getAsJsonArray("hsn_codes");
                for (JsonElement el : arr) {
                    if (!el.isJsonNull()) {
                        String code = normalize(el.getAsString());
                        if (!code.isEmpty()) codes.add(code);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to parse LC HSN codes: {}", e.getMessage());
        }
        logger.info("Extracted {} HSN code(s) from LC", codes.size());
        return codes;
    }

    /**
     * Build a single result row: all LC HSN codes (comma-separated) with the codes that were
     * NOT found in the comparison source shown against them.
     * Rule: every LC HSN code must exist in the comparison set (SAP or Invoice).
     *
     * @param lcHsnCodes   HSN codes declared in the LC (clause 45A)
     * @param compareCodes HSN codes from the source (SAP response or invoice page 2+)
     * @param sourceLabel  human label, e.g. "SAP" or "Invoice"
     * @param contextId    invoice number (SAP) or invoice file name (Invoice mode), for the reason text
     */
    public List<ReconciliationResult> buildComparisonRow(Set<String> lcHsnCodes, Set<String> compareCodes,
                                                         String sourceLabel, String contextId) {
        List<ReconciliationResult> results = new ArrayList<>();

        if (lcHsnCodes == null || lcHsnCodes.isEmpty()) {
            results.add(row("HSN Codes", "", "", "Not Applicable",
                    "No HSN codes found in the LC (clause 45A / Description of Goods).", 0));
            return results;
        }

        Set<String> src = compareCodes != null ? compareCodes : new LinkedHashSet<String>();
        List<String> missing = new ArrayList<>();
        for (String lcCode : lcHsnCodes) {
            if (!src.contains(lcCode)) missing.add(lcCode);
        }

        String lcJoined = join(lcHsnCodes);
        boolean complied = missing.isEmpty();
        String status = complied ? "Complied" : "Not Complied";
        String value = complied ? "All present in " + sourceLabel : join(missing);
        String reason = complied ? ""
                : missing.size() + " HS code(s) declared in LC not present in " + sourceLabel
                  + " (" + contextId + "): " + join(missing);
        results.add(row("HSN Codes", lcJoined, value, status, reason, 0));
        return results;
    }

    /**
     * Build a single row when the check could not be performed (e.g. SAP not configured, extraction error).
     * Always Not Complied so the parameter is visible in the report.
     */
    public List<ReconciliationResult> buildUnverifiedRow(Set<String> lcHsnCodes, String reasonMessage) {
        List<ReconciliationResult> results = new ArrayList<>();
        String lcJoined = join(lcHsnCodes);
        results.add(row("HSN Codes", lcJoined, "Not verified", "Not Complied", reasonMessage, 0));
        return results;
    }

    private ReconciliationResult row(String param, String lcValue, String sapValue,
                                     String status, String reason, int order) {
        ReconciliationResult r = new ReconciliationResult();
        r.setParameterName(param);
        r.setLcValue(lcValue);
        r.setDocumentValue(sapValue);
        r.setDocumentDataField("SAPData");
        r.setStatus(status);
        r.setReason(reason);
        r.setDocumentTypeCode(DOC_TYPE_CODE);
        r.setDocumentTypeName(DOC_TYPE_NAME);
        r.setDisplayOrder(order);
        return r;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replaceAll("[^0-9]", "").trim();
    }

    private String join(java.util.Collection<String> codes) {
        if (codes == null || codes.isEmpty()) return "";
        return String.join(", ", codes);
    }
}
