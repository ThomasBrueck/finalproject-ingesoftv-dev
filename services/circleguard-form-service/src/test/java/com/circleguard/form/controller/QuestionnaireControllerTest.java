package com.circleguard.form.controller;

import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.service.QuestionnaireService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QuestionnaireController.class)
class QuestionnaireControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionnaireService questionnaireService;

    @Test
    void shouldGetActiveQuestionnaire() throws Exception {
        Questionnaire q = Questionnaire.builder()
                .title("Daily Check")
                .isActive(true)
                .version(1)
                .build();
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.of(q));

        mockMvc.perform(get("/api/v1/questionnaires/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Daily Check"));
    }

    @Test
    void shouldReturn404WhenNoActiveQuestionnaire() throws Exception {
        when(questionnaireService.getActiveQuestionnaire()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/questionnaires/active"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateQuestionnaireFromRequest() throws Exception {
        Questionnaire saved = Questionnaire.builder()
                .title("New Q")
                .version(1)
                .isActive(false)
                .build();
        when(questionnaireService.saveQuestionnaire(any(Questionnaire.class))).thenReturn(saved);

        mockMvc.perform(post("/api/v1/questionnaires")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New Q\",\"description\":\"d\",\"version\":1,\"isActive\":false,"
                        + "\"questions\":[{\"text\":\"Q1\",\"type\":\"YES_NO\",\"options\":null,\"orderIndex\":0}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Q"));
    }
}
