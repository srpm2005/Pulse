package com.pulse.service.research;

import com.pulse.dto.StockQuoteDto;
import com.pulse.dto.research.TechnicalIndicatorsDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuantitativeEngineService {

    public TechnicalIndicatorsDto computeIndicators(String symbol, StockQuoteDto quote, List<Double> historicalPrices) {
        double currentPrice = (quote != null && quote.getLastPrice() > 0)
                ? quote.getLastPrice()
                : (historicalPrices != null && !historicalPrices.isEmpty() ? historicalPrices.get(historicalPrices.size() - 1) : 0.0);

        double sma20 = calculateSMA(historicalPrices, 20);
        double sma50 = calculateSMA(historicalPrices, 50);
        double rsi14 = calculateRSI(historicalPrices, 14);
        double annualizedVol = calculateAnnualizedVolatility(historicalPrices);
        double momentum30d = calculateMomentum(historicalPrices);

        double high52 = (quote != null && quote.getFiftyTwoWeekHigh() > 0) ? quote.getFiftyTwoWeekHigh() : currentPrice * 1.15;
        double low52 = (quote != null && quote.getFiftyTwoWeekLow() > 0) ? quote.getFiftyTwoWeekLow() : currentPrice * 0.85;
        double rangePosition52w = calculateRangePosition52w(currentPrice, low52, high52);

        double sma20Divergence = (sma20 > 0) ? ((currentPrice - sma20) / sma20) * 100.0 : 0.0;

        String trendRegime;
        if (currentPrice > sma20 && sma20 >= sma50) {
            trendRegime = "STRONG_BULLISH (Price > SMA20 > SMA50)";
        } else if (currentPrice > sma20) {
            trendRegime = "MODERATE_BULLISH (Price > SMA20)";
        } else if (currentPrice < sma20 && sma20 <= sma50) {
            trendRegime = "STRONG_BEARISH (Price < SMA20 < SMA50)";
        } else if (currentPrice < sma20) {
            trendRegime = "MODERATE_BEARISH (Price < SMA20)";
        } else {
            trendRegime = "CONSOLIDATION_NEUTRAL";
        }

        String rsiRegime;
        if (rsi14 >= 70.0) {
            rsiRegime = "OVERBOUGHT (RSI >= 70)";
        } else if (rsi14 <= 30.0) {
            rsiRegime = "OVERSOLD (RSI <= 30)";
        } else {
            rsiRegime = "NEUTRAL_MOMENTUM (30 < RSI < 70)";
        }

        return TechnicalIndicatorsDto.builder()
                .symbol(symbol)
                .currentPrice(round(currentPrice, 2))
                .sma20(round(sma20, 2))
                .sma50(round(sma50, 2))
                .rsi14(round(rsi14, 2))
                .annualizedVolatilityPercent(round(annualizedVol, 2))
                .momentum30dPercent(round(momentum30d, 2))
                .fiftyTwoWeekHigh(round(high52, 2))
                .fiftyTwoWeekLow(round(low52, 2))
                .rangePosition52wPercent(round(rangePosition52w, 2))
                .sma20DivergencePercent(round(sma20Divergence, 2))
                .trendRegime(trendRegime)
                .rsiRegime(rsiRegime)
                .build();
    }

    public double calculateSMA(List<Double> prices, int period) {
        if (prices == null || prices.isEmpty()) {
            return 0.0;
        }
        int size = prices.size();
        int window = Math.min(size, period);
        double sum = 0.0;
        for (int i = size - window; i < size; i++) {
            sum += prices.get(i);
        }
        return sum / window;
    }

    public double calculateRSI(List<Double> prices, int period) {
        if (prices == null || prices.size() < 3) {
            return 50.0; // default neutral
        }

        int count = prices.size();
        int lookback = Math.min(count - 1, period);

        double totalGain = 0.0;
        double totalLoss = 0.0;

        for (int i = count - lookback; i < count; i++) {
            double diff = prices.get(i) - prices.get(i - 1);
            if (diff > 0) {
                totalGain += diff;
            } else {
                totalLoss += Math.abs(diff);
            }
        }

        double avgGain = totalGain / lookback;
        double avgLoss = totalLoss / lookback;

        if (avgLoss == 0.0) {
            return 100.0;
        }
        if (avgGain == 0.0) {
            return 0.0;
        }

        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    public double calculateAnnualizedVolatility(List<Double> prices) {
        if (prices == null || prices.size() < 3) {
            return 15.0; // historical market baseline
        }

        int n = prices.size() - 1;
        double[] logReturns = new double[n];
        double sumReturns = 0.0;

        for (int i = 0; i < n; i++) {
            double pPrev = prices.get(i);
            double pCurr = prices.get(i + 1);
            if (pPrev > 0 && pCurr > 0) {
                logReturns[i] = Math.log(pCurr / pPrev);
            } else {
                logReturns[i] = 0.0;
            }
            sumReturns += logReturns[i];
        }

        double meanReturn = sumReturns / n;
        double sumSquaredDiff = 0.0;
        for (double r : logReturns) {
            sumSquaredDiff += Math.pow(r - meanReturn, 2);
        }

        double variance = sumSquaredDiff / Math.max(1, n - 1);
        double dailyStdDev = Math.sqrt(variance);

        // Annualize with 252 trading days
        return dailyStdDev * Math.sqrt(252) * 100.0;
    }

    public double calculateMomentum(List<Double> prices) {
        if (prices == null || prices.size() < 2) {
            return 0.0;
        }
        double first = prices.get(0);
        double last = prices.get(prices.size() - 1);
        if (first <= 0) return 0.0;
        return ((last - first) / first) * 100.0;
    }

    public double calculateRangePosition52w(double price, double low52, double high52) {
        if (high52 <= low52 || low52 <= 0) {
            return 50.0;
        }
        double position = ((price - low52) / (high52 - low52)) * 100.0;
        return Math.max(0.0, Math.min(100.0, position));
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
