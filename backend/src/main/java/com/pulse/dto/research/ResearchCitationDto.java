package com.pulse.dto.research;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResearchCitationDto {
    private String citationId;      // e.g. "[DOC-1]"
    private String documentId;      // e.g. "DOC-1"
    private String title;
    private String source;
    private String verifiableDataPoint;
}
