package com.pulse.service.research;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.research.GroundedDocumentDto;
import com.pulse.dto.research.ResearchCitationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmResearchServiceTest {

    private LlmResearchService service;

    @BeforeEach
    void setUp() {
        RAGRetrievalService retrievalService = Mockito.mock(RAGRetrievalService.class);
        ObjectMapper mapper = new ObjectMapper();
        RestTemplate restTemplate = new RestTemplate();
        service = new LlmResearchService(retrievalService, mapper, restTemplate);
    }

    @Test
    void testSynthesizeGroundedReport_ContainsCitationsAndGroundedMetrics() {
        GroundedDocumentDto liveDoc = GroundedDocumentDto.builder()
                .id("DOC-1")
                .symbol("NVDA")
                .category("LIVE_QUOTE")
                .title("NVDA Real-Time Telemetry")
                .source("Yahoo Finance Realtime Telemetry")
                .metrics(Map.of(
                        "lastPrice", 125.50,
                        "changePercent", 3.25,
                        "dayLow", 122.0,
                        "dayHigh", 126.8,
                        "volume", 45000000L,
                        "currency", "USD"
                ))
                .content("NVDA trading at 125.50 (+3.25%)")
                .build();

        GroundedDocumentDto quantDoc = GroundedDocumentDto.builder()
                .id("DOC-2")
                .symbol("NVDA")
                .category("QUANTITATIVE_INDICATORS")
                .title("NVDA Quantitative Indicators")
                .source("Pulse Quantitative Engine")
                .metrics(Map.of(
                        "sma20", 120.0,
                        "sma50", 115.0,
                        "rsi14", 62.4,
                        "volatility30d", 28.5,
                        "momentum30d", 8.4,
                        "rangePosition52w", 85.0,
                        "trendRegime", "STRONG_BULLISH"
                ))
                .content("NVDA SMA20: 120.0, RSI: 62.4")
                .build();

        ResearchCitationDto citation1 = new ResearchCitationDto("[DOC-1]", "DOC-1", "NVDA Real-Time Telemetry", "Yahoo Finance", "125.50");
        ResearchCitationDto citation2 = new ResearchCitationDto("[DOC-2]", "DOC-2", "NVDA Quantitative Indicators", "Pulse Engine", "RSI 62.4");

        RAGRetrievalService.RetrievalResult result = new RAGRetrievalService.RetrievalResult(
                List.of("NVDA"),
                List.of(liveDoc, quantDoc),
                List.of(citation1, citation2)
        );

        String report = service.synthesizeGroundedReport("Analyze NVDA momentum and valuation", result);

        assertNotNull(report);
        assertTrue(report.contains("NVDA"));
        assertTrue(report.contains("[Source: DOC-1]"), "Report must cite DOC-1");
        assertTrue(report.contains("[Source: DOC-2]"), "Report must cite DOC-2");
        assertTrue(report.contains("125.50"));
        assertTrue(report.contains("62.40") || report.contains("62.4"));
        assertTrue(report.contains("Anti-Hallucination Guarantee"));
    }
}
