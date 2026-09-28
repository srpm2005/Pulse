package com.pulse.controller;

import com.pulse.dto.research.ResearchRequest;
import com.pulse.dto.research.ResearchResponseDto;
import com.pulse.service.research.LlmResearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/research")
@RequiredArgsConstructor
public class ResearchController {

    private final LlmResearchService llmResearchService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResearch(
            @RequestParam("query") String query,
            @RequestParam(value = "symbols", required = false) List<String> symbols,
            Authentication authentication
    ) {
        String userEmail = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        log.info("Initiating SSE Quantitative Research Stream. User: {}, Query: '{}'", userEmail, query);

        // 3-minute timeout for SSE stream
        SseEmitter emitter = new SseEmitter(180_000L);

        emitter.onCompletion(() -> log.debug("SSE research stream completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE research stream timed out");
            emitter.complete();
        });
        emitter.onError(e -> log.error("SSE research stream error: {}", e.getMessage()));

        llmResearchService.streamResearch(query, symbols, userEmail, emitter);
        return emitter;
    }

    @PostMapping("/query")
    public ResponseEntity<ResearchResponseDto> queryResearch(
            @Valid @RequestBody ResearchRequest request,
            Authentication authentication
    ) {
        String userEmail = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : null;

        ResearchResponseDto response = llmResearchService.generateReportSync(
                request.getQuery(),
                request.getSymbols(),
                userEmail
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/presets")
    public ResponseEntity<List<Map<String, String>>> getResearchPresets() {
        return ResponseEntity.ok(List.of(
                Map.of(
                        "title", "AAPL Quantitative & SMA Analysis",
                        "query", "Analyze AAPL: calculate its 20-day SMA, 52-week position, and short-term volatility."
                ),
                Map.of(
                        "title", "NVDA vs TSLA Momentum Comparison",
                        "query", "Compare NVDA and TSLA 14-day RSI, 30-day momentum, and relative trend strength."
                ),
                Map.of(
                        "title", "Portfolio Risk & Price Alert Audit",
                        "query", "Perform a quantitative audit of my portfolio watchlist and check proximity to active price alerts."
                ),
                Map.of(
                        "title", "MSFT Volatility & Mean Reversion",
                        "query", "Analyze MSFT 30-day realized volatility and evaluate mean-reversion risk."
                )
        ));
    }
}
