package com.circleguard.form.dto;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for submitting a health survey. Used instead of binding directly
 * to the JPA entity so clients cannot set server-managed fields such as id,
 * validationStatus or validatedBy (S4684).
 */
public record HealthSurveyRequest(
        UUID anonymousId,
        Boolean hasFever,
        Boolean hasCough,
        String otherSymptoms,
        LocalDate exposureDate,
        Map<String, Object> responses,
        String attachmentPath
) {
}
