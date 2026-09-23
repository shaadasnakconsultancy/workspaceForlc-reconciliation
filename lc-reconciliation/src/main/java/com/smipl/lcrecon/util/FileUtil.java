package com.smipl.lcrecon.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    private static String uploadBaseDir;
    private static String reportBaseDir;

    public static void initialize(String catalinaBase) {
        try {
            Properties props = new Properties();
            InputStream is = FileUtil.class.getClassLoader().getResourceAsStream("application.properties");
            if (is != null) {
                props.load(is);
                is.close();
            }

            uploadBaseDir = props.getProperty("upload.base.dir", catalinaBase + "/lc-uploads")
                    .replace("${catalina.base}", catalinaBase);
            reportBaseDir = props.getProperty("report.base.dir", catalinaBase + "/lc-reports")
                    .replace("${catalina.base}", catalinaBase);

            new File(uploadBaseDir).mkdirs();
            new File(reportBaseDir).mkdirs();
            logger.info("File storage initialized: uploads={}, reports={}", uploadBaseDir, reportBaseDir);
        } catch (Exception e) {
            logger.error("Failed to initialize file storage", e);
        }
    }

    public static String getUploadBaseDir() {
        return uploadBaseDir;
    }

    public static String getReportBaseDir() {
        return reportBaseDir;
    }

    public static String saveFile(InputStream inputStream, String subDir, String fileName) throws IOException {
        Path dirPath = Paths.get(uploadBaseDir, subDir);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(fileName);
        Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    public static byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    public static String saveReport(String content, String fileName) throws IOException {
        Path dirPath = Paths.get(reportBaseDir);
        Files.createDirectories(dirPath);
        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, content.getBytes("UTF-8"));
        return filePath.toString();
    }

    public static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
