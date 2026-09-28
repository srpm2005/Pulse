package com.pulse.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalIndicatorsDto {
    private String symbol;
    private double currentPrice;
    private double sma20;
    private double sma50;
    private double rsi14;
    private double annualizedVolatilityPercent; // 30-day realized annualized volatility
    private double momentum30dPercent;          // 30-day price change %
    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;
    private double rangePosition52wPercent;     // 0% (at 52w low) to 100% (at 52w high)
    private double sma20DivergencePercent;      // (currentPrice - sma20) / sma20 * 100
    private String trendRegime;                 // "BULLISH_ABOVE_SMA20", "BEARISH_BELOW_SMA20", "CONSOLIDATING"
    private String rsiRegime;                   // "OVERBOUGHT", "OVERSOLD", "NEUTRAL"
}
