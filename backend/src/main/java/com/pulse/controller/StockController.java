package com.pulse.controller;

import com.pulse.dto.StockQuoteDto;
import com.pulse.service.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final YahooFinanceService yahooFinanceService;

    @GetMapping("/quote")
    public ResponseEntity<StockQuoteDto> getQuote(@RequestParam("symbol") String symbol) {
        StockQuoteDto quote = yahooFinanceService.getQuote(symbol);
        if (quote == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(quote);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchStock(@RequestParam("q") String query) {
        List<Map<String, String>> results = yahooFinanceService.searchSymbol(query);
        return ResponseEntity.ok(results);
    }
}
