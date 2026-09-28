package com.pulse.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchResponseDto {
    private String query;
    private List<String> analyzedSymbols;
    private List<GroundedDocumentDto> groundedDocuments;
    private List<ResearchCitationDto> citations;
    private String analysis;
    private String provider;
    private long executionTimeMs;
}
