package com.pulse.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockQuoteDto {
    private String instrumentKey;
    private String symbol;
    private String companyName;
    private double lastPrice;
    private double change;
    private double changePercent;
    private double high;
    private double low;
    private long volume;
}
