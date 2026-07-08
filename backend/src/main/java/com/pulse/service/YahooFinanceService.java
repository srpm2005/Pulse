package com.pulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.StockQuoteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class YahooFinanceService {

    private static final HttpEntity<String> HEADERS;
    static {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        HEADERS = new HttpEntity<>(headers);
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;



    public StockQuoteDto getQuote(String symbol) {
        try {
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol;
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, HEADERS, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode resultNode = root.path("chart").path("result");

            if (resultNode.isMissingNode() || resultNode.isEmpty()) {
                return null;
            }

            JsonNode meta = resultNode.get(0).path("meta");
            
            double lastPrice = meta.path("regularMarketPrice").asDouble(0.0);
            String currency = meta.path("currency").asText("USD");
            double previousClose = meta.path("previousClose").asDouble(0.0);
            double high = meta.path("regularMarketDayHigh").asDouble(0.0);
            double low = meta.path("regularMarketDayLow").asDouble(0.0);
            long volume = meta.path("regularMarketVolume").asLong(0);
            
            double change = lastPrice - previousClose;
            double changePercent = previousClose != 0 ? (change / previousClose) * 100 : 0.0;
            
            // Limit precision
            change = Math.round(change * 100.0) / 100.0;
            changePercent = Math.round(changePercent * 100.0) / 100.0;
            
            return StockQuoteDto.builder()
                    .instrumentKey(symbol)
                    .symbol(symbol)
                    .companyName(symbol) 
                    .currency(currency)
                    .lastPrice(lastPrice)
                    .change(change)
                    .changePercent(changePercent)
                    .high(high)
                    .low(low)
                    .volume(volume)
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch quote for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    public Map<String, Object> getChartData(String symbol, String range) {
        try {
            String interval = "1d";
            if ("1d".equals(range)) interval = "5m";
            else if ("5d".equals(range)) interval = "15m";
            
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?range=" + range + "&interval=" + interval;
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, HEADERS, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode resultNode = root.path("chart").path("result").get(0);
            
            if (resultNode == null || resultNode.isMissingNode()) {
                return null;
            }

            JsonNode timestamps = resultNode.path("timestamp");
            JsonNode closePrices = resultNode.path("indicators").path("quote").get(0).path("close");

            List<Long> timeList = new ArrayList<>();
            List<Double> priceList = new ArrayList<>();

            if (timestamps.isArray() && closePrices.isArray()) {
                for (int i = 0; i < timestamps.size(); i++) {
                    if (!closePrices.get(i).isNull()) {
                        timeList.add(timestamps.get(i).asLong());
                        priceList.add(closePrices.get(i).asDouble());
                    }
                }
            }

            Map<String, Object> chartData = new HashMap<>();
            chartData.put("timestamps", timeList);
            chartData.put("prices", priceList);
            return chartData;

        } catch (Exception e) {
            log.error("Failed to fetch chart for {}: {}", symbol, e.getMessage());
            return null;
        }
    }

    public List<Map<String, String>> searchSymbol(String query) {
        try {
            String url = "https://query2.finance.yahoo.com/v1/finance/search?q=" + query + "&quotesCount=10";
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, HEADERS, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode quotes = root.path("quotes");
            
            List<Map<String, String>> results = new ArrayList<>();
            if (quotes.isArray()) {
                for (JsonNode item : quotes) {
                    String type = item.path("quoteType").asText();
                    if ("EQUITY".equalsIgnoreCase(type) || "ETF".equalsIgnoreCase(type)) {
                        results.add(Map.of(
                                "instrumentKey", item.path("symbol").asText(),
                                "symbol", item.path("symbol").asText(),
                                "companyName", item.path("shortname").asText(item.path("longname").asText())
                        ));
                    }
                }
            }
            return results;

        } catch (Exception e) {
            log.error("Failed to search symbol {}: {}", query, e.getMessage());
            return List.of();
        }
    }
}
