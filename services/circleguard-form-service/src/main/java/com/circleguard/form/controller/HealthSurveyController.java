package com.circleguard.form.controller;

import com.circleguard.form.dto.HealthSurveyRequest;
import com.circleguard.form.model.HealthSurvey;
import com.circleguard.form.service.HealthSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/surveys")
@CrossOrigin(origins = "${app.cors.allowed-origins:http://localhost:3000}")
@RequiredArgsConstructor
public class HealthSurveyController {
    private final HealthSurveyService surveyService;

    @PostMapping
    public ResponseEntity<HealthSurvey> submit(@RequestBody HealthSurveyRequest request) {
        HealthSurvey survey = HealthSurvey.builder()
                .anonymousId(request.anonymousId())
                .hasFever(request.hasFever())
                .hasCough(request.hasCough())
                .otherSymptoms(request.otherSymptoms())
                .exposureDate(request.exposureDate())
                .responses(request.responses())
                .attachmentPath(request.attachmentPath())
                .build();
        return ResponseEntity.ok(surveyService.submitSurvey(survey));
    }
}
