package com.circleguard.form.dto;

import java.util.List;

/**
 * Request DTO for creating a questionnaire. Used instead of binding directly to
 * the JPA entity to avoid mass-assignment of server-managed fields (S4684).
 */
public record QuestionnaireRequest(
        String title,
        String description,
        Integer version,
        Boolean isActive,
        List<QuestionRequest> questions
) {
}
