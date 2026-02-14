package com.escoladoestudante.reco.api.dto;

import java.util.List;

public record RecommendationResponse(Long userId, String algo, boolean cacheHit, List<RecommendationItem> items) {}
