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
        String statusText = currentPrice >= targetPrice ? "Target Reached \uD83D\uDFE2" : "Dropped Below \uD83D\uDD34";

        return "<div style=\"margin:0;padding:20px;background-color:#0b0f19;font-family:'Helvetica Neue', Helvetica, Arial, sans-serif;color:#f8fafc;\">" +
                "  <div style=\"max-width:600px;margin:0 auto;background:#131b2c;border:1px solid #1e293b;border-radius:12px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.5);\">" +
                "    <div style=\"background: linear-gradient(90deg, #6366f1 0%, #a855f7 100%);padding:25px;text-align:center;\">" +
                "      <h1 style=\"margin:0;color:#ffffff;font-size:24px;letter-spacing:1px;font-weight:700;\">PULSE MARKET ALERT</h1>" +
                "    </div>" +
                "    <div style=\"padding:35px 30px;\">" +
                "      <h2 style=\"margin:0 0 10px 0;font-size:22px;color:#e2e8f0;\">" + (companyName != null ? companyName : symbol) + "</h2>" +
                "      <p style=\"margin:0 0 25px 0;color:#94a3b8;font-size:15px;letter-spacing:1px;\">Ticker: <strong>" + symbol + "</strong></p>" +
                "      <div style=\"background:#0f1523;border-radius:8px;padding:20px;border-left:4px solid " + trendColor + ";margin-bottom:30px;\">" +
                "        <h3 style=\"margin:0 0 15px 0;font-size:18px;color:#cbd5e1;\">" + statusText + "</h3>" +
                "        <table style=\"width:100%;border-collapse:collapse;\">" +
                "          <tr>" +
                "            <td style=\"padding:8px 0;color:#64748b;font-size:14px;\">Current Price</td>" +
                "            <td style=\"padding:8px 0;text-align:right;color:" + trendColor + ";font-size:18px;font-weight:bold;\">$" + String.format("%.2f", currentPrice) + "</td>" +
                "          </tr>" +
                "          <tr>" +
                "            <td style=\"padding:8px 0;border-top:1px solid #1e293b;color:#64748b;font-size:14px;\">Target Goal (" + condition + ")</td>" +
                "            <td style=\"padding:8px 0;border-top:1px solid #1e293b;text-align:right;color:#e2e8f0;font-size:16px;font-weight:600;\">$" + String.format("%.2f", targetPrice) + "</td>" +
                "          </tr>" +
                "        </table>" +
                "      </div>" +
                "      <a href=\"http://localhost:3000\" style=\"display:block;width:100%;text-align:center;background:#6366f1;color:#ffffff;text-decoration:none;padding:14px 0;border-radius:6px;font-weight:600;font-size:15px;letter-spacing:0.5px;transition:background 0.3s ease;\">View Your Dashboard</a>" +
                "    </div>" +
                "    <div style=\"background:#0d121f;padding:20px;text-align:center;border-top:1px solid #1e293b;\">" +
                "      <p style=\"margin:0;color:#475569;font-size:12px;\">&copy; 2026 Pulse Analytics. Crafted with precision.</p>" +
                "    </div>" +
                "  </div>" +
                "</div>";
    }
}
