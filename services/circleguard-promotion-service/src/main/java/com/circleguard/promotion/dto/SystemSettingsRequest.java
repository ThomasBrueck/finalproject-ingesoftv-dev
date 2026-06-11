package com.circleguard.promotion.dto;

/**
 * Request DTO for updating system settings. Used instead of binding directly to
 * the JPA entity to avoid mass-assignment of server-managed fields (S4684).
 */
public record SystemSettingsRequest(
        Boolean unconfirmedFencingEnabled,
        Long autoThresholdSeconds,
        Integer mandatoryFenceDays,
        Integer encounterWindowDays
) {
}
