package com.pulse.service;

import com.pulse.dto.AlertRequest;
import com.pulse.dto.StockQuoteDto;
import com.pulse.entity.AlertCondition;
import com.pulse.entity.PriceAlert;
import com.pulse.entity.User;
import com.pulse.repository.PriceAlertRepository;
import com.pulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final PriceAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final YahooFinanceService yahooFinanceService;
    private final EmailService emailService;

    public PriceAlert createAlert(AlertRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        PriceAlert alert = PriceAlert.builder()
                .user(user)
                .symbol(request.getSymbol())
                .companyName(request.getCompanyName())
                .targetPrice(request.getTargetPrice())
                .condition(request.getCondition())
                .triggered(false)
                .build();

        return alertRepository.save(alert);
    }

    public List<PriceAlert> getAlerts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return alertRepository.findByUser(user);
    }

    public void deleteAlert(Long id, String email) {
        alertRepository.findById(id).ifPresent(alert -> {
            if (alert.getUser().getEmail().equals(email)) {
                alertRepository.delete(alert);
            }
        });
    }

    @Transactional
    public void checkAndTriggerAlerts() {
        log.info("Checking triggered alerts...");
        List<PriceAlert> activeAlerts = alertRepository.findByTriggeredFalse();
        
        for (PriceAlert alert : activeAlerts) {
            try {
                StockQuoteDto quote = yahooFinanceService.getQuote(alert.getSymbol());
                if (quote != null) {
                    boolean isTriggered = false;
                    double currentPrice = quote.getLastPrice();
                    double target = alert.getTargetPrice().doubleValue();

                    if (alert.getCondition() == AlertCondition.ABOVE && currentPrice >= target) {
                        isTriggered = true;
                    } else if (alert.getCondition() == AlertCondition.BELOW && currentPrice <= target) {
                        isTriggered = true;
                    }

                    if (isTriggered) {
                        alert.setTriggered(true);
                        alertRepository.save(alert);
                        
                        emailService.sendPriceAlertEmail(
                                alert.getUser().getEmail(),
                                alert.getSymbol(),
                                alert.getCompanyName(),
                                target,
                                currentPrice,
                                alert.getCondition().name(),
                                quote.getCurrency()
                        );
                        log.info("Triggered alert for {} and sent email to {}", alert.getSymbol(), alert.getUser().getEmail());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to check alert for {}", alert.getSymbol(), e);
            }
        }
    }
}
