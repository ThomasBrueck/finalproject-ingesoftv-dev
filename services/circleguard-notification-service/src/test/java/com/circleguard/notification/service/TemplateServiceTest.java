package com.circleguard.notification.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private Configuration freemarkerConfig;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void shouldRenderTemplate() throws Exception {
        ReflectionTestUtils.setField(templateService, "testingUrl", "https://example.com/testing");
        ReflectionTestUtils.setField(templateService, "isolationUrl", "https://example.com/isolation");
        ReflectionTestUtils.setField(templateService, "guidelinesDeepLink", "circleguard://guidelines");

        Template template = mock(Template.class);
        when(freemarkerConfig.getTemplate("health_alert.ftl")).thenReturn(template);
        doAnswer(invocation -> {
            Writer out = invocation.getArgument(1);
            out.write("Rendered content");
            return null;
        }).when(template).process(any(), any(Writer.class));

        String result = templateService.generateEmailContent("SUSPECT", "John");

        assertThat(result).isEqualTo("Rendered content");
    }

    @Test
    void shouldHandleMissingTemplate() throws Exception {
        ReflectionTestUtils.setField(templateService, "testingUrl", "https://example.com/testing");
        ReflectionTestUtils.setField(templateService, "isolationUrl", "https://example.com/isolation");
        ReflectionTestUtils.setField(templateService, "guidelinesDeepLink", "circleguard://guidelines");

        when(freemarkerConfig.getTemplate("health_alert.ftl")).thenThrow(new RuntimeException("Template not found"));

        String result = templateService.generateEmailContent("SUSPECT", "John");

        assertThat(result).contains("CircleGuard Health Update");
        assertThat(result).contains("SUSPECT");
    }

    @Test
    void shouldDefaultUserNameAndStatusWhenNull() throws Exception {
        ReflectionTestUtils.setField(templateService, "testingUrl", "https://example.com/testing");
        ReflectionTestUtils.setField(templateService, "isolationUrl", "https://example.com/isolation");

        Template template = mock(Template.class);
        when(freemarkerConfig.getTemplate("health_alert.ftl")).thenReturn(template);
        doAnswer(invocation -> {
            Writer out = invocation.getArgument(1);
            out.write("ok");
            return null;
        }).when(template).process(any(), any(Writer.class));

        assertThat(templateService.generateEmailContent(null, null)).isEqualTo("ok");
    }

    @Test
    void shouldGeneratePushContentPerStatus() {
        assertThat(templateService.generatePushContent("SUSPECT"))
                .contains("SUSPECT").contains("isolation");
        assertThat(templateService.generatePushContent("PROBABLE"))
                .contains("PROBABLE").contains("exposure");
        assertThat(templateService.generatePushContent("ACTIVE"))
                .contains("updated to ACTIVE");
    }

    @Test
    void shouldGeneratePushMetadataOnlyForRiskStatuses() {
        ReflectionTestUtils.setField(templateService, "guidelinesDeepLink", "circleguard://guidelines");

        assertThat(templateService.generatePushMetadata("SUSPECT"))
                .containsEntry("url", "circleguard://guidelines");
        assertThat(templateService.generatePushMetadata("PROBABLE"))
                .containsEntry("url", "circleguard://guidelines");
        assertThat(templateService.generatePushMetadata("ACTIVE")).isEmpty();
    }

    @Test
    void shouldGenerateSmsContent() {
        assertThat(templateService.generateSmsContent("CONFIRMED"))
                .contains("CONFIRMED").contains("CircleGuard Alert");
    }
}
