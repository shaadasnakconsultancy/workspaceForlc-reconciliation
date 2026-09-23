package com.smipl.lcrecon.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Map;
import java.util.Properties;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    /** Setting key that switches all outbound email on/off (Settings -> SMTP tab). */
    public static final String KEY_ENABLED = "smtp_enabled";
    /** Setting key for the socket timeout, in seconds, applied to connect/read/write. */
    public static final String KEY_TIMEOUT = "smtp_timeout_seconds";
    /** Used when smtp_timeout_seconds is absent or unparseable. */
    private static final int DEFAULT_TIMEOUT_SECONDS = 20;

    /** Outcome of a send attempt. Never thrown - callers inspect this and carry on. */
    public static class EmailResult {
        public static final String SENT = "SENT";
        public static final String DISABLED = "DISABLED";
        public static final String NO_RECIPIENT = "NO_RECIPIENT";
        public static final String FAILED = "FAILED";

        public final String status;
        public final String message;

        private EmailResult(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public boolean isSent() { return SENT.equals(status); }
        /** True when the send was deliberately skipped rather than attempted and failed. */
        public boolean isSkipped() { return DISABLED.equals(status) || NO_RECIPIENT.equals(status); }

        static EmailResult sent() { return new EmailResult(SENT, ""); }
        static EmailResult disabled() {
            return new EmailResult(DISABLED, "Email sending is disabled in Settings > SMTP.");
        }
        static EmailResult noRecipient() {
            return new EmailResult(NO_RECIPIENT, "No recipient could be resolved for this job - no email group was "
                    + "selected and the job creator has no email address on record.");
        }
        static EmailResult failed(String message) { return new EmailResult(FAILED, message); }
    }

    /**
     * True when outbound email is switched on. A missing key counts as enabled so that
     * installations upgraded from an earlier build keep their existing behaviour.
     */
    public static boolean isEnabled(Map<String, String> smtpSettings) {
        if (smtpSettings == null) return false;
        return !"false".equalsIgnoreCase(smtpSettings.getOrDefault(KEY_ENABLED, "true"));
    }

    static int timeoutMillis(Map<String, String> smtpSettings) {
        int seconds = DEFAULT_TIMEOUT_SECONDS;
        if (smtpSettings != null) {
            try {
                int configured = Integer.parseInt(smtpSettings.getOrDefault(KEY_TIMEOUT, "").trim());
                if (configured > 0) seconds = configured;
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return seconds * 1000;
    }

    /**
     * Apply connect/read/write timeouts. Without these JavaMail waits forever, which parks the
     * calling thread permanently when the SMTP host accepts the TCP connection but never replies.
     */
    static void applyTimeouts(Properties props, int timeoutMs) {
        props.put("mail.smtp.connectiontimeout", String.valueOf(timeoutMs));
        props.put("mail.smtp.timeout", String.valueOf(timeoutMs));
        props.put("mail.smtp.writetimeout", String.valueOf(timeoutMs));
    }

    /**
     * Turn a JavaMail failure into a message an operator can act on. The raw exception text is
     * appended so nothing is lost, but the leading sentence names the actual problem.
     */
    static String describeFailure(Map<String, String> smtpSettings, Exception e) {
        String host = smtpSettings != null ? smtpSettings.getOrDefault("smtp_host", "") : "";
        String port = smtpSettings != null ? smtpSettings.getOrDefault("smtp_port", "587") : "587";
        String endpoint = host + ":" + port;
        int seconds = timeoutMillis(smtpSettings) / 1000;

        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String detail = root.getMessage() != null ? root.getMessage() : root.getClass().getSimpleName();

        if (e instanceof AuthenticationFailedException) {
            return "SMTP authentication rejected by " + endpoint
                    + " - check the username/password, and confirm Authenticated SMTP is enabled on the mailbox. ("
                    + detail + ")";
        }
        if (root instanceof java.net.SocketTimeoutException) {
            return "SMTP timed out after " + seconds + "s talking to " + endpoint
                    + " - the host accepted the connection but did not respond. Check the firewall and SMTP host name. ("
                    + detail + ")";
        }
        if (root instanceof java.net.UnknownHostException) {
            return "SMTP host '" + host + "' could not be resolved by DNS. (" + detail + ")";
        }
        if (root instanceof java.net.ConnectException || root instanceof java.net.NoRouteToHostException) {
            return "Could not connect to " + endpoint
                    + " within " + seconds + "s - outbound port likely blocked by the firewall. (" + detail + ")";
        }
        if (root instanceof javax.net.ssl.SSLException) {
            return "TLS handshake with " + endpoint + " failed - check the TLS setting or TLS inspection on the network. ("
                    + detail + ")";
        }
        if (host == null || host.trim().isEmpty()) {
            return "SMTP host is not configured in Settings > SMTP. (" + detail + ")";
        }
        return "Email to " + endpoint + " failed: " + detail;
    }

    /**
     * Open an authenticated SMTP session and close it again, without sending anything. Used by the
     * Test Connection button, so it deliberately ignores {@link #KEY_ENABLED} - an administrator
     * needs to be able to verify the settings before switching email on.
     */
    public EmailResult testConnection(Map<String, String> smtpSettings) {
        String host = smtpSettings.getOrDefault("smtp_host", "").trim();
        if (host.isEmpty()) {
            return EmailResult.failed("SMTP host is not set.");
        }

        Transport transport = null;
        try {
            int port = Integer.parseInt(smtpSettings.getOrDefault("smtp_port", "587").trim());

            Properties props = new Properties();
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            props.put("mail.smtp.auth", "true");
            applyTimeouts(props, timeoutMillis(smtpSettings));
            if ("true".equals(smtpSettings.getOrDefault("smtp_tls_enabled", "true"))) {
                props.put("mail.smtp.starttls.enable", "true");
            }

            Session session = Session.getInstance(props);
            transport = session.getTransport("smtp");
            transport.connect(host, port,
                    smtpSettings.getOrDefault("smtp_username", ""),
                    smtpSettings.getOrDefault("smtp_password", ""));
            return EmailResult.sent();
        } catch (NumberFormatException e) {
            return EmailResult.failed("SMTP port must be a number.");
        } catch (Exception e) {
            return EmailResult.failed(describeFailure(smtpSettings, e));
        } finally {
            if (transport != null) {
                try { transport.close(); } catch (Exception ignored) { /* nothing useful to do */ }
            }
        }
    }

    /**
     * Send the reconciliation report. Returns the outcome instead of throwing, so a mail problem
     * never prevents a job from completing.
     */
    public EmailResult sendReport(Map<String, String> smtpSettings, String toAddress,
                                  String subject, String htmlBody, File... attachments) {
        if (!isEnabled(smtpSettings)) {
            logger.info("Email disabled in settings; skipping report mail to: {}", toAddress);
            return EmailResult.disabled();
        }
        if (toAddress == null || toAddress.trim().isEmpty()) {
            return EmailResult.noRecipient();
        }

        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpSettings.getOrDefault("smtp_host", ""));
            props.put("mail.smtp.port", smtpSettings.getOrDefault("smtp_port", "587"));
            props.put("mail.smtp.auth", "true");
            applyTimeouts(props, timeoutMillis(smtpSettings));

            boolean tlsEnabled = "true".equals(smtpSettings.getOrDefault("smtp_tls_enabled", "true"));
            if (tlsEnabled) {
                props.put("mail.smtp.starttls.enable", "true");
            }

            String username = smtpSettings.getOrDefault("smtp_username", "");
            String password = smtpSettings.getOrDefault("smtp_password", "");
            String fromAddress = smtpSettings.getOrDefault("smtp_from", username);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);

            // Create multipart
            MimeMultipart multipart = new MimeMultipart();

            // HTML body
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            // Attachments
            for (File attachment : attachments) {
                if (attachment != null && attachment.exists()) {
                    MimeBodyPart attachPart = new MimeBodyPart();
                    attachPart.setDataHandler(new DataHandler(new FileDataSource(attachment)));
                    attachPart.setFileName(attachment.getName());
                    multipart.addBodyPart(attachPart);
                }
            }

            message.setContent(multipart);
            Transport.send(message);
            logger.info("Email sent successfully to: {}", toAddress);
            return EmailResult.sent();
        } catch (Exception e) {
            String reason = describeFailure(smtpSettings, e);
            logger.error("Failed to send email to {}: {}", toAddress, reason, e);
            return EmailResult.failed(reason);
        }
    }

    public String buildSuccessEmailBody(long jobId, String lcNumber, int matchedCount, int totalCount) {
        return "<html><body style='font-family:Arial,sans-serif'>" +
                "<h2>LC Reconciliation Complete</h2>" +
                "<table style='border-collapse:collapse'>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>Job ID:</td><td style='padding:4px 12px'>" + jobId + "</td></tr>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>LC Number:</td><td style='padding:4px 12px'>" + lcNumber + "</td></tr>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>Matched:</td><td style='padding:4px 12px'>" + matchedCount + " / " + totalCount + "</td></tr>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>Non-Compliant:</td><td style='padding:4px 12px'>" + (totalCount - matchedCount) + "</td></tr>" +
                "</table>" +
                "<p>Please find the reconciliation reports attached.</p>" +
                "</body></html>";
    }

    public String buildFailureEmailBody(long jobId, String lcNumber, String errorMessage) {
        return "<html><body style='font-family:Arial,sans-serif'>" +
                "<h2 style='color:#c00'>LC Reconciliation Failed</h2>" +
                "<table style='border-collapse:collapse'>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>Job ID:</td><td style='padding:4px 12px'>" + jobId + "</td></tr>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>LC Number:</td><td style='padding:4px 12px'>" + (lcNumber != null ? lcNumber : "N/A") + "</td></tr>" +
                "<tr><td style='padding:4px 12px;font-weight:bold'>Error:</td><td style='padding:4px 12px;color:#c00'>" + errorMessage + "</td></tr>" +
                "</table>" +
                "<p>Please check the Job Monitor for more details.</p>" +
                "</body></html>";
    }
}
