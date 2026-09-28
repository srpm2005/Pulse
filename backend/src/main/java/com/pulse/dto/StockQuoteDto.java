package com.pulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteDto {
    private String instrumentKey;
    private String symbol;
    private String companyName;
    private String currency;
    private double lastPrice;
    private double change;
    private double changePercent;
    private double high;
    private double low;
    private long volume;
    private double fiftyTwoWeekHigh;
    private double fiftyTwoWeekLow;
    private String exchangeName;
}
