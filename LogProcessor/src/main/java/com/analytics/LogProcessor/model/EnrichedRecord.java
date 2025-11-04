package com.analytics.LogProcessor.model;

public record EnrichedRecord(long id, String asset,String ip, String category,  String asn,long correlationId) {}

