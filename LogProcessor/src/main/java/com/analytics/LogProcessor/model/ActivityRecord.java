package com.analytics.LogProcessor.model;

import com.analytics.LogProcessor.validation.ValidCategory;
import com.analytics.LogProcessor.validation.ValidIpAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public record ActivityRecord(
        @NotNull(message = "ID is required")
        Long id,
        @NotBlank(message = "Asset name is required")
        String asset,
        @NotBlank(message = "IP address is required")
        @ValidIpAddress()
        String ip,
        @NotBlank(message = "Category is required")
        @ValidCategory()
        String category) implements Serializable {
}
