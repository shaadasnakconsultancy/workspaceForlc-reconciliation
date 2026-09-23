package com.smipl.lcrecon.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Open-source extraction of HSN codes from the uploaded Invoice document, from page 2 onward
 * (where the importer part-wise HSN list appears). Uses Apache PDFBox to read embedded text -
 * no Azure OCR, so it is fast and free.
 *
 * Page 1 is deliberately skipped: it carries invoice/PO numbers and dates that can also be
 * 8-digit tokens and would be picked up as false HSN codes.
 *
 * NOTE: PDFBox reads embedded text; it works on digitally generated invoice PDFs. A scanned
 * (image-only) invoice yields no text and therefore no HSN codes (logged as a warning).
 */
public class InvoiceHsnExtractor {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceHsnExtractor.class);

    // HSN codes are 8-digit tariff codes; match as distinct tokens (not embedded in longer numbers)
    private static final Pattern HSN_TOKEN = Pattern.compile("(?<!\\d)(\\d{8})(?!\\d)");

    /**
     * Extract distinct 8-digit HSN codes from the invoice PDF starting at page 2.
     *
     * @param pdfPath path to the (already-PDF) invoice file
     * @return distinct HSN codes found on page 2+ (empty if none / not a text PDF)
     */
    public Set<String> extractHsnCodes(String pdfPath) {
        Set<String> codes = new LinkedHashSet<>();
        if (pdfPath == null || pdfPath.trim().isEmpty()) return codes;

        File file = new File(pdfPath);
        if (!file.exists()) {
            logger.warn("Invoice PDF not found for HSN extraction: {}", pdfPath);
            return codes;
        }

        try (PDDocument doc = PDDocument.load(file)) {
            int pageCount = doc.getNumberOfPages();
            if (pageCount < 2) {
                logger.warn("Invoice has only {} page(s); no page 2+ HSN list to extract from", pageCount);
                return codes;
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(2);              // page 2 onward
            stripper.setEndPage(pageCount);
            String text = stripper.getText(doc);

            if (text == null || text.trim().isEmpty()) {
                logger.warn("No embedded text on invoice page 2+ (possibly a scanned/image PDF). "
                        + "HSN extraction returned nothing for {}", pdfPath);
                return codes;
            }

            Matcher m = HSN_TOKEN.matcher(text);
            while (m.find()) {
                codes.add(m.group(1));
            }
            logger.info("Extracted {} distinct HSN code(s) from invoice page 2+ ({} pages scanned)",
                    codes.size(), pageCount - 1);
        } catch (Exception e) {
            logger.error("Failed to extract HSN codes from invoice PDF {}: {}", pdfPath, e.getMessage());
        }
        return codes;
    }
}
