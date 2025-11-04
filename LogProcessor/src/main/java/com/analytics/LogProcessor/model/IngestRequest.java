package com.analytics.LogProcessor.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record IngestRequest(
        @Valid
        @NotEmpty
        List<ActivityRecord> activityRecordList){}
