package com.pulse.dto;

import com.pulse.entity.AlertCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRequest {
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    private String companyName;
    
    @NotNull(message = "Target price is required")
    @Positive(message = "Target price must be positive")
    private BigDecimal targetPrice;
    
    @NotNull(message = "Condition is required")
    private AlertCondition condition;
}
