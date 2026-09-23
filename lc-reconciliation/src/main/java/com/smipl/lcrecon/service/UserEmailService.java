package com.smipl.lcrecon.service;

import com.smipl.lcrecon.dao.SettingsDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Map;
import java.util.Properties;

/**
 * Sends user-related notification emails (account creation, password reset).
 */
public class UserEmailService {
    private static final Logger logger = LoggerFactory.getLogger(UserEmailService.class);

    public static void sendNewUserEmail(Map<String, String> smtpSettings, String toEmail,
                                         String firstName, String username, String password) {
        String subject = "LC Reconciliation System - Your Account Has Been Created";
        String body = "<html><body style='font-family:Arial,sans-serif;font-size:14px;color:#333;line-height:1.8'>"
                + "<h2 style='color:#0d6efd'>Welcome to LC Reconciliation System</h2>"
                + "<p>Dear " + esc(firstName) + ",</p>"
                + "<p>Your account has been created on the LC Reconciliation System. Please find your login details below:</p>"
                + "<table style='border-collapse:collapse;margin:15px 0'>"
                + "<tr><td style='padding:8px 15px;font-weight:bold;background:#f8f9fa;border:1px solid #dee2e6'>Username</td>"
                + "<td style='padding:8px 15px;border:1px solid #dee2e6'>" + esc(username) + "</td></tr>"
                + "<tr><td style='padding:8px 15px;font-weight:bold;background:#f8f9fa;border:1px solid #dee2e6'>Password</td>"
                + "<td style='padding:8px 15px;border:1px solid #dee2e6'>" + esc(password) + "</td></tr>"
                + "</table>"
                + "<p style='color:#dc3545'><strong>Important:</strong> Please change your password after first login.</p>"
                + "<p>Regards,<br>LC Reconciliation System</p>"
                + "<hr style='border:none;border-top:1px solid #dee2e6'>"
                + "<p style='font-size:12px;color:#6c757d'>This is a system generated email. Do not reply.</p>"
                + "</body></html>";
        sendEmail(smtpSettings, toEmail, subject, body);
    }

    public static void sendPasswordResetByAdminEmail(Map<String, String> smtpSettings, String toEmail,
                                                      String firstName, String username, String newPassword,
                                                      String adminName) {
        String subject = "LC Reconciliation System - Your Password Has Been Reset";
        String body = "<html><body style='font-family:Arial,sans-serif;font-size:14px;color:#333;line-height:1.8'>"
                + "<h2 style='color:#ffc107'>Password Reset Notification</h2>"
                + "<p>Dear " + esc(firstName) + ",</p>"
                + "<p>Your password has been reset by the administrator (<strong>" + esc(adminName) + "</strong>). Your new login details are:</p>"
                + "<table style='border-collapse:collapse;margin:15px 0'>"
                + "<tr><td style='padding:8px 15px;font-weight:bold;background:#f8f9fa;border:1px solid #dee2e6'>Username</td>"
                + "<td style='padding:8px 15px;border:1px solid #dee2e6'>" + esc(username) + "</td></tr>"
                + "<tr><td style='padding:8px 15px;font-weight:bold;background:#f8f9fa;border:1px solid #dee2e6'>New Password</td>"
                + "<td style='padding:8px 15px;border:1px solid #dee2e6'>" + esc(newPassword) + "</td></tr>"
                + "</table>"
                + "<p style='color:#dc3545'><strong>Important:</strong> Please change your password immediately after login.</p>"
                + "<p>Regards,<br>LC Reconciliation System</p>"
                + "<hr style='border:none;border-top:1px solid #dee2e6'>"
                + "<p style='font-size:12px;color:#6c757d'>This is a system generated email. Do not reply.</p>"
                + "</body></html>";
        sendEmail(smtpSettings, toEmail, subject, body);
    }

