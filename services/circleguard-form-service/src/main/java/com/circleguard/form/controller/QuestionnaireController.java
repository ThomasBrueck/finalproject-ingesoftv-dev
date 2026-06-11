package com.circleguard.form.controller;

import com.circleguard.form.dto.QuestionnaireRequest;
import com.circleguard.form.model.Question;
import com.circleguard.form.model.Questionnaire;
import com.circleguard.form.service.QuestionnaireService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/questionnaires")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RequiredArgsConstructor
public class QuestionnaireController {
    private final QuestionnaireService service;

    @GetMapping
    public ResponseEntity<List<Questionnaire>> getAll() {
        return ResponseEntity.ok(service.getAllQuestionnaires());
    }

    @GetMapping("/active")
    public ResponseEntity<Questionnaire> getActive() {
        return service.getActiveQuestionnaire()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Questionnaire> create(@RequestBody QuestionnaireRequest request) {
        Questionnaire questionnaire = Questionnaire.builder()
                .title(request.title())
                .description(request.description())
                .version(request.version())
                .isActive(request.isActive())
                .build();
        if (request.questions() != null) {
            questionnaire.setQuestions(request.questions().stream()
                    .map(q -> Question.builder()
                            .text(q.text())
                            .type(q.type())
                            .options(q.options())
                            .orderIndex(q.orderIndex())
                            .build())
                    .collect(Collectors.toList()));
        }
        return ResponseEntity.ok(service.saveQuestionnaire(questionnaire));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        service.activateQuestionnaire(id);
        return ResponseEntity.ok().build();
    }
}
