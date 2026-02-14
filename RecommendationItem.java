package com.escoladoestudante.reco.api.dto;

public record RecommendationItem(Long contentId, double score, String category, String title, String reason) {}
