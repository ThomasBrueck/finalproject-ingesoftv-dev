package com.circleguard.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración centralizada de clientes HTTP.
 * Registra RestTemplate como un bean de Spring para permitir su inyección
 * de dependencias, configuración centralizada y reemplazo por mocks en pruebas.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