    public static void sendSelfPasswordResetEmail(Map<String, String> smtpSettings, String toEmail,
                                                    String firstName, String username) {
        String subject = "LC Reconciliation System - Password Changed Successfully";
        String body = "<html><body style='font-family:Arial,sans-serif;font-size:14px;color:#333;line-height:1.8'>"
                + "<h2 style='color:#198754'>Password Changed</h2>"
                + "<p>Dear " + esc(firstName) + ",</p>"
                + "<p>Your password has been changed successfully for username: <strong>" + esc(username) + "</strong></p>"
                + "<p>If you did not make this change, please contact your administrator immediately.</p>"
                + "<p>Regards,<br>LC Reconciliation System</p>"
                + "<hr style='border:none;border-top:1px solid #dee2e6'>"
                + "<p style='font-size:12px;color:#6c757d'>This is a system generated email. Do not reply.</p>"
                + "</body></html>";
        sendEmail(smtpSettings, toEmail, subject, body);
    }

    public static void sendForgotPasswordEmail(Map<String, String> smtpSettings, String toEmail,
                                                String firstName, String username, String newPassword) {
        String subject = "LC Reconciliation System - Password Reset";
        String body = "<html><body style='font-family:Arial,sans-serif;font-size:14px;color:#333;line-height:1.8'>"
                + "<h2 style='color:#0dcaf0'>Password Reset</h2>"
                + "<p>Dear " + esc(firstName) + ",</p>"
                + "<p>Your password has been reset as requested. Your new login details are:</p>"
                + "<table style='border-collapse:collapse;margin:15px 0'>"
                + "<tr><td style='padding:8px 15px;font-weight:bold;background:#f8f9fa;border:1px solid #dee2e6'>Username</td>"
                + "<td style='padding:8px 15px;border:1px solid #dee2e6'>" + esc(username) + "</td></tr>"
                + "<tr><td style='padding:8px 15px;font-weight:bold;background:#f8f9fa;border:1px solid #dee2e6'>New Password</td>"
                + "<td style='padding:8px 15px;border:1px solid #dee2e6'>" + esc(newPassword) + "</td></tr>"
                + "</table>"
                + "<p style='color:#dc3545'><strong>Important:</strong> Please change your password immediately after login.</p>"
                + "<p>Regards,<br>LC Reconciliation System</p>"
                + "<hr style='border:none;border-top:1px solid #dee2e6'>"
                + "<p style='font-size:12px;color:#6c757d'>This is a system generated email. Do not reply.</p>"
                + "</body></html>";
        sendEmail(smtpSettings, toEmail, subject, body);
    }

    /**
     * Sends a user notification. Returns false when email is switched off or the send failed -
     * user creation / password reset must succeed regardless of the mail server's state.
     */
    private static boolean sendEmail(Map<String, String> smtpSettings, String toEmail, String subject, String htmlBody) {
        if (!EmailService.isEnabled(smtpSettings)) {
            logger.info("Email disabled in settings; skipping '{}' mail to {}", subject, toEmail);
            return false;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpSettings.getOrDefault("smtp_host", ""));
            props.put("mail.smtp.port", smtpSettings.getOrDefault("smtp_port", "587"));
            props.put("mail.smtp.auth", "true");
            EmailService.applyTimeouts(props, EmailService.timeoutMillis(smtpSettings));
            if ("true".equals(smtpSettings.getOrDefault("smtp_tls_enabled", "true"))) {
                props.put("mail.smtp.starttls.enable", "true");
            }

            String username = smtpSettings.getOrDefault("smtp_username", "");
            String password = smtpSettings.getOrDefault("smtp_password", "");
            String fromAddress = smtpSettings.getOrDefault("smtp_from", username);

            Session session = Session.getInstance(props, new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
            logger.info("User email sent to: {} subject: {}", toEmail, subject);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send user email to {}: {}", toEmail,
                    EmailService.describeFailure(smtpSettings, e));
            return false;
        }
    }

    private static String esc(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
