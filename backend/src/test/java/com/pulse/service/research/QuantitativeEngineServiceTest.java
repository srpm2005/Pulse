package com.pulse.service.research;

import com.pulse.dto.StockQuoteDto;
import com.pulse.dto.research.TechnicalIndicatorsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuantitativeEngineServiceTest {

    private QuantitativeEngineService engine;

    @BeforeEach
    void setUp() {
        engine = new QuantitativeEngineService();
    }

    @Test
    void testCalculateSMA() {
        List<Double> prices = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
        double sma3 = engine.calculateSMA(prices, 3);
        // last 3 are 30, 40, 50 -> average is 40.0
        assertEquals(40.0, sma3, 0.001);

        double smaAll = engine.calculateSMA(prices, 10);
        // all 5 averaged -> 30.0
        assertEquals(30.0, smaAll, 0.001);

        assertEquals(0.0, engine.calculateSMA(null, 5), 0.001);
    }

    @Test
    void testCalculateRSI_Uptrend() {
        // Monotonically increasing prices -> RSI should be near 100
        List<Double> uptrend = List.of(100.0, 102.0, 105.0, 108.0, 112.0, 115.0, 120.0);
        double rsi = engine.calculateRSI(uptrend, 6);
        assertEquals(100.0, rsi, 0.001);
    }

    @Test
    void testCalculateRSI_Downtrend() {
        // Monotonically decreasing prices -> RSI should be 0
        List<Double> downtrend = List.of(120.0, 115.0, 112.0, 108.0, 105.0, 102.0, 100.0);
        double rsi = engine.calculateRSI(downtrend, 6);
        assertEquals(0.0, rsi, 0.001);
    }

    @Test
    void testCalculateAnnualizedVolatility() {
        List<Double> prices = List.of(100.0, 102.0, 101.0, 103.0, 102.5, 104.0, 103.0);
        double vol = engine.calculateAnnualizedVolatility(prices);
        assertTrue(vol > 0.0, "Volatility should be strictly positive for fluctuating prices");
    }

    @Test
    void testCalculateRangePosition52w() {
        double posMid = engine.calculateRangePosition52w(150.0, 100.0, 200.0);
        assertEquals(50.0, posMid, 0.001);

        double posLow = engine.calculateRangePosition52w(100.0, 100.0, 200.0);
        assertEquals(0.0, posLow, 0.001);

        double posHigh = engine.calculateRangePosition52w(200.0, 100.0, 200.0);
        assertEquals(100.0, posHigh, 0.001);
    }

    @Test
    void testComputeIndicators() {
        StockQuoteDto quote = StockQuoteDto.builder()
                .symbol("AAPL")
                .lastPrice(180.0)
                .fiftyTwoWeekHigh(200.0)
                .fiftyTwoWeekLow(150.0)
                .build();

        List<Double> prices = List.of(160.0, 165.0, 170.0, 172.0, 175.0, 180.0);
        TechnicalIndicatorsDto indicators = engine.computeIndicators("AAPL", quote, prices);

        assertNotNull(indicators);
        assertEquals("AAPL", indicators.getSymbol());
        assertEquals(180.0, indicators.getCurrentPrice(), 0.01);
        assertEquals(60.0, indicators.getRangePosition52wPercent(), 0.01);
        assertTrue(indicators.getTrendRegime().contains("BULLISH"));
    }
}
