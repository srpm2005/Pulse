package com.pulse.controller;

import com.pulse.entity.TrackedStock;
import com.pulse.entity.User;
import com.pulse.repository.TrackedStockRepository;
import com.pulse.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stocks/tracked")
@RequiredArgsConstructor
public class TrackedStockController {

    private final TrackedStockRepository trackedStockRepository;
    private final UserRepository userRepository;

    @Data
    public static class TrackedStockRequest {
        private String symbol;
        private String companyName;
    }

    @Data
    public static class TrackedStockResponse {
        private String instrumentKey;
        private String symbol;
        private String companyName;

        public TrackedStockResponse(TrackedStock stock) {
            this.instrumentKey = stock.getSymbol();
            this.symbol = stock.getSymbol();
            this.companyName = stock.getCompanyName();
        }
    }

    private User getAuthUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @GetMapping
    public ResponseEntity<List<TrackedStockResponse>> getTrackedStocks(Authentication authentication) {
        User user = getAuthUser(authentication);
        List<TrackedStock> stocks = trackedStockRepository.findByUser(user);
        return ResponseEntity.ok(stocks.stream().map(TrackedStockResponse::new).collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<TrackedStockResponse> addTrackedStock(@RequestBody TrackedStockRequest request, Authentication authentication) {
        User user = getAuthUser(authentication);

        if (trackedStockRepository.findByUserAndSymbol(user, request.getSymbol()).isPresent()) {
            return ResponseEntity.badRequest().build(); // Already tracking
        }

        TrackedStock newStock = TrackedStock.builder()
                .user(user)
                .symbol(request.getSymbol())
                .companyName(request.getCompanyName())
                .build();

        TrackedStock saved = trackedStockRepository.save(newStock);
        return ResponseEntity.ok(new TrackedStockResponse(saved));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> removeTrackedStock(@PathVariable String symbol, Authentication authentication) {
        User user = getAuthUser(authentication);
        
        trackedStockRepository.findByUserAndSymbol(user, symbol).ifPresent(trackedStockRepository::delete);
        return ResponseEntity.ok().build();
    }
}
