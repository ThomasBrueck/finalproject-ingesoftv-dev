package com.circleguard.e2e.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public class FormServiceClient {

    private final String baseUrl;

    public FormServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public JsonNode getActiveQuestionnaire() {
        return HttpClientUtil.get(baseUrl + "/api/v1/questionnaires/active", null);
    }

    public JsonNode submitSurvey(UUID anonymousId, boolean hasFever, boolean hasCough,
                                  String otherSymptoms, String attachmentPath) {
        return HttpClientUtil.post(baseUrl + "/api/v1/surveys",
                Map.of("anonymousId", anonymousId.toString(), "hasFever", hasFever,
                        "hasCough", hasCough, "otherSymptoms", otherSymptoms != null ? otherSymptoms : "",
                        "attachmentPath", attachmentPath != null ? attachmentPath : ""),
                null);
    }

    public JsonNode listCertificatesPending() {
        return HttpClientUtil.get(baseUrl + "/api/v1/certificates/pending", null);
    }

    public JsonNode validateCertificate(String certificateId, String jwt) {
        return HttpClientUtil.post(baseUrl + "/api/v1/certificates/" + certificateId + "/validate",
                Map.of("valid", true, "notes", "Validated by E2E test"), jwt);
    }
}
