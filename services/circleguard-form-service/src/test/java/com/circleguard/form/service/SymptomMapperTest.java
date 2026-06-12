package com.circleguard.form.service;

import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.model.Question;
import com.circleguard.form.model.QuestionType;
import com.circleguard.form.model.Questionnaire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SymptomMapperTest {

    private final SymptomMapper mapper = new SymptomMapper();

    @Test
    void shouldDetectSymptomsFromFever() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Do you have a fever?")
                .type(QuestionType.YES_NO)
                .build();
        
        Questionnaire questionnaire = Questionnaire.builder()
                .questions(List.of(q))
                .build();
        
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "YES"))
                .build();
        
        assertTrue(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldNotDetectSymptomsWhenNo() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Do you have a fever?")
                .type(QuestionType.YES_NO)
                .build();
        
        Questionnaire questionnaire = Questionnaire.builder()
                .questions(List.of(q))
                .build();
        
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "NO"))
                .build();

        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldReturnFalseWhenResponsesNull() {
        HealthSurvey survey = HealthSurvey.builder().build();
        Questionnaire questionnaire = Questionnaire.builder().questions(List.of()).build();

        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldReturnFalseWhenQuestionnaireNull() {
        HealthSurvey survey = HealthSurvey.builder().responses(Map.of()).build();

        assertFalse(mapper.hasSymptoms(survey, null));
    }

    @Test
    void shouldReturnFalseWhenQuestionsNull() {
        HealthSurvey survey = HealthSurvey.builder().responses(Map.of()).build();

        assertFalse(mapper.hasSymptoms(survey, Questionnaire.builder().build()));
    }

    @Test
    void shouldIgnoreYesOnUnrelatedQuestion() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Did you travel recently?")
                .type(QuestionType.YES_NO)
                .build();
        Questionnaire questionnaire = Questionnaire.builder().questions(List.of(q)).build();
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "YES"))
                .build();

        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldDetectBreathingAndCoughKeywords() {
        UUID coughId = UUID.randomUUID();
        UUID breathId = UUID.randomUUID();
        Questionnaire questionnaire = Questionnaire.builder().questions(List.of(
                Question.builder().id(coughId).text("Do you have a cough?").type(QuestionType.YES_NO).build(),
                Question.builder().id(breathId).text("Trouble breathing?").type(QuestionType.YES_NO).build()
        )).build();

        assertTrue(mapper.hasSymptoms(HealthSurvey.builder()
                .responses(Map.of(coughId.toString(), "yes")).build(), questionnaire));
        assertTrue(mapper.hasSymptoms(HealthSurvey.builder()
                .responses(Map.of(breathId.toString(), "YES")).build(), questionnaire));
    }

    @Test
    void shouldIgnoreQuestionsWithoutResponse() {
        UUID answered = UUID.randomUUID();
        UUID unanswered = UUID.randomUUID();
        Questionnaire questionnaire = Questionnaire.builder().questions(List.of(
                Question.builder().id(answered).text("Do you have fever?").type(QuestionType.YES_NO).build(),
                Question.builder().id(unanswered).text("Do you have a cough?").type(QuestionType.YES_NO).build()
        )).build();
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(answered.toString(), "NO"))
                .build();

        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldDetectSelectionOnChoiceSymptomQuestion() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Select your symptoms")
                .type(QuestionType.MULTI_CHOICE)
                .build();
        Questionnaire questionnaire = Questionnaire.builder().questions(List.of(q)).build();
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "[\"headache\"]"))
                .build();

        assertTrue(mapper.hasSymptoms(survey, questionnaire));
    }

    @Test
    void shouldIgnoreEmptyChoiceSelection() {
        UUID questionId = UUID.randomUUID();
        Question q = Question.builder()
                .id(questionId)
                .text("Select your symptoms")
                .type(QuestionType.MULTI_CHOICE)
                .build();
        Questionnaire questionnaire = Questionnaire.builder().questions(List.of(q)).build();
        HealthSurvey survey = HealthSurvey.builder()
                .responses(Map.of(questionId.toString(), "[]"))
                .build();

        assertFalse(mapper.hasSymptoms(survey, questionnaire));
    }
}
