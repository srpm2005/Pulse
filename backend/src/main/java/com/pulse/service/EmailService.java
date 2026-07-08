package com.pulse.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from-email}")
    private String fromEmail;

    public void sendPriceAlertEmail(String toEmail, String symbol, String companyName, double targetPrice, double currentPrice, String condition) {
        try {
            Resend resend = new Resend(resendApiKey);

            String subject = "Pulse Alert: " + symbol + " has reached your target!";
            String html = buildEmailHtml(symbol, companyName, targetPrice, currentPrice, condition);

            CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                    .from(fromEmail)
                    .to(toEmail)
                    .subject(subject)
                    .html(html)
                    .build();

            CreateEmailResponse data = resend.emails().send(sendEmailRequest);
            System.out.println("Email sent successfully: " + data.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String buildEmailHtml(String symbol, String companyName, double targetPrice, double currentPrice, String condition) {
        return "<div style=\"font-family: Arial, sans-serif; background-color: #0d0f14; color: #f1f5f9; padding: 20px; border-radius: 8px;\">" +
               "<h2 style=\"color: #6366f1;\">Price Alert Triggered!</h2>" +
               "<p>Your alert for <strong>" + (companyName != null ? companyName : symbol) + " (" + symbol + ")</strong> has been triggered.</p>" +
               "<p>Target Condition: " + condition + " <strong>" + targetPrice + "</strong></p>" +
               "<p>Current Price: <strong style=\"color: #22c55e;\">" + currentPrice + "</strong></p>" +
               "<hr style=\"border: 1px solid #2a2f3e; margin: 20px 0;\" />" +
               "<p style=\"color: #94a3b8; font-size: 12px;\">Sent via Pulse Stock Dashboard</p>" +
               "</div>";
    }
}
