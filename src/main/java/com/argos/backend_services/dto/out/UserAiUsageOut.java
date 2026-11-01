package com.argos.backend_services.dto.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record UserAiUsageOut(
        UUID userId,
        String name,
        String email,
        String photoUrl,
        long calls,
        long tokens,
        long modelCalls,
        long unknownTokenCalls,
        @JsonProperty("known_cost_calls") long knownCostCalls,
        @JsonProperty("unknown_cost_calls") long unknownCostCalls,
        @JsonProperty("cost_usd") BigDecimal costUsd
) {}
