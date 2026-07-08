package com.pulse.scheduler;

import com.pulse.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final AlertService alertService;

    @Scheduled(cron = "${alert.scheduler.cron}")
    public void scheduleAlertCheck() {
        alertService.checkAndTriggerAlerts();
    }
}
