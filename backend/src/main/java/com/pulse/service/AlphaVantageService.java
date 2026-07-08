package com.pulse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.StockQuoteDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlphaVantageService {

    @Value("${alphavantage.api.key}")
    private String apiKey;

    @Value("${alphavantage.base-url:https://www.alphavantage.co/query}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public StockQuoteDto getQuote(String symbol) {
        try {
            String url = baseUrl + "?function=GLOBAL_QUOTE&symbol=" + symbol + "&apikey=" + apiKey;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode quote = root.path("Global Quote");
            
            if (quote.isMissingNode() || quote.isEmpty()) {
                return null;
            }

            double lastPrice = quote.path("05. price").asDouble(0.0);
            double change = quote.path("09. change").asDouble(0.0);
            
            String changePercentStr = quote.path("10. change percent").asText("0%");
            changePercentStr = changePercentStr.replace("%", "");
            double changePercent = Double.parseDouble(changePercentStr);
            
            double high = quote.path("03. high").asDouble(0.0);
            double low = quote.path("04. low").asDouble(0.0);
            long volume = quote.path("06. volume").asLong(0);
            
            return StockQuoteDto.builder()
                    .instrumentKey(symbol)
                    .symbol(symbol)
                    .companyName(symbol) 
                    .lastPrice(lastPrice)
                    .change(change)
                    .changePercent(changePercent)
                    .high(high)
                    .low(low)
                    .volume(volume)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, String>> searchSymbol(String query) {
        try {
            String url = baseUrl + "?function=SYMBOL_SEARCH&keywords=" + query + "&apikey=" + apiKey;
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode matches = root.path("bestMatches");
            
            List<Map<String, String>> results = new ArrayList<>();
            if (matches.isArray()) {
                for (JsonNode item : matches) {
                    results.add(Map.of(
                            "instrumentKey", item.path("1. symbol").asText(),
                            "symbol", item.path("1. symbol").asText(),
                            "companyName", item.path("2. name").asText()
                    ));
                }
            }
            return results;

        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
