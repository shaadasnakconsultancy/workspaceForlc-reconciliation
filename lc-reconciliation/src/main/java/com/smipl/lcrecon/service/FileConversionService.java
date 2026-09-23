package com.smipl.lcrecon.service;

import com.smipl.lcrecon.integration.GraphApiClient;
import com.smipl.lcrecon.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Converts non-PDF files to PDF using Microsoft Graph API (OneDrive).
 * Supported formats: .docx, .doc, .xlsx, .xlsm, .xls, .msg, .pptx, .ppt
 */
public class FileConversionService {
    private static final Logger logger = LoggerFactory.getLogger(FileConversionService.class);

    private static final Set<String> CONVERTIBLE_EXTENSIONS = new HashSet<>(Arrays.asList(
            "docx", "doc", "xlsx", "xlsm", "xls", "msg", "pptx", "ppt", "rtf", "odt", "ods"
    ));

    /**
     * Convert file to PDF if it's not already a PDF.
     * @param filePath   Path to the original file
     * @param graphSettings  Graph API settings from DB
     * @return Path to PDF file (original path if already PDF, or converted file path)
     */
    public String convertToPdfIfNeeded(String filePath, Map<String, String> graphSettings) throws Exception {
        String extension = FileUtil.getFileExtension(filePath);

        // Already PDF - no conversion needed
        if ("pdf".equalsIgnoreCase(extension)) {
            return filePath;
        }

        // Check if this format is convertible
        if (!CONVERTIBLE_EXTENSIONS.contains(extension.toLowerCase())) {
            logger.warn("Unsupported format for conversion: .{} - sending as-is to OCR", extension);
            return filePath;
        }

        // Check if Graph API is configured
        GraphApiClient graphClient = new GraphApiClient(graphSettings);
        if (!graphClient.isConfigured()) {
            logger.warn("Graph API not configured. Cannot convert .{} file. Sending as-is to OCR.", extension);
            return filePath;
        }

        // Read original file
        Path originalPath = Paths.get(filePath);
        byte[] fileBytes = Files.readAllBytes(originalPath);
        String fileName = originalPath.getFileName().toString();

        logger.info("Converting {} (.{}) to PDF via Graph API...", fileName, extension);

        // Convert via OneDrive
        byte[] pdfBytes = graphClient.convertToPdf(fileBytes, fileName);

        // Save converted PDF alongside original
        String pdfFileName = fileName.substring(0, fileName.lastIndexOf('.')) + "_converted.pdf";
        Path pdfPath = originalPath.getParent().resolve(pdfFileName);
        Files.write(pdfPath, pdfBytes);

        logger.info("Conversion complete: {} -> {} ({} bytes)", fileName, pdfFileName, pdfBytes.length);
        return pdfPath.toString();
    }

    /**
     * Check if a file needs conversion (non-PDF).
     */
    public boolean needsConversion(String filePath) {
        String ext = FileUtil.getFileExtension(filePath);
        return !"pdf".equalsIgnoreCase(ext) && CONVERTIBLE_EXTENSIONS.contains(ext.toLowerCase());
    }
}
