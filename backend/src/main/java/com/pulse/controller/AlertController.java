package com.pulse.controller;

import com.pulse.dto.AlertRequest;
import com.pulse.entity.PriceAlert;
import com.pulse.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public ResponseEntity<PriceAlert> createAlert(@Valid @RequestBody AlertRequest request, Authentication authentication) {
        PriceAlert alert = alertService.createAlert(request, authentication.getName());
        return ResponseEntity.ok(alert);
    }

    @GetMapping
    public ResponseEntity<List<PriceAlert>> getAlerts(Authentication authentication) {
        return ResponseEntity.ok(alertService.getAlerts(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id, Authentication authentication) {
        alertService.deleteAlert(id, authentication.getName());
        return ResponseEntity.ok().build();
    }
}
