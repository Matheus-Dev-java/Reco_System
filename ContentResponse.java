package com.escoladoestudante.reco.api.dto;

import com.escoladoestudante.reco.domain.ContentType;
import java.time.Instant;

public record ContentResponse(
    Long id,
    ContentType type,
    String title,
    String description,
    String category,
    String tags,
    String url,
    Instant createdAt,
    Instant updatedAt
) {}
