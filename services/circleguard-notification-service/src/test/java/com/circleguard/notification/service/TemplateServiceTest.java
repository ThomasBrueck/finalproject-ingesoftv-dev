package com.circleguard.notification.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(FreeMarkerTemplateUtils.processTemplateIntoString(eq(template), any())).thenReturn("Rendered content");

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
}
