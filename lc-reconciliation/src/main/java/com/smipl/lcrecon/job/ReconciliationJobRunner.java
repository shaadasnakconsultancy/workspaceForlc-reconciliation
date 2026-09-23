package com.smipl.lcrecon.job;

import com.smipl.lcrecon.dao.*;
import com.smipl.lcrecon.integration.AzureDocIntelligenceClient.OcrResponse;
import com.smipl.lcrecon.integration.AzureOpenAIClient.GptResponse;
import com.smipl.lcrecon.model.*;
import com.smipl.lcrecon.service.*;
import com.smipl.lcrecon.service.XlsxReportService;
import com.smipl.lcrecon.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import java.io.File;
import java.math.BigDecimal;
import java.util.*;

public class ReconciliationJobRunner implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ReconciliationJobRunner.class);

    private final long jobId;
    private final ServletContext ctx;

    public ReconciliationJobRunner(long jobId, ServletContext ctx) {
        this.jobId = jobId;
        this.ctx = ctx;
    }

    @Override
    public void run() {
        JobDao jobDao = (JobDao) ctx.getAttribute("jobDao");
        LcDocumentDao lcDocDao = (LcDocumentDao) ctx.getAttribute("lcDocumentDao");
        SettingsDao settingsDao = (SettingsDao) ctx.getAttribute("settingsDao");
        PromptTemplateDao promptDao = (PromptTemplateDao) ctx.getAttribute("promptTemplateDao");

        OcrService ocrService = new OcrService();
        GptExtractionService gptService = new GptExtractionService();
        ReportGenerationService reportService = new ReportGenerationService();
        EmailService emailService = new EmailService();

        ReconciliationJob job = jobDao.findById(jobId);
        if (job == null) { logger.error("Job not found: {}", jobId); return; }

        String batchId = job.getUploadBatchId();
        String lcNumber = null;

        try {
            Map<String, String> openaiSettings = settingsDao.getSettingsByGroup("OPENAI");
            Map<String, String> docIntelSettings = settingsDao.getSettingsByGroup("DOC_INTELLIGENCE");
            Map<String, String> smtpSettings = settingsDao.getSettingsByGroup("SMTP");
            Map<String, String> graphSettings = settingsDao.getSettingsByGroup("GRAPH_API");
            Map<String, String> sapSettings = settingsDao.getSettingsByGroup("SAP_API");
            com.smipl.lcrecon.integration.SapApiClient sapClient = new com.smipl.lcrecon.integration.SapApiClient(sapSettings);
            boolean hsnCheckEnabled = sapClient.isEnabled();
            String hsnSource = sapSettings.getOrDefault("hsn_source", "SAP");

            // Load cost settings
            Map<String, String> costSettings = settingsDao.getSettingsByGroup("COST_RATES");
            JobCostDao costDao = (JobCostDao) ctx.getAttribute("jobCostDao");
            double usdToInr = parseDouble(costSettings.getOrDefault("cost_usd_to_inr", "85.00"));
            double inputRate = parseDouble(costSettings.getOrDefault("cost_openai_input_per_1m_tokens", "0.15")) * usdToInr;
            double outputRate = parseDouble(costSettings.getOrDefault("cost_openai_output_per_1m_tokens", "0.60")) * usdToInr;
            double ocrPageRate = parseDouble(costSettings.getOrDefault("cost_doc_intel_per_page", "0.001")) * usdToInr;

            String docIntelEndpoint = docIntelSettings.get("doc_intel_endpoint");
            String docIntelKey = docIntelSettings.get("doc_intel_api_key");
            FileConversionService conversionService = new FileConversionService();
            String openaiUrl = openaiSettings.getOrDefault("openai_endpoint", "https://api.openai.com/v1/responses");
            String openaiKey = openaiSettings.get("openai_api_key");
            String openaiModel = openaiSettings.getOrDefault("openai_model", "gpt-4o-mini");

            List<LcDocument> lcDocs = lcDocDao.findByBatchId(batchId);
            List<SupportingDocument> supDocs = lcDocDao.findSupportingByBatchId(batchId);

            int totalSteps = lcDocs.size() + 1 + supDocs.size() * 2 + 2; // OCR each LC + 1 GPT LC + 2 per sup + report + email
            if (hsnCheckEnabled) totalSteps += 1; // HSN verification (SAP or Invoice)
            jobDao.updateTotalSteps(jobId, totalSteps);
            jobDao.updateStatus(jobId, "RUNNING", "Initializing...", 0);
            jobDao.addLog(jobId, "INFO", "Job started. LC docs: " + lcDocs.size() + ", Supporting docs: " + supDocs.size() + ", Total steps: " + totalSteps);
            int completedSteps = 0;

            // ============================================================
            // STEP 1: OCR all LC Documents
            // ============================================================
            checkAborted();
            StringBuilder combinedLcText = new StringBuilder();
            for (LcDocument lcDoc : lcDocs) {
                // Convert to PDF if needed
                String ocrFilePath = lcDoc.getFilePath();
                if (conversionService.needsConversion(ocrFilePath)) {
                    String convStep = "Converting: " + lcDoc.getFileName() + " to PDF";
                    jobDao.updateStatus(jobId, "RUNNING", convStep, completedSteps);
                    jobDao.addLog(jobId, "INFO", convStep);
                    ocrFilePath = conversionService.convertToPdfIfNeeded(ocrFilePath, graphSettings);
                }

                String step = "OCR: " + lcDoc.getFileName() + (lcDoc.isAddendum() ? " (Addendum)" : " (Master LC)");
                jobDao.updateStatus(jobId, "RUNNING", step, completedSteps);
                jobDao.addLog(jobId, "INFO", step);

                OcrResponse ocrResult = ocrService.performOcr(ocrFilePath, 0, docIntelEndpoint, docIntelKey);
                lcDocDao.updateOcrStatus(lcDoc.getId(), "COMPLETED", ocrResult.text);

                if (costDao != null) {
                    JobApiCost cost = new JobApiCost();
                    cost.setJobId(jobId);
                    cost.setApiType("OCR");
                    cost.setDocumentName(lcDoc.getFileName());
                    cost.setPagesProcessed(ocrResult.pagesProcessed);
                    cost.setCostInr(BigDecimal.valueOf(ocrResult.pagesProcessed * ocrPageRate));
                    costDao.save(cost);
                }

                combinedLcText.append(lcDoc.isAddendum() ? "\n\n=== LC ADDENDUM ===\n" : "=== MASTER LC ===\n");
                combinedLcText.append(ocrResult.text);
                completedSteps++;
            }

            // ============================================================
            // STEP 2: Extract LC Parameters (single GPT call)
            // ============================================================
            checkAborted();
            jobDao.updateStatus(jobId, "RUNNING", "Extracting LC Parameters (GPT)", completedSteps);
            jobDao.addLog(jobId, "INFO", "Extracting LC Parameters");

            PromptTemplate lcPrompt = promptDao.findActiveByDocumentTypeCode("MASTER_LC");
            String lcParamsJson = "{}";
            Map<String, String> lcParams = new LinkedHashMap<>();

            if (lcPrompt != null) {
                String lcUserContent = "LC_TEXT: " + combinedLcText.toString();
                GptResponse lcGptResponse = gptService.callGpt(lcUserContent, lcPrompt.getPromptText(),
                        openaiUrl, openaiKey, openaiModel, lcPrompt.getResponseSchema(), "lc_parameter");
                lcParams = gptService.parseLcParameters(lcGptResponse.text);
                // Rebuild deduplicated ComplianceMasterList JSON from parsed unique params
                // (LinkedHashMap already deduplicates - last occurrence wins for addendum overrides)
                lcParamsJson = gptService.buildDeduplicatedLcJson(lcGptResponse.text, lcParams);

                if (costDao != null) {
                    JobApiCost cost = new JobApiCost();
                    cost.setJobId(jobId);
                    cost.setApiType("GPT");
                    cost.setDocumentName("MASTER_LC");
                    cost.setPromptTokens(lcGptResponse.promptTokens);
                    cost.setCompletionTokens(lcGptResponse.completionTokens);
                    cost.setTotalTokens(lcGptResponse.totalTokens);
                    cost.setCostInr(BigDecimal.valueOf(
                        (lcGptResponse.promptTokens * inputRate / 1_000_000.0) +
                        (lcGptResponse.completionTokens * outputRate / 1_000_000.0)
                    ));
                    costDao.save(cost);
                }

                for (LcDocument lcDoc : lcDocs) {
                    lcDocDao.updateExtractedJson(lcDoc.getId(), JsonUtil.mapToJson(lcParams));
                }
                if (lcParams.containsKey("LCNumber")) lcNumber = lcParams.get("LCNumber");
                else if (lcParams.containsKey("LCNo")) lcNumber = lcParams.get("LCNo");
            }
            completedSteps++;

            if (lcNumber != null) {
                job.setLcNumber(lcNumber);
                jobDao.updateLcNumber(jobId, lcNumber);
            }
            jobDao.addLog(jobId, "INFO", "LC params extracted: " + lcParams.size() + ". LC#: " + lcNumber);

            // ============================================================
            // STEP 3: Separate Invoice from other docs
            // ============================================================
            SupportingDocument invoiceDoc = null;
            List<SupportingDocument> otherDocs = new ArrayList<>();
            for (SupportingDocument sd : supDocs) {
                if ("INVOICE".equals(sd.getDocumentTypeCode())) invoiceDoc = sd;
                else otherDocs.add(sd);
            }

            List<ReconciliationResult> allResults = new ArrayList<>();
            List<String> docTypeNames = new ArrayList<>();
            String invoiceSummary = null;
            String sapInvoiceNo = null;
            String invoicePdfPath = null;
            String invoiceFileName = null;

            // ============================================================
            // STEP 4: OCR & Extract Invoice
            // ============================================================
            checkAborted();
            if (invoiceDoc != null) {
                // Convert Invoice if needed
                String invoiceFilePath = invoiceDoc.getFilePath();
                if (conversionService.needsConversion(invoiceFilePath)) {
                    jobDao.addLog(jobId, "INFO", "Converting: " + invoiceDoc.getFileName() + " to PDF");
                    invoiceFilePath = conversionService.convertToPdfIfNeeded(invoiceFilePath, graphSettings);
                }
                invoicePdfPath = invoiceFilePath;             // final (possibly converted) invoice PDF for HSN extraction
                invoiceFileName = invoiceDoc.getFileName();

                jobDao.updateStatus(jobId, "RUNNING", "OCR: " + invoiceDoc.getFileName(), completedSteps);
                jobDao.addLog(jobId, "INFO", "OCR: " + invoiceDoc.getFileName());

                OcrResponse invoiceOcrResult = ocrService.performOcr(invoiceFilePath, invoiceDoc.getPageLimit(),
                        docIntelEndpoint, docIntelKey);
                String invoiceOcr = invoiceOcrResult.text;
                lcDocDao.updateSupportingOcrStatus(invoiceDoc.getId(), "COMPLETED", invoiceOcr);

                if (costDao != null) {
                    JobApiCost cost = new JobApiCost();
                    cost.setJobId(jobId);
                    cost.setApiType("OCR");
                    cost.setDocumentName(invoiceDoc.getFileName());
                    cost.setPagesProcessed(invoiceOcrResult.pagesProcessed);
                    cost.setCostInr(BigDecimal.valueOf(invoiceOcrResult.pagesProcessed * ocrPageRate));
                    costDao.save(cost);
                }

                completedSteps++;

                jobDao.updateStatus(jobId, "RUNNING", "Extract Invoice (GPT)", completedSteps);
                jobDao.addLog(jobId, "INFO", "Extracting Invoice with LC context");

                PromptTemplate invoicePrompt = promptDao.findActiveByDocumentTypeCode("INVOICE");
                if (invoicePrompt != null) {
                    // Build user content matching the payload format
                    String userContent = "ComplianceMasterList: " + lcParamsJson +
                            "\n\nInvoiceDocument: " + invoiceOcr;

                    GptResponse invoiceGptResponse = gptService.callGpt(userContent, invoicePrompt.getPromptText(),
                            openaiUrl, openaiKey, openaiModel, invoicePrompt.getResponseSchema(), "lc_inv_reconciliation");

                    if (costDao != null) {
                        JobApiCost cost = new JobApiCost();
                        cost.setJobId(jobId);
                        cost.setApiType("GPT");
                        cost.setDocumentName(invoiceDoc.getFileName());
                        cost.setPromptTokens(invoiceGptResponse.promptTokens);
                        cost.setCompletionTokens(invoiceGptResponse.completionTokens);
                        cost.setTotalTokens(invoiceGptResponse.totalTokens);
                        cost.setCostInr(BigDecimal.valueOf(
                            (invoiceGptResponse.promptTokens * inputRate / 1_000_000.0) +
                            (invoiceGptResponse.completionTokens * outputRate / 1_000_000.0)
                        ));
                        costDao.save(cost);
                    }

                    // Parse results and fill LC values from lcParams
                    List<ReconciliationResult> invoiceResults = gptService.parseDocumentResults(
                            invoiceGptResponse.text, "INVOICE", invoiceDoc.getDocumentTypeName());
                    fillMissingLcValues(invoiceResults, lcParams);
                    allResults.addAll(invoiceResults);
                    docTypeNames.add(invoiceDoc.getDocumentTypeName());

                    // Extract invoice_summary for subsequent docs
                    invoiceSummary = gptService.extractInvoiceSummary(invoiceGptResponse.text);
                    sapInvoiceNo = extractInvoiceNo(invoiceSummary);

                    lcDocDao.updateSupportingExtractedJson(invoiceDoc.getId(), invoiceGptResponse.text);
                    jobDao.addLog(jobId, "INFO", "Invoice processed: " + invoiceResults.size() + " params. Summary extracted.");
                }
                completedSteps++;
            }

            // ============================================================
            // STEP 5: OCR & Extract each remaining document individually
            // ============================================================
            checkAborted();
            for (SupportingDocument supDoc : otherDocs) {
                checkAborted();
                // Convert if needed
                String supFilePath = supDoc.getFilePath();
                if (conversionService.needsConversion(supFilePath)) {
                    jobDao.addLog(jobId, "INFO", "Converting: " + supDoc.getFileName() + " to PDF");
                    supFilePath = conversionService.convertToPdfIfNeeded(supFilePath, graphSettings);
                }

                jobDao.updateStatus(jobId, "RUNNING", "OCR: " + supDoc.getFileName(), completedSteps);
                jobDao.addLog(jobId, "INFO", "OCR: " + supDoc.getFileName());

                OcrResponse supOcrResult = ocrService.performOcr(supFilePath, supDoc.getPageLimit(),
                        docIntelEndpoint, docIntelKey);
                String ocrText = supOcrResult.text;
                lcDocDao.updateSupportingOcrStatus(supDoc.getId(), "COMPLETED", ocrText);

                if (costDao != null) {
                    JobApiCost cost = new JobApiCost();
                    cost.setJobId(jobId);
                    cost.setApiType("OCR");
                    cost.setDocumentName(supDoc.getFileName());
                    cost.setPagesProcessed(supOcrResult.pagesProcessed);
                    cost.setCostInr(BigDecimal.valueOf(supOcrResult.pagesProcessed * ocrPageRate));
                    costDao.save(cost);
                }

                completedSteps++;

                String step = "Extract " + supDoc.getDocumentTypeName() + " (GPT)";
                jobDao.updateStatus(jobId, "RUNNING", step, completedSteps);
                jobDao.addLog(jobId, "INFO", step);

                PromptTemplate prompt = promptDao.findActiveByDocumentTypeCode(supDoc.getDocumentTypeCode());
                if (prompt != null) {
                    // Build user content with LC params + invoice summary + doc text
                    StringBuilder userContent = new StringBuilder();
                    userContent.append("ComplianceMasterList: ").append(lcParamsJson);
                    if (invoiceSummary != null) {
                        userContent.append("\n\ninvoice_summary: ").append(invoiceSummary);
                    }
                    // Use document type code for the label
                    String docLabel = supDoc.getDocumentTypeCode().replace("_", "") + "Document";
                    userContent.append("\n\n").append(docLabel).append(": ").append(ocrText);

                    String schemaName = "lc_" + supDoc.getDocumentTypeCode().toLowerCase() + "_reconciliation";
                    GptResponse docGptResponse = gptService.callGpt(userContent.toString(), prompt.getPromptText(),
                            openaiUrl, openaiKey, openaiModel, prompt.getResponseSchema(), schemaName);

                    if (costDao != null) {
                        JobApiCost cost = new JobApiCost();
                        cost.setJobId(jobId);
                        cost.setApiType("GPT");
                        cost.setDocumentName(supDoc.getFileName());
                        cost.setPromptTokens(docGptResponse.promptTokens);
                        cost.setCompletionTokens(docGptResponse.completionTokens);
                        cost.setTotalTokens(docGptResponse.totalTokens);
                        cost.setCostInr(BigDecimal.valueOf(
                            (docGptResponse.promptTokens * inputRate / 1_000_000.0) +
                            (docGptResponse.completionTokens * outputRate / 1_000_000.0)
                        ));
                        costDao.save(cost);
                    }

                    List<ReconciliationResult> docResults = gptService.parseDocumentResults(
                            docGptResponse.text, supDoc.getDocumentTypeCode(), supDoc.getDocumentTypeName());
                    fillMissingLcValues(docResults, lcParams);
                    allResults.addAll(docResults);
                    if (!docTypeNames.contains(supDoc.getDocumentTypeName())) {
                        docTypeNames.add(supDoc.getDocumentTypeName());
                    }

                    lcDocDao.updateSupportingExtractedJson(supDoc.getId(), docGptResponse.text);
                    jobDao.addLog(jobId, "INFO", supDoc.getDocumentTypeName() + " processed: " + docResults.size() + " params.");
                } else {
                    jobDao.addLog(jobId, "WARN", "No prompt for " + supDoc.getDocumentTypeCode());
                }
                completedSteps++;
            }

            // ============================================================
            // STEP 5b: HSN Code compliance check (source = SAP API or Invoice)
            // ============================================================
            if (hsnCheckEnabled) {
                checkAborted();
                SapHsnService sapHsnService = new SapHsnService();
                jobDao.updateStatus(jobId, "RUNNING", "HSN Code verification", completedSteps);
                jobDao.addLog(jobId, "INFO", "Starting HSN Code verification (source=" + hsnSource + ")");

                // Extract HSN codes declared in the LC (clause 45A) - needed in both modes
                Set<String> lcHsnCodes = new LinkedHashSet<>();
                try {
                    GptResponse hsnGpt = sapHsnService.extractHsnGpt(combinedLcText.toString(),
                            gptService, openaiUrl, openaiKey, openaiModel);
                    lcHsnCodes = sapHsnService.parseLcHsnCodes(hsnGpt.text);
                    if (costDao != null) {
                        JobApiCost cost = new JobApiCost();
                        cost.setJobId(jobId);
                        cost.setApiType("GPT");
                        cost.setDocumentName("LC_HSN_EXTRACTION");
                        cost.setPromptTokens(hsnGpt.promptTokens);
                        cost.setCompletionTokens(hsnGpt.completionTokens);
                        cost.setTotalTokens(hsnGpt.totalTokens);
                        cost.setCostInr(BigDecimal.valueOf(
                            (hsnGpt.promptTokens * inputRate / 1_000_000.0) +
                            (hsnGpt.completionTokens * outputRate / 1_000_000.0)
                        ));
                        costDao.save(cost);
                    }
                    jobDao.addLog(jobId, "INFO", "LC HSN codes extracted: " + lcHsnCodes.size());
                } catch (Exception e) {
                    jobDao.addLog(jobId, "WARN", "LC HSN extraction failed: " + e.getMessage());
                }

                List<ReconciliationResult> hsnResults;
                boolean useSap = "SAP".equalsIgnoreCase(hsnSource);

                // Invoice mode: try open-source extraction from invoice page 2+; if scanned (no text), fall back to SAP
                if (!useSap) {
                    Set<String> invoiceHsn = new LinkedHashSet<>();
                    if (invoicePdfPath != null) {
                        invoiceHsn = new InvoiceHsnExtractor().extractHsnCodes(invoicePdfPath);
                    }
                    if (invoiceHsn.isEmpty()) {
                        // Likely a scanned/image invoice (no embedded text) -> auto fall back to SAP API
                        jobDao.addLog(jobId, "WARN", "No HSN text extracted from invoice (scanned/image PDF). "
                                + "Falling back to SAP API for HSN verification.");
                        useSap = true;
                    } else {
                        jobDao.addLog(jobId, "INFO", "Invoice HSN codes extracted (page 2+): " + invoiceHsn.size());
                        hsnResults = sapHsnService.buildComparisonRow(lcHsnCodes, invoiceHsn, "Invoice",
                                invoiceFileName != null ? invoiceFileName : "invoice");
                        allResults.addAll(hsnResults);
                        if (!docTypeNames.contains(SapHsnService.DOC_TYPE_NAME)) {
                            docTypeNames.add(SapHsnService.DOC_TYPE_NAME);
                        }
                        useSap = false; // handled via invoice
                        completedSteps++;
                        // skip SAP path
                        hsnResults = null;
                    }
                }

                // SAP mode (primary, or fallback from a scanned invoice)
                if (useSap) {
                    if (!sapClient.isConfigured()) {
                        jobDao.addLog(jobId, "WARN", "HSN not verified: SAP credentials not configured.");
                        hsnResults = sapHsnService.buildUnverifiedRow(lcHsnCodes,
                                "SAP credentials not configured - HSN codes could not be verified.");
                    } else if (sapInvoiceNo == null || sapInvoiceNo.trim().isEmpty()) {
                        jobDao.addLog(jobId, "WARN", "HSN not verified: invoice number not found in Invoice document.");
                        hsnResults = sapHsnService.buildUnverifiedRow(lcHsnCodes,
                                "Invoice number not found in Invoice document - cannot query SAP.");
                    } else {
                        try {
                            com.smipl.lcrecon.integration.SapApiClient.SapHsnResult sapResult =
                                    sapClient.fetchHsnCodes(sapInvoiceNo);
                            jobDao.addLog(jobId, "INFO", "SAP returned " + sapResult.hsnCodes.size()
                                    + " HSN code(s) for invoice " + sapInvoiceNo
                                    + (sapResult.found ? "" : " (no rows found)"));
                            hsnResults = sapHsnService.buildComparisonRow(lcHsnCodes, sapResult.hsnCodes, "SAP", sapInvoiceNo);
                        } catch (Exception e) {
                            jobDao.addLog(jobId, "ERROR", "SAP HSN lookup failed: " + e.getMessage());
                            hsnResults = sapHsnService.buildUnverifiedRow(lcHsnCodes,
                                    "Could not verify HSN against SAP for invoice " + sapInvoiceNo + ": " + e.getMessage());
                        }
                    }
                    allResults.addAll(hsnResults);
                    if (!docTypeNames.contains(SapHsnService.DOC_TYPE_NAME)) {
                        docTypeNames.add(SapHsnService.DOC_TYPE_NAME);
                    }
                    completedSteps++;
                }
            }

            // ============================================================
            // STEP 6: Save results & generate reports
            // ============================================================
            jobDao.updateStatus(jobId, "RUNNING", "Saving results & generating reports...", completedSteps);
            jobDao.saveResults(jobId, lcNumber, allResults);

            int compliedCount = 0;
            int nonCompliedCount = 0;
            for (ReconciliationResult r : allResults) {
                if ("Complied".equals(r.getStatus())) compliedCount++;
                else nonCompliedCount++;
            }

            jobDao.addLog(jobId, "INFO", "Results: " + compliedCount + " Complied, " + nonCompliedCount + " Non-Complied out of " + allResults.size());

            // Generate HTML reports
            String fullReportPath = reportService.generateFullReport(jobId, lcNumber, allResults, docTypeNames);
            String ncReportPath = reportService.generateNonComplianceReport(jobId, lcNumber, allResults, docTypeNames);

            // Generate XLSX reports
            XlsxReportService xlsxService = new XlsxReportService();
            String xlsxReportPath = xlsxService.generateFullReport(jobId, lcNumber, job.getJobName(), job.getShipmentDocName(), allResults, docTypeNames);
            String xlsxNcReportPath = xlsxService.generateNonComplianceReport(jobId, lcNumber, job.getJobName(), job.getShipmentDocName(), allResults, docTypeNames);
            completedSteps++;

            // ============================================================
            // STEP 7: Send email with all 4 report attachments
            // ============================================================
            checkAborted(); // don't mail out a report for a job the user has cancelled
            jobDao.updateStatus(jobId, "RUNNING", "Sending email...", completedSteps);

            String notifyEmail = resolveRecipients(job);

            // Default subject/body, overridden by the configured template when one is available.
            String subject = "LC Reconciliation Report - " + lcNumber;
            String body = emailService.buildSuccessEmailBody(jobId, lcNumber, compliedCount, allResults.size());
            try {
                // Try to use email template (default first, then any active)
                EmailTemplateDao emailTemplateDao = (EmailTemplateDao) ctx.getAttribute("emailTemplateDao");
                com.smipl.lcrecon.model.EmailTemplate template = emailTemplateDao.findDefault();
                if (template == null) {
                    List<com.smipl.lcrecon.model.EmailTemplate> templates = emailTemplateDao.findAll();
                    if (templates != null && !templates.isEmpty()) {
                        for (com.smipl.lcrecon.model.EmailTemplate t : templates) {
                            if (t.isActive()) { template = t; break; }
                        }
                    }
                }
                if (template != null) {
                    Map<String, String> placeholders = buildPlaceholders(job, lcNumber, compliedCount, allResults.size());
                    subject = template.resolveSubject(placeholders);
                    body = template.resolveBody(placeholders);
                }
            } catch (Exception e) {
                jobDao.addLog(jobId, "WARN", "Email template could not be loaded, using the default layout: " + e.getMessage());
            }

            // Send with 2 Excel report attachments. sendReport never throws and is bounded by the
            // configured SMTP timeout, so the job always reaches COMPLETED below.
            EmailService.EmailResult emailResult = emailService.sendReport(smtpSettings, notifyEmail, subject, body,
                    new File(xlsxReportPath), new File(xlsxNcReportPath));
            if (emailResult.isSent()) {
                jobDao.addLog(jobId, "INFO", "Email sent to: " + notifyEmail + " with Excel report attachments");
            } else {
                jobDao.addLog(jobId, "WARN", "Email not sent. " + emailResult.message);
            }
            completedSteps++;

            // Final gate: an abort during the email step must not be overwritten with COMPLETED.
            checkAborted();

            // The reconciliation itself succeeded; a mail problem only affects the notification.
            String completionStep = emailResult.isSent() ? "Completed" : "Completed (email not sent)";
            jobDao.updateCompleted(jobId, "COMPLETED", fullReportPath, ncReportPath, xlsxReportPath, xlsxNcReportPath, null);
            jobDao.updateEmailSent(jobId, emailResult.isSent(), emailResult.isSent() ? null : emailResult.message);
            jobDao.updateStatus(jobId, "COMPLETED", completionStep, completedSteps);
            jobDao.addLog(jobId, "INFO", "Job completed. Complied: " + compliedCount + "/" + allResults.size());

        } catch (InterruptedException ie) {
            // Job was aborted
            logger.info("Job {} was aborted", jobId);
            jobDao.updateCompleted(jobId, "ABORTED", null, null, null, null, "Job aborted by user");
            jobDao.addLog(jobId, "WARN", "Job aborted by user");
            Thread.currentThread().interrupt();
            return;
        } catch (Exception e) {
            // Check if the cause was interruption (abort)
            if (Thread.currentThread().isInterrupted() || e.getCause() instanceof InterruptedException) {
                logger.info("Job {} was aborted", jobId);
                jobDao.updateCompleted(jobId, "ABORTED", null, null, null, null, "Job aborted by user");
                jobDao.addLog(jobId, "WARN", "Job aborted by user");
                return;
            }
            logger.error("Job {} failed", jobId, e);
            jobDao.updateCompleted(jobId, "FAILED", null, null, null, null, e.getMessage());
            jobDao.addLog(jobId, "ERROR", "Job failed: " + e.getMessage());

            try {
                Map<String, String> smtpSettings = settingsDao.getSettingsByGroup("SMTP");
                String notifyEmail = resolveRecipients(job);
                EmailService.EmailResult failureMail = emailService.sendReport(smtpSettings, notifyEmail,
                        "LC Reconciliation FAILED - " + (lcNumber != null ? lcNumber : "Job #" + jobId),
                        emailService.buildFailureEmailBody(jobId, lcNumber, e.getMessage()));
                jobDao.updateEmailSent(jobId, failureMail.isSent(), failureMail.isSent() ? null : failureMail.message);
                if (!failureMail.isSent()) {
                    jobDao.addLog(jobId, "WARN", "Failure notification not sent. " + failureMail.message);
                }
            } catch (Exception ex) {
                logger.error("Failed to send failure email", ex);
            }
        }
    }

    /**
     * Who the report goes to.
     *
     * The person who scheduled the job always receives a copy. When an email group is also
     * selected, that group's active members are added on top - so no group means the creator
     * alone, and a group means the group plus the creator even if they are not a member.
     *
     * The submitter's address is looked up from their user record rather than taken from the
     * upload form, so it is authoritative and still correct when an old job is re-run.
     *
     * @return comma-separated recipients, or null when there is nobody to send to
     */
    private String resolveRecipients(ReconciliationJob job) {
        JobDao dao = (JobDao) ctx.getAttribute("jobDao");

        // Keys are lower-cased so casing differences cannot produce a duplicate copy; the map
        // preserves group order and keeps each address in its original form.
        Map<String, String> recipients = new LinkedHashMap<>();

        if (job.getEmailGroupId() > 0) {
            EmailGroupDao emailGroupDao = (EmailGroupDao) ctx.getAttribute("emailGroupDao");
            String groupEmails = emailGroupDao.getEmailsByGroupId(job.getEmailGroupId());
            if (groupEmails != null && !groupEmails.trim().isEmpty()) {
                for (String email : groupEmails.split(",")) {
                    addRecipient(recipients, email);
                }
            } else if (dao != null) {
                dao.addLog(jobId, "WARN", "Email group #" + job.getEmailGroupId() + " has no active members.");
            }
        }

        String submitterEmail = findSubmitterEmail(job.getCreatedBy());
        if (submitterEmail != null) {
            int before = recipients.size();
            addRecipient(recipients, submitterEmail);
            if (dao != null && recipients.size() > before) {
                dao.addLog(jobId, "INFO", "Added job creator " + submitterEmail + " to the recipients.");
            }
        } else if (dao != null && job.getCreatedBy() != null) {
            dao.addLog(jobId, "WARN", "No email address on record for job creator '" + job.getCreatedBy()
                    + "'; they will not receive a copy.");
        }

        if (recipients.isEmpty()) {
            if (dao != null) {
                dao.addLog(jobId, "WARN", "No recipients could be resolved (no email group, and no address "
                        + "on record for the job creator); no notification will be sent.");
            }
            return null;
        }
        return String.join(",", recipients.values());
    }

    private void addRecipient(Map<String, String> recipients, String email) {
        if (email == null) return;
        String trimmed = email.trim();
        if (trimmed.isEmpty()) return;
        recipients.putIfAbsent(trimmed.toLowerCase(), trimmed);
    }

    /** Look up the email address of the user who scheduled the job. */
    private String findSubmitterEmail(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        try {
            UserDao userDao = (UserDao) ctx.getAttribute("userDao");
            if (userDao == null) return null;
            com.smipl.lcrecon.model.User user = userDao.findByUsername(username);
            if (user != null && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                return user.getEmail().trim();
            }
        } catch (Exception e) {
            logger.warn("Could not resolve email for job creator '{}': {}", username, e.getMessage());
        }
        return null;
    }

    /**
     * Stop the job if an abort has been requested. The interrupt flag alone is not enough: HTTP and
     * mail libraries routinely catch InterruptedException and clear it, so the flag held by
     * JobManager is checked as well.
     */
    private void checkAborted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Job aborted by user");
        }
        JobManager jobManager = (JobManager) ctx.getAttribute("jobManager");
        if (jobManager != null && jobManager.isAbortRequested(jobId)) {
            throw new InterruptedException("Job aborted by user");
        }
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    /**
     * Extract InvoiceNo from the invoice_summary JSON string produced during invoice extraction.
     */
    private String extractInvoiceNo(String invoiceSummaryJson) {
        if (invoiceSummaryJson == null || invoiceSummaryJson.trim().isEmpty()) return null;
        try {
            JsonObject obj = JsonParser.parseString(invoiceSummaryJson).getAsJsonObject();
            if (obj.has("InvoiceNo") && !obj.get("InvoiceNo").isJsonNull()) {
                String no = obj.get("InvoiceNo").getAsString().trim();
                return no.isEmpty() ? null : no;
            }
        } catch (Exception e) {
            logger.warn("Could not parse InvoiceNo from invoice_summary: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Fill in LC values for results where LCClauseDescription is missing from GPT response.
     * This happens for non-Invoice docs (PL, BL, COO etc.) whose schemas don't include LCClauseDescription.
     */
    private void fillMissingLcValues(List<ReconciliationResult> results, Map<String, String> lcParams) {
        for (ReconciliationResult r : results) {
            if ((r.getLcValue() == null || r.getLcValue().isEmpty()) && r.getParameterName() != null) {
                String lcVal = lcParams.get(r.getParameterName());
                if (lcVal != null) {
                    r.setLcValue(lcVal);
                }
            }
        }
    }

    private Map<String, String> buildPlaceholders(ReconciliationJob job, String lcNumber, int compliedCount, int totalCount) {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("JOB_ID", String.valueOf(job.getId()));
        p.put("JOB_NAME", job.getJobName() != null ? job.getJobName() : "Job #" + job.getId());
        p.put("LC_NUMBER", lcNumber != null ? lcNumber : "N/A");
        p.put("SHIPMENT_DOC_NAME", job.getShipmentDocName() != null ? job.getShipmentDocName() : "N/A");
        p.put("STATUS", "COMPLETED");
        p.put("COMPLIED_COUNT", String.valueOf(compliedCount));
        p.put("NON_COMPLIED_COUNT", String.valueOf(totalCount - compliedCount));
        p.put("TOTAL_COUNT", String.valueOf(totalCount));
        p.put("START_DATE", job.getStartedAt() != null ? job.getStartedAt().toString() : "N/A");
        p.put("END_DATE", new java.util.Date().toString());
        p.put("CREATED_BY", job.getCreatedBy() != null ? job.getCreatedBy() : "N/A");
        return p;
    }
}
