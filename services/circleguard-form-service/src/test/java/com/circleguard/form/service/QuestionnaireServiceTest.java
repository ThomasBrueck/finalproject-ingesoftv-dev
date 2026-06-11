package com.circleguard.form.service;

import com.circleguard.form.model.Question;
import com.circleguard.form.model.QuestionType;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class QuestionnaireServiceTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    @MockBean
    private QuestionnaireRepository questionnaireRepository;

    @Test
    void shouldReturnActiveQuestionnaire() {
        Questionnaire q = Questionnaire.builder()
                .title("Daily Check")
                .isActive(true)
                .version(2)
                .build();
        when(questionnaireRepository.findFirstByIsActiveTrueOrderByVersionDesc()).thenReturn(Optional.of(q));

        Optional<Questionnaire> result = questionnaireService.getActiveQuestionnaire();

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Daily Check");
    }

    @Test
    void shouldSaveQuestionnaireWithQuestions() {
        Question question = Question.builder()
                .id(UUID.randomUUID())
                .text("Do you have fever?")
                .type(QuestionType.YES_NO)
                .build();
        Questionnaire questionnaire = Questionnaire.builder()
                .title("Health Check")
                .questions(List.of(question))
                .build();
        Questionnaire saved = Questionnaire.builder().id(UUID.randomUUID()).title("Health Check").build();
        when(questionnaireRepository.save(any())).thenReturn(saved);

        Questionnaire result = questionnaireService.saveQuestionnaire(questionnaire);

        assertThat(result.getId()).isEqualTo(saved.getId());
        verify(questionnaireRepository).save(questionnaire);
    }

    @Test
    void shouldReturnEmptyWhenNoActiveQuestionnaire() {
        when(questionnaireRepository.findFirstByIsActiveTrueOrderByVersionDesc()).thenReturn(Optional.empty());

        Optional<Questionnaire> result = questionnaireService.getActiveQuestionnaire();

        assertThat(result).isEmpty();
    }
}
