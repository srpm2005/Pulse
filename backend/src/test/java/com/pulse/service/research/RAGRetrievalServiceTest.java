package com.pulse.service.research;

import com.pulse.dto.StockQuoteDto;
import com.pulse.repository.PriceAlertRepository;
import com.pulse.repository.TrackedStockRepository;
import com.pulse.repository.UserRepository;
import com.pulse.service.YahooFinanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class RAGRetrievalServiceTest {

    private YahooFinanceService yahooFinanceService;
    private QuantitativeEngineService quantitativeEngineService;
    private TrackedStockRepository trackedStockRepository;
    private PriceAlertRepository priceAlertRepository;
    private UserRepository userRepository;
    private RAGRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        yahooFinanceService = Mockito.mock(YahooFinanceService.class);
        quantitativeEngineService = new QuantitativeEngineService();
        trackedStockRepository = Mockito.mock(TrackedStockRepository.class);
        priceAlertRepository = Mockito.mock(PriceAlertRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        retrievalService = new RAGRetrievalService(
                yahooFinanceService,
                quantitativeEngineService,
                trackedStockRepository,
                priceAlertRepository,
                userRepository
        );
    }

    @Test
    void testResolveToTicker_DirectValidTicker() {
        StockQuoteDto quote = StockQuoteDto.builder()
                .symbol("NVDA")
                .lastPrice(125.0)
                .build();
        when(yahooFinanceService.getQuote("NVDA")).thenReturn(quote);

        String ticker = retrievalService.resolveToTicker("NVDA");
        assertEquals("NVDA", ticker);
    }

    @Test
    void testResolveToTicker_CompanyNameToTicker() {
        when(yahooFinanceService.getQuote("APPLE")).thenReturn(null);
        when(yahooFinanceService.searchSymbol("Apple")).thenReturn(List.of(
                Map.of("symbol", "AAPL", "companyName", "Apple Inc.")
        ));

        String ticker = retrievalService.resolveToTicker("Apple");
        assertEquals("AAPL", ticker);
    }

    @Test
    void testRetrieveGroundedContext_NaturalLanguageQueryResolution() {
        when(yahooFinanceService.getQuote("MICROSOFT")).thenReturn(null);
        when(yahooFinanceService.searchSymbol("Microsoft")).thenReturn(List.of(
                Map.of("symbol", "MSFT", "companyName", "Microsoft Corp")
        ));

        StockQuoteDto msftQuote = StockQuoteDto.builder()
                .symbol("MSFT")
                .companyName("Microsoft Corp")
                .lastPrice(400.0)
                .change(2.5)
                .changePercent(0.6)
                .high(405.0)
                .low(398.0)
                .volume(20000000L)
                .currency("USD")
                .exchangeName("NASDAQ")
                .build();
        when(yahooFinanceService.getQuote("MSFT")).thenReturn(msftQuote);
        when(yahooFinanceService.getChartData("MSFT", "1mo")).thenReturn(Collections.emptyMap());

        RAGRetrievalService.RetrievalResult result = retrievalService.retrieveGroundedContext(
                "Please analyze Microsoft momentum", null, null
        );

        assertNotNull(result);
        assertTrue(result.getSymbols().contains("MSFT"), "Should dynamically resolve 'Microsoft' to MSFT");
    }
}
