package com.smipl.lcrecon.service;

import com.smipl.lcrecon.model.ReconciliationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ReconciliationService {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationService.class);

    /**
     * Compare LC parameters against supporting document parameters.
     *
     * @param lcParams          Merged LC parameters (from master + addendums)
     * @param supportingParams  Map of docTypeName -> Map of paramName -> paramValue
     * @return List of reconciliation results
     */
    public List<ReconciliationResult> reconcile(Map<String, String> lcParams,
                                                 Map<String, Map<String, String>> supportingParams,
                                                 String lcNumber) {
        logger.info("Starting reconciliation for LC: {}", lcNumber);

        // Collect all parameter names from LC and all supporting documents
        LinkedHashSet<String> allParams = new LinkedHashSet<>(lcParams.keySet());
        for (Map<String, String> docParams : supportingParams.values()) {
            allParams.addAll(docParams.keySet());
        }

        List<String> docTypes = new ArrayList<>(supportingParams.keySet());
        List<ReconciliationResult> results = new ArrayList<>();
        int order = 0;

        for (String paramName : allParams) {
            ReconciliationResult result = new ReconciliationResult();
            result.setLcNumber(lcNumber);
            result.setParameterName(paramName);
            result.setDisplayOrder(order++);

            String lcValue = lcParams.getOrDefault(paramName, "");
            result.setLcValue(lcValue);

            Map<String, String> docValues = new LinkedHashMap<>();
            Map<String, String> cellStatus = new LinkedHashMap<>();
            boolean allMatch = true;
            boolean anyValue = false;
            boolean lcBlank = (lcValue == null || lcValue.trim().isEmpty());

            for (String docType : docTypes) {
                Map<String, String> docParams = supportingParams.get(docType);
                String docValue = docParams != null ? docParams.getOrDefault(paramName, "") : "";
                docValues.put(docType, docValue);

                boolean docBlank = (docValue == null || docValue.trim().isEmpty());
                if (!docBlank) anyValue = true;

                if (lcBlank) {
                    cellStatus.put(docType, "NO_COMPARE");
                } else if (docBlank) {
                    cellStatus.put(docType, "MISMATCH");
                    allMatch = false;
                } else if (normalizeForCompare(lcValue).equals(normalizeForCompare(docValue))) {
                    cellStatus.put(docType, "MATCH");
                } else {
                    cellStatus.put(docType, "MISMATCH");
                    allMatch = false;
                }
            }

            result.setDocumentValues(docValues);
            result.setCellMatchStatus(cellStatus);

            // Determine overall result
            if (lcBlank && !anyValue) {
                result.setMatchResult("LC_MISSING");
                result.setReason("LC data is blank but values are matching in all other data");
            } else if (lcBlank) {
                result.setMatchResult("LC_MISSING");
                result.setReason("LC data is blank but values are matching in all other data");
            } else if (allMatch) {
                result.setMatchResult("MATCHED");
                result.setReason("Data matches");
            } else {
                result.setMatchResult("NOT_MATCHED");
                result.setReason(buildMismatchReason(lcValue, docValues, cellStatus));
            }

            results.add(result);
        }

        logger.info("Reconciliation complete: {} parameters processed", results.size());
        return results;
    }

    private String normalizeForCompare(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[,.:;'\"]+$", "");
    }

    private String buildMismatchReason(String lcValue, Map<String, String> docValues,
                                       Map<String, String> cellStatus) {
        List<String> missingDocs = new ArrayList<>();
        List<String> mismatchDocs = new ArrayList<>();

        for (Map.Entry<String, String> entry : cellStatus.entrySet()) {
            String docType = entry.getKey();
            String status = entry.getValue();
            String docValue = docValues.get(docType);
            boolean docBlank = (docValue == null || docValue.trim().isEmpty());

            if ("MISMATCH".equals(status)) {
                if (docBlank) {
                    missingDocs.add(docType);
                } else {
                    mismatchDocs.add(docType);
                }
            }
        }

        StringBuilder reason = new StringBuilder("Data mismatch");
        if (!missingDocs.isEmpty()) {
            reason.append(" due to missing value in ").append(String.join(", ", missingDocs));
        }
        if (!mismatchDocs.isEmpty()) {
            if (!missingDocs.isEmpty()) reason.append(" and");
            reason.append(" due to differences in ").append(String.join(", ", mismatchDocs));
        }
        return reason.toString();
    }
}
