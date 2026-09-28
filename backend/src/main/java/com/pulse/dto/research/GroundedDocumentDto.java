package com.pulse.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroundedDocumentDto {
    private String id;              // e.g. "DOC-1"
    private String title;           // e.g. "AAPL Real-Time Market Telemetry"
    private String source;          // e.g. "Yahoo Finance API"
    private String category;        // "LIVE_QUOTE", "QUANTITATIVE_INDICATORS", "PRICE_ALERT"
    private String symbol;          // e.g. "AAPL"
    private Map<String, Object> metrics;
    private String content;         // Grounded factual data used to eliminate hallucination
    private String retrievedAt;     // Timestamp
}
