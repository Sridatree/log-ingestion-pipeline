package com.analytics.LogProcessor.controller;

import com.analytics.LogProcessor.model.ActivityRecord;
import com.analytics.LogProcessor.model.IngestRequest;
import com.analytics.LogProcessor.model.IngestResponse;
import com.analytics.LogProcessor.service.IngestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for LogProcessorController
 */
@WebMvcTest(LogProcessorController.class)
class LogProcessorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IngestService ingestService;

    @Test
    void testIngestSuccess() throws Exception {
        // Arrange
        IngestRequest request = new IngestRequest(Arrays.asList(
                new ActivityRecord(1L, "asset1", "192.168.1.1", "phishing"),
                new ActivityRecord(2L, "asset2", "10.0.0.1", "phishing")
        ));

        IngestResponse response = IngestResponse.builder()
                .recordsIngested(2)
                .message("Records Ingested to raw queue")
                .build();

        when(ingestService.ingest(anyList(), anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/private/v1/ingest")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recordsIngested").value(2))
                .andExpect(jsonPath("$.message").value("Records Ingested to raw queue"));
    }

    @Test
    void testIngestValidationFailureInvalidIp() throws Exception {
        // Arrange - Invalid IP address
        IngestRequest request = new IngestRequest(List.of(
                new ActivityRecord(1L, "asset1", "999.999.999.999", "phishing")
        ));

        // Act & Assert
        mockMvc.perform(post("/private/v1/ingest")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngestValidationFailureInvalidCategory() throws Exception {
        // Arrange - Invalid category
        IngestRequest request = new IngestRequest(List.of(
                new ActivityRecord(1L, "asset1", "192.168.1.1", "invalid-category")
        ));

        // Act & Assert
        mockMvc.perform(post("/private/v1/ingest")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testIngestValidationFailureMissingFields() throws Exception {
        // Arrange - Missing required fields (null asset, ip, category)
        String invalidJson = "{\"activityRecordList\":[{\"id\":1}]}";

        // Act & Assert
        mockMvc.perform(post("/private/v1/ingest")
                        .header("Authorization", "test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

}

