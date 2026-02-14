package com.escoladoestudante.reco.api.dto;

import java.time.Instant;

public record UserResponse(Long id, String externalId, Instant createdAt) {}
