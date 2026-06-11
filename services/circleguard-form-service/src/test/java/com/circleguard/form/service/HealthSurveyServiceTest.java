package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.model.ValidationStatus;
import com.circleguard.form.repository.HealthSurveyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class HealthSurveyServiceTest {

    @Autowired
    private HealthSurveyService surveyService;

    @MockBean
    private HealthSurveyRepository repository;

    @MockBean
    private QuestionnaireService questionnaireService;

    @MockBean
    private SymptomMapper symptomMapper;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void shouldSubmitSurveySuccessfully() {
        Questionnaire q = Questionnaire.builder().isActive(true).version(1).build();
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(q));
        when(symptomMapper.hasSymptoms(any(), any())).thenReturn(false);

        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder().anonymousId(anonymousId).build();
        HealthSurvey saved = HealthSurvey.builder().id(UUID.randomUUID()).anonymousId(anonymousId).build();
        when(repository.save(any())).thenReturn(saved);

        HealthSurvey result = surveyService.submitSurvey(survey);

        verify(repository).save(any());
        verify(kafkaTemplate).send(eq("survey.submitted"), anyString(), any(Map.class));
    }

    @Test
    void shouldSubmitSurveyWithSymptoms() {
        Questionnaire q = Questionnaire.builder().isActive(true).version(1).build();
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(q));
        when(symptomMapper.hasSymptoms(any(), any())).thenReturn(true);

        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder().anonymousId(anonymousId).build();
        HealthSurvey saved = HealthSurvey.builder().id(UUID.randomUUID()).anonymousId(anonymousId).build();
        when(repository.save(any())).thenReturn(saved);

        surveyService.submitSurvey(survey);

        verify(repository).save(argThat(s ->
                Boolean.TRUE.equals(s.getHasFever()) && Boolean.TRUE.equals(s.getHasCough())));
        verify(kafkaTemplate).send(eq("survey.submitted"), anyString(), argThat((Map<String, Object> m) ->
                Boolean.TRUE.equals(m.get("hasSymptoms"))));
    }

    @Test
    void shouldSetPendingValidationWhenAttachmentPresent() {
        Questionnaire q = Questionnaire.builder().isActive(true).version(1).build();
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(q));
        when(symptomMapper.hasSymptoms(any(), any())).thenReturn(false);

        UUID anonymousId = UUID.randomUUID();
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(anonymousId)
                .attachmentPath("/tmp/file.pdf")
                .build();
        HealthSurvey saved = HealthSurvey.builder().id(UUID.randomUUID()).anonymousId(anonymousId).build();
        when(repository.save(any())).thenReturn(saved);

        surveyService.submitSurvey(survey);

        verify(repository).save(argThat(s ->
                s.getAttachmentPath() != null && ValidationStatus.PENDING == s.getValidationStatus()));
    }

    @Test
    void shouldThrowWhenSurveyNotFound() {
        UUID surveyId = UUID.randomUUID();
        when(repository.findById(surveyId)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                surveyService.validateSurvey(surveyId, ValidationStatus.APPROVED, UUID.randomUUID()));
    }
}
