package com.pulse.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPriceAlertEmail(String toEmail, String symbol, String companyName, double targetPrice, double currentPrice, String condition, String currency) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Pulse Alert: " + symbol + " has reached your target!";
            String html = buildEmailHtml(symbol, companyName, targetPrice, currentPrice, condition, currency);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            javaMailSender.send(message);
            log.info("Email sent successfully via Gmail SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send price alert email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Pulse: Password Reset Verification Code";
            String html = buildPasswordResetHtml(resetCode);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            javaMailSender.send(message);
            log.info("Password reset email sent successfully via Gmail SMTP to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildPasswordResetHtml(String resetCode) {
        return "<div style=\"margin:0;padding:40px 20px;background-color:#000000;font-family:-apple-system, BlinkMacSystemFont, 'Inter', sans-serif;color:#ffffff;\">" +
                "  <div style=\"max-width:500px;margin:0 auto;background:#0a0a0a;border:1px solid #222222;border-radius:4px;overflow:hidden;\">" +
                "    <div style=\"padding:16px 20px;border-bottom:1px solid #222222;\">" +
                "      <span style=\"margin:0;color:#888888;font-size:11px;font-weight:600;letter-spacing:0.5px;text-transform:uppercase;\">PULSE // SECURITY VERIFICATION</span>" +
                "    </div>" +
                "    <div style=\"padding:24px 20px;\">" +
                "      <h2 style=\"margin:0 0 8px 0;font-size:18px;font-weight:600;color:#ffffff;\">Password Reset Request</h2>" +
                "      <p style=\"margin:0 0 20px 0;color:#888888;font-size:13px;line-height:1.5;\">We received a request to reset your Pulse account password. Use the verification code below to set a new password:</p>" +
                "      <div style=\"background:#111111;border:1px dashed #444444;border-radius:4px;padding:20px;text-align:center;margin:20px 0;\">" +
                "        <div style=\"font-size:10px;color:#888888;text-transform:uppercase;letter-spacing:1px;margin-bottom:8px;font-weight:600;\">6-Digit Verification Code</div>" +
                "        <div style=\"font-size:32px;font-weight:700;letter-spacing:10px;color:#ffffff;font-family:ui-monospace, Consolas, monospace;\">" + resetCode + "</div>" +
                "      </div>" +
                "      <p style=\"margin:0;color:#666666;font-size:11px;line-height:1.4;\">This code is valid for <strong>15 minutes</strong>. If you did not request a password reset, please ignore this email.</p>" +
                "    </div>" +
                "  </div>" +
                "</div>";
    }

    private String buildEmailHtml(String symbol, String companyName, double targetPrice, double currentPrice, String condition, String currencyCode) {
        String trendColor = currentPrice >= targetPrice ? "#10b981" : "#ef4444";
        String statusText = currentPrice >= targetPrice ? "CONDITION MET" : "CONDITION MET"; // Alert triggered
        
        String currentFormatted = formatCurrency(currentPrice, currencyCode);
        String targetFormatted = formatCurrency(targetPrice, currencyCode);

        return "<div style=\"margin:0;padding:40px 20px;background-color:#000000;font-family:-apple-system, BlinkMacSystemFont, 'Inter', sans-serif;color:#ffffff;\">" +
                "  <div style=\"max-width:500px;margin:0 auto;background:#0a0a0a;border:1px solid #222222;border-radius:4px;overflow:hidden;\">" +
                "    <div style=\"padding:16px 20px;border-bottom:1px solid #222222;display:flex;justify-content:space-between;align-items:center;\">" +
                "      <span style=\"margin:0;color:#888888;font-size:11px;font-weight:600;letter-spacing:0.5px;text-transform:uppercase;\">PULSE // SYSTEM ALERT</span>" +
                "    </div>" +
                "    <div style=\"padding:24px 20px;\">" +
                "      <div style=\"margin-bottom:24px;\">" +
                "        <h2 style=\"margin:0 0 4px 0;font-size:16px;font-weight:600;color:#ffffff;\">" + symbol + "</h2>" +
                "        <span style=\"color:#555555;font-size:11px;\">" + (companyName != null ? companyName : symbol) + "</span>" +
                "      </div>" +
                "      <div style=\"background:#000000;border:1px solid #222222;border-radius:4px;padding:16px;margin-bottom:24px;\">" +
                "        <div style=\"font-size:10px;color:" + trendColor + ";font-weight:600;text-transform:uppercase;margin-bottom:12px;letter-spacing:0.5px;\">STATUS: " + statusText + "</div>" +
                "        <table style=\"width:100%;border-collapse:collapse;font-variant-numeric:tabular-nums;\">" +
                "          <tr>" +
                "            <td style=\"padding:8px 0;color:#888888;font-size:12px;border-bottom:1px solid #222222;\">Current Price</td>" +
                "            <td style=\"padding:8px 0;text-align:right;color:#ffffff;font-size:14px;font-weight:600;border-bottom:1px solid #222222;\">" + currentFormatted + "</td>" +
                "          </tr>" +
                "          <tr>" +
                "            <td style=\"padding:8px 0;color:#888888;font-size:12px;\">Target (" + condition + ")</td>" +
                "            <td style=\"padding:8px 0;text-align:right;color:#888888;font-size:12px;font-weight:500;\">" + targetFormatted + "</td>" +
                "          </tr>" +
                "        </table>" +
                "      </div>" +
                "      <a href=\"http://localhost:5173\" style=\"display:block;width:100%;text-align:center;background:#ffffff;color:#000000;text-decoration:none;padding:10px 0;border-radius:4px;font-weight:600;font-size:12px;transition:background 0.2s;\">View Dashboard (Local)</a>" +
                "    </div>" +
                "  </div>" +
                "</div>";
    }

    private String formatCurrency(double amount, String currencyCode) {
        if (currencyCode == null) currencyCode = "USD";
        if (currencyCode.equals("INR")) return "₹" + String.format("%.2f", amount);
        if (currencyCode.equals("EUR")) return "€" + String.format("%.2f", amount);
        if (currencyCode.equals("GBP")) return "£" + String.format("%.2f", amount);
        if (currencyCode.equals("JPY")) return "¥" + String.format("%.2f", amount);
        return "$" + String.format("%.2f", amount);
    }
}
