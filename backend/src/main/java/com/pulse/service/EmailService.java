package com.pulse.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPriceAlertEmail(String toEmail, String symbol, String companyName, double targetPrice, double currentPrice, String condition) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject = "Pulse Alert: " + symbol + " has reached your target!";
            String html = buildEmailHtml(symbol, companyName, targetPrice, currentPrice, condition);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            javaMailSender.send(message);
            System.out.println("Email sent successfully via Gmail SMTP to: " + toEmail);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildEmailHtml(String symbol, String companyName, double targetPrice, double currentPrice, String condition) {
        String trendColor = currentPrice >= targetPrice ? "#10b981" : "#ef4444";
        String statusText = currentPrice >= targetPrice ? "CONDITION MET" : "CONDITION MET"; // Alert triggered

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
                "            <td style=\"padding:8px 0;text-align:right;color:#ffffff;font-size:14px;font-weight:600;border-bottom:1px solid #222222;\">$" + String.format("%.2f", currentPrice) + "</td>" +
                "          </tr>" +
                "          <tr>" +
                "            <td style=\"padding:8px 0;color:#888888;font-size:12px;\">Target (" + condition + ")</td>" +
                "            <td style=\"padding:8px 0;text-align:right;color:#888888;font-size:12px;font-weight:500;\">$" + String.format("%.2f", targetPrice) + "</td>" +
                "          </tr>" +
                "        </table>" +
                "      </div>" +
                "      <a href=\"http://localhost:5173\" style=\"display:block;width:100%;text-align:center;background:#ffffff;color:#000000;text-decoration:none;padding:10px 0;border-radius:4px;font-weight:600;font-size:12px;transition:background 0.2s;\">View Dashboard (Local)</a>" +
                "    </div>" +
                "  </div>" +
                "</div>";
    }
}
