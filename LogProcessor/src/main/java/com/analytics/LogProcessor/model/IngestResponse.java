package com.analytics.LogProcessor.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngestResponse{
    private Integer recordsIngested;
    private String message;
}
