package com.escoladoestudante.reco.api.dto;

import com.escoladoestudante.reco.domain.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateContentRequest(
    @NotNull ContentType type,
    @NotBlank String title,
    @NotBlank String description,
    @NotBlank String category,
    @NotBlank String tags,
    @NotBlank String url
) {}
