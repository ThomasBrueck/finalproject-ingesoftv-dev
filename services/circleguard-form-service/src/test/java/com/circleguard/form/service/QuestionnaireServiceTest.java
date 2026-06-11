package com.circleguard.form.service;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
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
    void shouldReturnEmptyWhenNoActiveQuestionnaire() {
        when(questionnaireRepository.findFirstByIsActiveTrueOrderByVersionDesc()).thenReturn(Optional.empty());

        Optional<Questionnaire> result = questionnaireService.getActiveQuestionnaire();

        assertThat(result).isEmpty();
    }
}
