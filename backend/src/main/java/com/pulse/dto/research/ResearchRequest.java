package com.pulse.dto.research;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchRequest {
    @NotBlank(message = "Query must not be blank")
    private String query;
    private List<String> symbols;
}
