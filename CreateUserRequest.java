package com.escoladoestudante.reco.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String externalId) {}
