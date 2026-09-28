package com.pulse.service.research;

import com.pulse.dto.StockQuoteDto;
import com.pulse.dto.research.GroundedDocumentDto;
import com.pulse.dto.research.ResearchCitationDto;
import com.pulse.dto.research.TechnicalIndicatorsDto;
import com.pulse.entity.PriceAlert;
import com.pulse.entity.TrackedStock;
import com.pulse.entity.User;
import com.pulse.repository.PriceAlertRepository;
import com.pulse.repository.TrackedStockRepository;
import com.pulse.repository.UserRepository;
import com.pulse.service.YahooFinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RAGRetrievalService {

    private final YahooFinanceService yahooFinanceService;
    private final QuantitativeEngineService quantitativeEngineService;
    private final TrackedStockRepository trackedStockRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository userRepository;

    private final Map<String, String> resolvedSymbolCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static final Set<String> STOPWORDS = Set.of(
            "WHAT", "IS", "THE", "AND", "FOR", "BUY", "SELL", "HOLD", "HIGH", "LOW",
            "STOCK", "STOCKS", "RSI", "SMA", "RAG", "PORTFOLIO", "WATCHLIST", "ALERT", "ALERTS",
            "RISK", "TECH", "PRICE", "PRICES", "CHECK", "COMPARE", "ANALYSIS", "REPORT",
            "WITH", "FROM", "ABOUT", "SHOW", "TELL", "GIVE", "HOW", "WHY", "DOES", "MARKET",
            "ANALYZE", "PLEASE", "CAN", "YOU", "DO", "IN", "ON", "AT", "OF", "TO", "ME",
            "MY", "THIS", "THAT", "LOOK", "SEE", "PERFORMANCE", "VALUATION", "TODAY", "WEEK",
            "MOMENTUM", "VOLATILITY", "INDICATOR", "INDICATORS", "AVERAGE", "AVERAGES", "MOVING", "OUTLOOK"
    );

    private static final Pattern TICKER_PATTERN = Pattern.compile("\\b[A-Za-z]{2,20}(?:\\.[A-Za-z]{1,2})?\\b");

    public String resolveToTicker(String candidate) {
        if (candidate == null || candidate.isBlank()) return null;
        String clean = candidate.trim().toUpperCase();
        if (STOPWORDS.contains(clean) || clean.length() < 2) return null;

        if (resolvedSymbolCache.containsKey(clean)) {
            return resolvedSymbolCache.get(clean);
        }

        // 1. Direct quote check if already a valid ticker symbol
        StockQuoteDto direct = yahooFinanceService.getQuote(clean);
        if (direct != null && direct.getLastPrice() > 0) {
            resolvedSymbolCache.put(clean, clean);
            return clean;
        }

        // 2. Dynamic symbol search via Yahoo Finance search API (Company name -> Ticker)
        try {
            List<Map<String, String>> matches = yahooFinanceService.searchSymbol(candidate);
            if (matches != null && !matches.isEmpty()) {
                String symbol = matches.get(0).get("symbol");
                if (symbol != null && !symbol.isBlank()) {
                    symbol = symbol.toUpperCase();
                    resolvedSymbolCache.put(clean, symbol);
                    return symbol;
                }
            }
        } catch (Exception e) {
            log.warn("Failed dynamic ticker lookup for '{}': {}", candidate, e.getMessage());
        }

        return null;
    }

    public RetrievalResult retrieveGroundedContext(String query, List<String> explicitSymbols, String userEmail) {
        Set<String> targetSymbols = new LinkedHashSet<>();

        if (explicitSymbols != null && !explicitSymbols.isEmpty()) {
            for (String s : explicitSymbols) {
                String resolved = resolveToTicker(s);
                if (resolved != null) {
                    targetSymbols.add(resolved);
                } else if (s != null && !s.isBlank()) {
                    targetSymbols.add(s.trim().toUpperCase());
                }
            }
        }

        // Extract ticker or company candidates from query
        boolean asksAboutPortfolio = query != null &&
                (query.toUpperCase().contains("PORTFOLIO") || query.toUpperCase().contains("WATCHLIST") || query.toUpperCase().contains("TRACKED"));

        if (query != null && !query.isBlank()) {
            Matcher matcher = TICKER_PATTERN.matcher(query);
            while (matcher.find()) {
                String candidate = matcher.group();
                String upper = candidate.toUpperCase();
                if (!STOPWORDS.contains(upper) && upper.length() >= 2) {
                    String resolved = resolveToTicker(candidate);
                    if (resolved != null) {
                        targetSymbols.add(resolved);
                    }
                }
            }

            // If still no symbols found and query isn't about general portfolio, try searching cleaned query tokens
            if (targetSymbols.isEmpty() && !asksAboutPortfolio) {
                String sanitizedQuery = Arrays.stream(query.split("\\s+"))
                        .filter(w -> !STOPWORDS.contains(w.toUpperCase().replaceAll("[^A-Za-z0-9]", "")))
                        .collect(Collectors.joining(" "))
                        .trim();
                if (!sanitizedQuery.isBlank()) {
                    String resolved = resolveToTicker(sanitizedQuery);
                    if (resolved != null) {
                        targetSymbols.add(resolved);
                    }
                }
            }
        }

        User user = null;
        if (userEmail != null && !userEmail.isBlank()) {
            user = userRepository.findByEmail(userEmail).orElse(null);
        }

        if (user != null && (targetSymbols.isEmpty() || asksAboutPortfolio)) {
            List<TrackedStock> tracked = trackedStockRepository.findByUser(user);
            for (TrackedStock ts : tracked) {
                targetSymbols.add(ts.getSymbol().toUpperCase());
            }
        }

        // If still empty, default to active benchmark tickers
        if (targetSymbols.isEmpty()) {
            targetSymbols.add("AAPL");
            targetSymbols.add("NVDA");
        }

        // Limit to max 4 symbols for bounded latency & dense factual grounding
        List<String> finalSymbols = targetSymbols.stream().limit(4).collect(Collectors.toList());

        List<GroundedDocumentDto> groundedDocs = new ArrayList<>();
        List<ResearchCitationDto> citations = new ArrayList<>();
        List<PriceAlert> userAlerts = (user != null) ? priceAlertRepository.findByUser(user) : Collections.emptyList();

        int docIndex = 1;
        String nowTimestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

        for (String symbol : finalSymbols) {
            StockQuoteDto quote = yahooFinanceService.getQuote(symbol);
            Map<String, Object> chartData = yahooFinanceService.getChartData(symbol, "1mo");

            List<Double> priceSeries = Collections.emptyList();
            if (chartData != null && chartData.containsKey("prices")) {
                Object pObj = chartData.get("prices");
                if (pObj instanceof List<?>) {
                    priceSeries = ((List<?>) pObj).stream()
                            .filter(Double.class::isInstance)
                            .map(Double.class::cast)
                            .collect(Collectors.toList());
                }
            }

            TechnicalIndicatorsDto indicators = quantitativeEngineService.computeIndicators(symbol, quote, priceSeries);

            // Document 1: Live Market Telemetry
            if (quote != null) {
                String docId = "DOC-" + (docIndex++);
                String title = symbol + " Real-Time Telemetry & Exchange Fundamentals";
                String snippet = String.format(
                        "Symbol: %s (%s). Current Last Price: %s %.2f. Day Change: %+.2f (%+.2f%%). Day Range: %.2f - %.2f. Volume: %,d. 52-Week Range: %.2f - %.2f. Exchange: %s.",
                        quote.getSymbol(), quote.getCompanyName(), quote.getCurrency(), quote.getLastPrice(),
                        quote.getChange(), quote.getChangePercent(), quote.getLow(), quote.getHigh(),
                        quote.getVolume(), quote.getFiftyTwoWeekLow(), quote.getFiftyTwoWeekHigh(), quote.getExchangeName()
                );

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("lastPrice", quote.getLastPrice());
                metrics.put("changePercent", quote.getChangePercent());
                metrics.put("dayHigh", quote.getHigh());
                metrics.put("dayLow", quote.getLow());
                metrics.put("volume", quote.getVolume());
                metrics.put("currency", quote.getCurrency());
                metrics.put("exchange", quote.getExchangeName());

                GroundedDocumentDto doc = GroundedDocumentDto.builder()
                        .id(docId)
                        .title(title)
                        .source("Yahoo Finance Realtime Telemetry")
                        .category("LIVE_QUOTE")
                        .symbol(symbol)
                        .metrics(metrics)
                        .content(snippet)
                        .retrievedAt(nowTimestamp)
                        .build();

                groundedDocs.add(doc);
                citations.add(new ResearchCitationDto("[" + docId + "]", docId, title, "Yahoo Finance API", snippet));
            }

            // Document 2: Quantitative Technical Indicators
            if (indicators != null) {
                String techDocId = "DOC-" + (docIndex++);
                String techTitle = symbol + " Quantitative Technical Indicators & Realized Volatility";
                String techSnippet = String.format(
                        "Symbol: %s. Price: %.2f. SMA-20: %.2f (Divergence: %+.2f%%). SMA-50: %.2f. 14-Day RSI: %.2f [%s]. 30-Day Realized Volatility: %.2f%%. 30-Day Momentum: %+.2f%%. 52-Week Range Position: %.2f%%. Trend Regime: %s.",
                        symbol, indicators.getCurrentPrice(), indicators.getSma20(), indicators.getSma20DivergencePercent(),
                        indicators.getSma50(), indicators.getRsi14(), indicators.getRsiRegime(),
                        indicators.getAnnualizedVolatilityPercent(), indicators.getMomentum30dPercent(),
                        indicators.getRangePosition52wPercent(), indicators.getTrendRegime()
                );

                Map<String, Object> techMetrics = new HashMap<>();
                techMetrics.put("sma20", indicators.getSma20());
                techMetrics.put("sma50", indicators.getSma50());
                techMetrics.put("rsi14", indicators.getRsi14());
                techMetrics.put("volatility30d", indicators.getAnnualizedVolatilityPercent());
                techMetrics.put("momentum30d", indicators.getMomentum30dPercent());
                techMetrics.put("rangePosition52w", indicators.getRangePosition52wPercent());
                techMetrics.put("trendRegime", indicators.getTrendRegime());

                GroundedDocumentDto techDoc = GroundedDocumentDto.builder()
                        .id(techDocId)
                        .title(techTitle)
                        .source("Pulse Quantitative Engine (30D Series)")
                        .category("QUANTITATIVE_INDICATORS")
                        .symbol(symbol)
                        .metrics(techMetrics)
                        .content(techSnippet)
                        .retrievedAt(nowTimestamp)
                        .build();

                groundedDocs.add(techDoc);
                citations.add(new ResearchCitationDto("[" + techDocId + "]", techDocId, techTitle, "Pulse Quantitative Engine", techSnippet));
            }

            // Document 3: Configured Alerts for Symbol
            List<PriceAlert> matchedAlerts = userAlerts.stream()
                    .filter(a -> a.getSymbol().equalsIgnoreCase(symbol))
                    .collect(Collectors.toList());

            if (!matchedAlerts.isEmpty()) {
                String alertDocId = "DOC-" + (docIndex++);
                String alertTitle = symbol + " Active Target Price Alerts";
                StringBuilder sb = new StringBuilder("Active alerts for " + symbol + ": ");
                for (PriceAlert alert : matchedAlerts) {
                    sb.append(String.format("[%s target: %.2f, Triggered: %b] ",
                            alert.getCondition(), alert.getTargetPrice(), alert.isTriggered()));
                }
                String alertSnippet = sb.toString().trim();

                GroundedDocumentDto alertDoc = GroundedDocumentDto.builder()
                        .id(alertDocId)
                        .title(alertTitle)
                        .source("Pulse Alerts Database")
                        .category("PRICE_ALERTS")
                        .symbol(symbol)
                        .metrics(Map.of("alertCount", matchedAlerts.size()))
                        .content(alertSnippet)
                        .retrievedAt(nowTimestamp)
                        .build();

                groundedDocs.add(alertDoc);
                citations.add(new ResearchCitationDto("[" + alertDocId + "]", alertDocId, alertTitle, "Pulse Database", alertSnippet));
            }
        }

        return new RetrievalResult(finalSymbols, groundedDocs, citations);
    }

    public static class RetrievalResult {
        private final List<String> symbols;
        private final List<GroundedDocumentDto> documents;
        private final List<ResearchCitationDto> citations;

        public RetrievalResult(List<String> symbols, List<GroundedDocumentDto> documents, List<ResearchCitationDto> citations) {
            this.symbols = symbols;
            this.documents = documents;
            this.citations = citations;
        }

        public List<String> getSymbols() { return symbols; }
        public List<GroundedDocumentDto> getDocuments() { return documents; }
        public List<ResearchCitationDto> getCitations() { return citations; }
    }
}
