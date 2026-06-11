package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.FileServiceClient;
import com.circleguard.e2e.client.FormServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HealthSurveyFlowE2ETest extends E2ETestBase {

    private static AuthServiceClient auth;
    private static FormServiceClient form;
    private static FileServiceClient file;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
        form = new FormServiceClient(formServiceUrl);
        file = new FileServiceClient(fileServiceUrl);
    }

    @Test
    void ca21_getActiveQuestionnaire() {
        JsonNode response = form.getActiveQuestionnaire();

        assertEquals(200, HttpClientUtil.getStatus(response));
    }

    @Test
    void ca22_submitSymptomaticSurveyWithAttachment() {
        UUID anonymousId = UUID.randomUUID();
        JsonNode response = form.submitSurvey(anonymousId, true, true, "headache", "/uploads/test.pdf");

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertEquals(anonymousId.toString(), response.get("anonymousId").asText());
    }

    @Test
    void ca23_uploadFile() {
        byte[] content = "Dummy certificate content for E2E test".getBytes();

        JsonNode response = file.uploadFile(content, "certificate.pdf", null);

        assertEquals(200, HttpClientUtil.getStatus(response));
    }

    @Test
    void ca24_adminValidatesPendingCertificate() {
        JsonNode loginResponse = auth.login("health_user", "password");
        String jwt = loginResponse.get("token").asText();
        UUID anonymousId = UUID.randomUUID();

        form.submitSurvey(anonymousId, true, false, null, "/uploads/doctor-note.pdf");

        JsonNode pending = form.listCertificatesPending();
        assertNotNull(pending);

        if (pending.isArray() && pending.size() > 0) {
            String certId = pending.get(0).get("id").asText();
            JsonNode validation = form.validateCertificate(certId, jwt);
            assertEquals(200, HttpClientUtil.getStatus(validation));
        }
    }
}
