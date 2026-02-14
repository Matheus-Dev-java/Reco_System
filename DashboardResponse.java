package com.escoladoestudante.reco.api.dto;

public record DashboardResponse(long recommendationRequests24h, double recommendationCacheHitRate24h, long openAiTokens24h, double estimatedCostUsd24h) {}
