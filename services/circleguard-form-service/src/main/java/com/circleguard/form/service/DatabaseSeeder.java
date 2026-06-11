package com.circleguard.form.service;

import com.circleguard.form.model.Question;
import com.circleguard.form.model.QuestionType;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import java.util.List;

@Configuration
@Lazy(false)
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class DatabaseSeeder {
    private final QuestionnaireRepository questionnaireRepository;

    @PostConstruct
    public void seedQuestionnaire() {
        try {
            if (questionnaireRepository.count() == 0) {
                log.info("Seeding default active questionnaire...");
                Questionnaire q = Questionnaire.builder()
                        .title("Default Health Survey")
                        .description("Daily symptoms questionnaire for campus access")
                        .version(1)
                        .isActive(true)
                        .build();

                Question q1 = Question.builder()
                        .text("Do you have fever?")
                        .type(QuestionType.YES_NO)
                        .orderIndex(0)
                        .questionnaire(q)
                        .build();

                Question q2 = Question.builder()
                        .text("Do you have a cough?")
                        .type(QuestionType.YES_NO)
                        .orderIndex(1)
                        .questionnaire(q)
                        .build();

                q.setQuestions(List.of(q1, q2));
                questionnaireRepository.save(q);
                log.info("Default active questionnaire seeded successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to seed default active questionnaire", e);
        }
    }
}
