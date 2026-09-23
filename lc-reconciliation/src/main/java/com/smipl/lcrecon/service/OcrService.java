package com.smipl.lcrecon.service;

import com.smipl.lcrecon.integration.AzureDocIntelligenceClient;
import com.smipl.lcrecon.integration.AzureDocIntelligenceClient.OcrResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class OcrService {
    private static final Logger logger = LoggerFactory.getLogger(OcrService.class);

    public OcrResponse performOcr(String filePath, int pageLimit, String endpoint, String apiKey) throws Exception {
        logger.info("Starting OCR for file: {}, pageLimit: {}", filePath, pageLimit);

        AzureDocIntelligenceClient client = new AzureDocIntelligenceClient(endpoint, apiKey);
        byte[] pdfBytes = Files.readAllBytes(Paths.get(filePath));

        int retries = 3;
        for (int attempt = 1; attempt <= retries; attempt++) {
            try {
                OcrResponse result = client.analyzeDocument(pdfBytes, pageLimit);
                logger.info("OCR completed for: {}, extracted {} chars, {} pages", filePath, result.text.length(), result.pagesProcessed);
                return result;
            } catch (Exception e) {
                logger.warn("OCR attempt {} failed for {}: {}", attempt, filePath, e.getMessage());
                if (attempt == retries) throw e;
                Thread.sleep(2000 * attempt); // exponential backoff
            }
        }
        throw new RuntimeException("OCR failed after " + retries + " attempts");
    }
}
