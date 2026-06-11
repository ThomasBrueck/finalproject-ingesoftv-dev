package com.circleguard.e2e.flow;

import com.circleguard.e2e.client.AuthServiceClient;
import com.circleguard.e2e.client.IdentityServiceClient;
import com.circleguard.e2e.client.HttpClientUtil;
import com.circleguard.e2e.config.E2ETestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IdentityFlowE2ETest extends E2ETestBase {

    private static AuthServiceClient auth;
    private static IdentityServiceClient identity;

    @BeforeAll
    static void init() {
        auth = new AuthServiceClient(authServiceUrl);
        identity = new IdentityServiceClient(identityServiceUrl);
    }

    @Test
    void shouldMapStudentIdentity() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();

        String realIdentity = "student-" + UUID.randomUUID().toString().substring(0, 8) + "@university.edu";

        JsonNode response = identity.mapIdentity(realIdentity, jwt);

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertNotNull(response.get("anonymousId"));
    }

    @Test
    void shouldRegisterVisitor() {
        String name = "Visitor-" + UUID.randomUUID().toString().substring(0, 6);
        String email = "visitor-" + UUID.randomUUID().toString().substring(0, 8) + "@external.com";

        JsonNode response = identity.registerVisitor(name, email);

        assertEquals(200, HttpClientUtil.getStatus(response));
        assertNotNull(response.get("anonymousId"));
    }

    @Test
    void shouldLookupIdentity() {
        JsonNode loginResponse = auth.login("staff_guard", "password");
        String jwt = loginResponse.get("token").asText();

        String realIdentity = "lookup-test-" + UUID.randomUUID().toString().substring(0, 8) + "@university.edu";

        JsonNode mapResponse = identity.mapIdentity(realIdentity, jwt);
        assertEquals(200, HttpClientUtil.getStatus(mapResponse));
        String anonymousId = mapResponse.get("anonymousId").asText();

        JsonNode lookupResponse = identity.lookupIdentity(anonymousId, jwt);

        assertEquals(200, HttpClientUtil.getStatus(lookupResponse));
        assertNotNull(lookupResponse.get("realIdentity"));
        assertEquals(realIdentity, lookupResponse.get("realIdentity").asText());
    }
}
