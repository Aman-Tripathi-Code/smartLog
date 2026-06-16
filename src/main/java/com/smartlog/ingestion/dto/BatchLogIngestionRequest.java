package com.smartlog.ingestion.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record BatchLogIngestionRequest(
        @NotEmpty(message = "logs must contain at least one item")
        List<@Valid LogIngestionRequest> logs
) {
}
