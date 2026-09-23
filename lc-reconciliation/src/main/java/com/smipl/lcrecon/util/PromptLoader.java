package com.smipl.lcrecon.util;

import com.smipl.lcrecon.dao.PromptTemplateDao;
import com.smipl.lcrecon.model.PromptTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility to load prompt files into the database on startup if no active prompt exists.
 */
public class PromptLoader {
    private static final Logger logger = LoggerFactory.getLogger(PromptLoader.class);

    private static final String PROMPT_DIR = "C:/Users/GauravBansal/Documents/LC_Documents/LC_Documents/Prompt/Individual";

    // Document type code -> {prompt file, schema, schema name}
    private static final Map<String, String[]> PROMPT_MAP = new LinkedHashMap<>();

    static {
        PROMPT_MAP.put("MASTER_LC", new String[]{
                "LC_Parameter_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"LCClauseDescription\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"LCClauseDescription\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("INVOICE", new String[]{
                "INV_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"LCClauseDescription\":{\"type\":\"string\"},\"InvoiceData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"LCClauseDescription\",\"InvoiceData\",\"Status\",\"ReasonOfNonCompliance\"]}},\"invoice_summary\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"InvoiceDate\":{\"type\":\"string\"},\"InvoiceNo\":{\"type\":\"string\"},\"FinalDestination\":{\"type\":\"string\"},\"MarksNos\":{\"type\":\"string\"},\"DeliveryMode\":{\"type\":\"string\"},\"PortOfLoading\":{\"type\":\"string\"}},\"required\":[\"InvoiceDate\",\"InvoiceNo\",\"FinalDestination\",\"MarksNos\",\"DeliveryMode\",\"PortOfLoading\"]}},\"required\":[\"rows\",\"invoice_summary\"]}"
        });
        PROMPT_MAP.put("PACKING_LIST", new String[]{
                "PL_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"PackingData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"PackingData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("BL", new String[]{
                "BL_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"BLData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"BLData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("COO", new String[]{
                "COO_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"COOData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"COOData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("BENEF_CERT", new String[]{
                "BC_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"BCData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"BCData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("BILL_EXCH", new String[]{
                "BE_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"BEData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"BEData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("SHIP_ADVISE", new String[]{
                "SA_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"SAData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"SAData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
        PROMPT_MAP.put("VESSEL_CERT", new String[]{
                "VC_Prompt.txt",
                "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"rows\":{\"type\":\"array\",\"items\":{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"Parameter\":{\"type\":\"string\"},\"LCClauseNo\":{\"type\":\"string\"},\"VCData\":{\"type\":\"string\"},\"Status\":{\"type\":\"string\",\"enum\":[\"Complied\",\"Not Complied\",\"Not Applicable\"]},\"ReasonOfNonCompliance\":{\"type\":\"string\"}},\"required\":[\"Parameter\",\"LCClauseNo\",\"VCData\",\"Status\",\"ReasonOfNonCompliance\"]}}},\"required\":[\"rows\"]}"
        });
    }

    public static void loadPrompts(PromptTemplateDao promptDao,
                                    com.smipl.lcrecon.dao.DocumentTypeDao docTypeDao) {
        logger.info("Loading prompt templates from: {}", PROMPT_DIR);

        for (Map.Entry<String, String[]> entry : PROMPT_MAP.entrySet()) {
            String typeCode = entry.getKey();
            String fileName = entry.getValue()[0];
            String schema = entry.getValue()[1];

            try {
                // Find document type
                com.smipl.lcrecon.model.DocumentType dt = docTypeDao.findByCode(typeCode);
                if (dt == null) {
                    logger.warn("Document type not found: {}. Skipping.", typeCode);
                    continue;
                }

                // Read prompt file
                Path promptPath = Paths.get(PROMPT_DIR, fileName);
                // Also try original directory for Invoice and PL which aren't in Individual
                if (!Files.exists(promptPath)) {
                    promptPath = Paths.get(PROMPT_DIR.replace("/Individual", ""), fileName);
                }
                if (!Files.exists(promptPath)) {
                    logger.warn("Prompt file not found: {}. Skipping.", fileName);
                    continue;
                }

                String promptText = new String(Files.readAllBytes(promptPath), "UTF-8");

                // Deactivate existing prompts
                promptDao.deactivateAllForDocType(dt.getId());

                // Create new active prompt
                PromptTemplate pt = new PromptTemplate();
                pt.setDocumentTypeId(dt.getId());
                pt.setPromptName(dt.getTypeName() + " Prompt");
                pt.setPromptText(promptText);
                pt.setResponseSchema(schema);
                pt.setActive(true);
                pt.setVersion(1);
                promptDao.create(pt);

                logger.info("Loaded prompt for {}: {} ({} chars)", typeCode, fileName, promptText.length());
            } catch (Exception e) {
                logger.error("Failed to load prompt for {}: {}", typeCode, e.getMessage());
            }
        }

        logger.info("Prompt loading complete.");
    }
}
