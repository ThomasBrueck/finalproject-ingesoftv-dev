package com.circleguard.e2e.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

public abstract class E2ETestBase {

    private static DockerComposeContainer<?> environment;
    private static boolean ciMode = false;

    protected static String authServiceUrl;
    protected static String identityServiceUrl;
    protected static String promotionServiceUrl;
    protected static String notificationServiceUrl;
    protected static String formServiceUrl;
    protected static String gatewayServiceUrl;
    protected static String dashboardServiceUrl;
    protected static String fileServiceUrl;

    @BeforeAll
    static void setupEnvironment() {
        String ciAuth = System.getenv("AUTH_SERVICE_URL");
        if (ciAuth != null && !ciAuth.isEmpty()) {
            ciMode = true;
            authServiceUrl = ciAuth;
            identityServiceUrl = System.getenv("IDENTITY_SERVICE_URL");
            promotionServiceUrl = System.getenv("PROMOTION_SERVICE_URL");
            notificationServiceUrl = System.getenv("NOTIFICATION_SERVICE_URL");
            formServiceUrl = System.getenv("FORM_SERVICE_URL");
            gatewayServiceUrl = System.getenv("GATEWAY_SERVICE_URL");
            dashboardServiceUrl = System.getenv("DASHBOARD_SERVICE_URL");
            fileServiceUrl = System.getenv("FILE_SERVICE_URL");
            logUrls();
            return;
        }

        File composeFile = new File("docker-compose-e2e.yml");

        environment = new DockerComposeContainer<>(composeFile)
                .withLocalCompose(true)
                .withOptions("--compatibility")
                .withExposedService("postgres", 5432,
                        Wait.forLogMessage(".*database system is ready to accept connections.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("neo4j", 7687,
                        Wait.forLogMessage(".*Started.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("redis", 6379,
                        Wait.forLogMessage(".*Ready to accept connections.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(30)))
                .withExposedService("kafka", 9092,
                        Wait.forLogMessage(".*started.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(60)))
                .withExposedService("openldap", 389,
                        Wait.forLogMessage(".*slapd starting.*", 1)
                                .withStartupTimeout(Duration.ofSeconds(30)))
                .withExposedService("auth-service", 8180,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("identity-service", 8083,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("promotion-service", 8088,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("notification-service", 8082,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("form-service", 8086,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("gateway-service", 8087,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("dashboard-service", 8084,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)))
                .withExposedService("file-service", 8085,
                        Wait.forHttp("/actuator/health").forStatusCode(200)
                                .withStartupTimeout(Duration.ofSeconds(120)));

        environment.start();

        authServiceUrl = "http://" + environment.getServiceHost("auth-service", 8180)
                + ":" + environment.getServicePort("auth-service", 8180);
        identityServiceUrl = "http://" + environment.getServiceHost("identity-service", 8083)
                + ":" + environment.getServicePort("identity-service", 8083);
        promotionServiceUrl = "http://" + environment.getServiceHost("promotion-service", 8088)
                + ":" + environment.getServicePort("promotion-service", 8088);
        notificationServiceUrl = "http://" + environment.getServiceHost("notification-service", 8082)
                + ":" + environment.getServicePort("notification-service", 8082);
        formServiceUrl = "http://" + environment.getServiceHost("form-service", 8086)
                + ":" + environment.getServicePort("form-service", 8086);
        gatewayServiceUrl = "http://" + environment.getServiceHost("gateway-service", 8087)
                + ":" + environment.getServicePort("gateway-service", 8087);
        dashboardServiceUrl = "http://" + environment.getServiceHost("dashboard-service", 8084)
                + ":" + environment.getServicePort("dashboard-service", 8084);
        fileServiceUrl = "http://" + environment.getServiceHost("file-service", 8085)
                + ":" + environment.getServicePort("file-service", 8085);

        logUrls();
    }

    @AfterAll
    static void teardownEnvironment() {
        if (!ciMode && environment != null) {
            environment.stop();
        }
    }

    private static void logUrls() {
        System.out.println("=== E2E Test Environment Ready (" + (ciMode ? "CI" : "Testcontainers") + " mode) ===");
        System.out.println("auth-service:        " + authServiceUrl);
        System.out.println("identity-service:    " + identityServiceUrl);
        System.out.println("promotion-service:   " + promotionServiceUrl);
        System.out.println("notification-service:" + notificationServiceUrl);
        System.out.println("form-service:        " + formServiceUrl);
        System.out.println("gateway-service:     " + gatewayServiceUrl);
        System.out.println("dashboard-service:   " + dashboardServiceUrl);
        System.out.println("file-service:        " + fileServiceUrl);
        System.out.println("==================================");
    }
}
