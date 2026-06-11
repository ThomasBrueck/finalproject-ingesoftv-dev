package com.circleguard.form.dto;

import com.circleguard.form.model.QuestionType;

/**
 * Request DTO for a single question inside a questionnaire creation request.
 * Avoids binding directly to the JPA entity (S4684).
 */
public record QuestionRequest(
        String text,
        QuestionType type,
        String options,
        Integer orderIndex
) {
}
