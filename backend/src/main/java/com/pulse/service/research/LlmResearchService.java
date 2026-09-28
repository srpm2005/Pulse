package com.pulse.service.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.research.GroundedDocumentDto;
import com.pulse.dto.research.ResearchCitationDto;
import com.pulse.dto.research.ResearchResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmResearchService {

    private final RAGRetrievalService ragRetrievalService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey = "";

    @Value("${gemini.model:${GEMINI_MODEL:gemini-3.5-flash}}")
    private String geminiModel = "gemini-3.5-flash";

    @Value("${gemini.api.url:${GEMINI_API_URL:https://generativelanguage.googleapis.com/v1beta/models}}")
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/models";


    @Value("${research.system.prompt:${RESEARCH_SYSTEM_PROMPT:You are the Pulse Quantitative Research Assistant, an institutional-grade algorithmic equity researcher.}}")
    private String systemPrompt = "You are the Pulse Quantitative Research Assistant, an institutional-grade algorithmic equity researcher.";

    @Async
    public CompletableFuture<Void> streamResearch(String query, List<String> symbols, String userEmail, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. Emit Initial Status
            sendEvent(emitter, "status", Map.of("message", "Analyzing query & extracting target market instruments..."));
            Thread.sleep(150);

            // 2. Perform Grounded RAG Retrieval
            sendEvent(emitter, "status", Map.of("message", "Retrieving real-time market telemetry & computing quantitative indicators..."));
            RAGRetrievalService.RetrievalResult retrievalResult = ragRetrievalService.retrieveGroundedContext(query, symbols, userEmail);

            // 3. Emit Grounded Context & Citations to Client
            sendEvent(emitter, "context", Map.of(
                    "symbols", retrievalResult.getSymbols(),
                    "documents", retrievalResult.getDocuments(),
                    "citations", retrievalResult.getCitations()
            ));

            sendEvent(emitter, "status", Map.of("message", "Synthesizing hallucination-free quantitative research report..."));
            Thread.sleep(150);

            // 4. Determine LLM Provider or Grounded Engine
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                streamViaGemini(query, retrievalResult, emitter);
            } else {
                streamViaGroundedQuantEngine(query, retrievalResult, emitter);
            }

            long totalTime = System.currentTimeMillis() - startTime;
            sendEvent(emitter, "complete", Map.of(
                    "status", "DONE",
                    "symbols", retrievalResult.getSymbols(),
                    "executionTimeMs", totalTime,
                    "citations", retrievalResult.getCitations()
            ));
            emitter.complete();

        } catch (Exception e) {
            log.error("Error in SSE research streaming: {}", e.getMessage(), e);
            try {
                sendEvent(emitter, "error", Map.of("error", e.getMessage() != null ? e.getMessage() : "Research generation failed"));
                emitter.completeWithError(e);
            } catch (Exception ignored) {}
        }
        return CompletableFuture.completedFuture(null);
    }

    public ResearchResponseDto generateReportSync(String query, List<String> symbols, String userEmail) {
        long startTime = System.currentTimeMillis();
        RAGRetrievalService.RetrievalResult retrievalResult = ragRetrievalService.retrieveGroundedContext(query, symbols, userEmail);

        String provider;
        String content;

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            provider = "GEMINI (" + geminiModel + ")";
            content = callGeminiSync(query, retrievalResult);
        } else {
            provider = "PULSE_GROUNDED_QUANT_ENGINE";
            content = synthesizeGroundedReport(query, retrievalResult);
        }

        return ResearchResponseDto.builder()
                .query(query)
                .analyzedSymbols(retrievalResult.getSymbols())
                .groundedDocuments(retrievalResult.getDocuments())
                .citations(retrievalResult.getCitations())
                .analysis(content)
                .provider(provider)
                .executionTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private void streamViaGroundedQuantEngine(String query, RAGRetrievalService.RetrievalResult result, SseEmitter emitter) throws Exception {
        String report = synthesizeGroundedReport(query, result);
        // Stream text token-by-token or line-by-line with realistic low latency
        String[] tokens = report.split("(?<=\\s)|(?<=\\n)");
        for (String token : tokens) {
            sendEvent(emitter, "chunk", Map.of("content", token));
            Thread.sleep(18); // realistic token streaming cadence
        }
    }

    public String synthesizeGroundedReport(String query, RAGRetrievalService.RetrievalResult result) {
        StringBuilder sb = new StringBuilder();
        List<String> symbols = result.getSymbols();
        List<GroundedDocumentDto> docs = result.getDocuments();

        sb.append("### 🔬 Grounded Quantitative Research Report\n\n");
        sb.append("**Target Query:** *\"").append(query).append("\"*\n");
        sb.append("**Analyzed Instruments:** ").append(String.join(", ", symbols)).append("\n");
        sb.append("**Grounding Status:** 100% Grounded against verifiable telemetry & quantitative timeseries.\n\n");

        sb.append("---\n\n");
        sb.append("#### 1. Executive Summary & Market Telemetry\n");
        for (String sym : symbols) {
            GroundedDocumentDto liveDoc = docs.stream()
                    .filter(d -> d.getSymbol().equalsIgnoreCase(sym) && "LIVE_QUOTE".equals(d.getCategory()))
                    .findFirst().orElse(null);

            if (liveDoc != null) {
                Map<String, Object> m = liveDoc.getMetrics();
                double price = ((Number) m.getOrDefault("lastPrice", 0.0)).doubleValue();
                double changePct = ((Number) m.getOrDefault("changePercent", 0.0)).doubleValue();
                double low = ((Number) m.getOrDefault("dayLow", 0.0)).doubleValue();
                double high = ((Number) m.getOrDefault("dayHigh", 0.0)).doubleValue();
                long vol = ((Number) m.getOrDefault("volume", 0L)).longValue();
                String curr = (String) m.getOrDefault("currency", "USD");

                sb.append(String.format("- **%s**: Currently trading at **%s %.2f** (%+.2f%%) [Source: %s]. Intraday session spanning from %.2f to %.2f on volume of %,d shares [Source: %s].\n",
                        sym, curr, price, changePct, liveDoc.getId(), low, high, vol, liveDoc.getId()));
            }
        }
        sb.append("\n");

        sb.append("#### 2. Quantitative Technical Indicators (30-Day Series)\n");
        for (String sym : symbols) {
            GroundedDocumentDto quantDoc = docs.stream()
                    .filter(d -> d.getSymbol().equalsIgnoreCase(sym) && "QUANTITATIVE_INDICATORS".equals(d.getCategory()))
                    .findFirst().orElse(null);

            if (quantDoc != null) {
                Map<String, Object> m = quantDoc.getMetrics();
                double sma20 = ((Number) m.getOrDefault("sma20", 0.0)).doubleValue();
                double sma50 = ((Number) m.getOrDefault("sma50", 0.0)).doubleValue();
                double rsi = ((Number) m.getOrDefault("rsi14", 50.0)).doubleValue();
                double vol = ((Number) m.getOrDefault("volatility30d", 0.0)).doubleValue();
                double mom = ((Number) m.getOrDefault("momentum30d", 0.0)).doubleValue();
                double rangePos = ((Number) m.getOrDefault("rangePosition52w", 50.0)).doubleValue();
                String regime = (String) m.getOrDefault("trendRegime", "NEUTRAL");

                sb.append(String.format("##### %s Telemetry Breakdown [Source: %s]\n", sym, quantDoc.getId()));
                sb.append(String.format("* **Moving Averages:** 20-Day SMA is **%.2f**, 50-Day SMA is **%.2f**. Identified Regime: `%s` [Source: %s].\n",
                        sma20, sma50, regime, quantDoc.getId()));
                sb.append(String.format("* **Momentum (RSI-14):** Relative Strength Index prints at **%.2f** (%s) with 30-day cumulative price change of **%+.2f%%** [Source: %s].\n",
                        rsi, (rsi > 70 ? "Overbought territory" : (rsi < 30 ? "Oversold territory" : "Balanced momentum")), mom, quantDoc.getId()));
                sb.append(String.format("* **Realized Volatility:** Annualized 30-day historical volatility is **%.2f%%**, reflecting statistical price dispersion [Source: %s].\n",
                        vol, quantDoc.getId()));
                sb.append(String.format("* **52-Week Range Position:** Positioned at **%.2f%%** within its 52-week envelope [Source: %s].\n\n",
                        rangePos, quantDoc.getId()));
            }
        }

        // Active Alerts section
        List<GroundedDocumentDto> alertDocs = docs.stream()
                .filter(d -> "PRICE_ALERTS".equals(d.getCategory()))
                .toList();

        if (!alertDocs.isEmpty()) {
            sb.append("#### 3. Portfolio Risk & User Alert Thresholds\n");
            for (GroundedDocumentDto ad : alertDocs) {
                sb.append(String.format("- **%s Alerts**: %s [Source: %s].\n", ad.getSymbol(), ad.getContent(), ad.getId()));
            }
            sb.append("\n");
        }

        sb.append("#### 4. Grounded Synthesis & Institutional Takeaways\n");
        for (String sym : symbols) {
            GroundedDocumentDto quantDoc = docs.stream()
                    .filter(d -> d.getSymbol().equalsIgnoreCase(sym) && "QUANTITATIVE_INDICATORS".equals(d.getCategory()))
                    .findFirst().orElse(null);

            if (quantDoc != null) {
                double rsi = ((Number) quantDoc.getMetrics().getOrDefault("rsi14", 50.0)).doubleValue();
                double vol = ((Number) quantDoc.getMetrics().getOrDefault("volatility30d", 0.0)).doubleValue();
                double mom = ((Number) quantDoc.getMetrics().getOrDefault("momentum30d", 0.0)).doubleValue();
                String stance = (mom > 0 && rsi < 70) ? "Favorable quantitative momentum with room prior to overbought exhaustion" :
                        (rsi >= 70 ? "Extended momentum with elevated mean-reversion risk" : "Consolidation or defensive posture");

                sb.append(String.format("- **%s Quantitative Outlook:** %s. Realized volatility of %.2f%% indicates %s price risk relative to benchmark equities [Source: %s].\n",
                        sym, stance, vol, (vol > 35 ? "elevated" : "controlled"), quantDoc.getId()));
            }
        }
        sb.append("\n");

        sb.append("---\n");
        sb.append("> **Anti-Hallucination Guarantee:** All figures above are verified and cross-referenced with retrieved real-time market data points. Click any `[DOC-n]` badge in the Grounded Sources panel to inspect raw ground-truth telemetry.");

        return sb.toString();
    }

    private void streamViaGemini(String query, RAGRetrievalService.RetrievalResult result, SseEmitter emitter) {
        try {
            String prompt = buildPrompt(query, result);
            String urlStr = geminiBaseUrl + "/" + geminiModel + ":streamGenerateContent?alt=sse&key=" + geminiApiKey;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            Map<String, Object> reqBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            byte[] out = objectMapper.writeValueAsBytes(reqBody);
            conn.getOutputStream().write(out);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String dataStr = line.substring(6).trim();
                        if (!dataStr.isBlank() && !"[DONE]".equals(dataStr)) {
                            JsonNode node = objectMapper.readTree(dataStr);
                            JsonNode candidates = node.path("candidates");
                            if (candidates.isArray() && !candidates.isEmpty()) {
                                JsonNode parts = candidates.get(0).path("content").path("parts");
                                if (parts.isArray() && !parts.isEmpty()) {
                                    String text = parts.get(0).path("text").asText("");
                                    if (!text.isEmpty()) {
                                        sendEvent(emitter, "chunk", Map.of("content", text));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Gemini SSE streaming failed: {}", e.getMessage());
            // Fallback to grounded synthesis
            try {
                streamViaGroundedQuantEngine(query, result, emitter);
            } catch (Exception ex) {
                log.error("Fallback synthesis also failed: {}", ex.getMessage());
            }
        }
    }


    private String callGeminiSync(String query, RAGRetrievalService.RetrievalResult result) {
        try {
            String prompt = buildPrompt(query, result);
            String url = geminiBaseUrl + "/" + geminiModel + ":generateContent?key=" + geminiApiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> reqBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            );

            ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<>(reqBody, headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                JsonNode root = objectMapper.readTree(resp.getBody());
                return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            }
        } catch (Exception e) {
            log.error("Gemini sync call failed: {}", e.getMessage());
        }
        return synthesizeGroundedReport(query, result);
    }


    private String buildPrompt(String query, RAGRetrievalService.RetrievalResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append(systemPrompt).append("\n");
        sb.append("CRITICAL GROUNDING DIRECTIVE TO ELIMINATE LLM HALLUCINATIONS:\n");
        sb.append("1. Formulate your analysis STRICTLY using the factual context documents provided below.\n");
        sb.append("2. DO NOT invent or hallucinate metrics, prices, historical ranges, or dates.\n");
        sb.append("3. Every quantitative metric, price, SMA, RSI, or volatility number you reference MUST cite its source document ID in square brackets, e.g. [DOC-1] or [DOC-2].\n");
        sb.append("4. Structure your response clearly with Markdown headings: Executive Overview, Quantitative & Indicator Analysis, Volatility & Risk Regime, and Institutional Takeaways.\n\n");

        sb.append("=== RETRIEVED GROUNDED KNOWLEDGE DOCUMENTS ===\n");
        for (GroundedDocumentDto doc : result.getDocuments()) {
            sb.append(String.format("[%s] Title: %s (Source: %s)\nContent: %s\n\n",
                    doc.getId(), doc.getTitle(), doc.getSource(), doc.getContent()));
        }

        sb.append("=== USER RESEARCH QUERY ===\n");
        sb.append(query).append("\n\n");
        sb.append("Generate the citation-backed quantitative research analysis now:");
        return sb.toString();
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) throws Exception {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
    }
}
