package com.circleguard.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000); // 2 segundos
        requestFactory.setReadTimeout(2000);    // 2 segundos
        return new RestTemplate(requestFactory);
    }
}
