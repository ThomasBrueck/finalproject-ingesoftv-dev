package com.circleguard.form.service;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.repository.QuestionnaireRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSeederTest {

    @Mock
    private QuestionnaireRepository questionnaireRepository;

    @Test
    void seedQuestionnaire_ShouldSeedDefaultWhenEmpty() {
        when(questionnaireRepository.count()).thenReturn(0L);
        when(questionnaireRepository.save(any(Questionnaire.class))).thenAnswer(i -> i.getArguments()[0]);

        new DatabaseSeeder(questionnaireRepository).seedQuestionnaire();

        ArgumentCaptor<Questionnaire> captor = ArgumentCaptor.forClass(Questionnaire.class);
        verify(questionnaireRepository).save(captor.capture());
        Questionnaire seeded = captor.getValue();
        assertEquals("Default Health Survey", seeded.getTitle());
        assertTrue(seeded.getIsActive());
        assertEquals(2, seeded.getQuestions().size());
        // Las preguntas quedan ligadas al cuestionario (relación bidireccional)
        seeded.getQuestions().forEach(q -> assertEquals(seeded, q.getQuestionnaire()));
    }

    @Test
    void seedQuestionnaire_ShouldSkipWhenDataExists() {
        when(questionnaireRepository.count()).thenReturn(3L);

        new DatabaseSeeder(questionnaireRepository).seedQuestionnaire();

        verify(questionnaireRepository, never()).save(any());
    }

    @Test
    void seedQuestionnaire_ShouldSwallowErrors() {
        when(questionnaireRepository.count()).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> new DatabaseSeeder(questionnaireRepository).seedQuestionnaire());
    }
}
