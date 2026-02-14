package com.escoladoestudante.reco.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InteractionRequest(
    @NotNull Long userId,
    @NotNull Long contentId,
    @NotBlank String kind,
    @Min(0) double value
) {}
