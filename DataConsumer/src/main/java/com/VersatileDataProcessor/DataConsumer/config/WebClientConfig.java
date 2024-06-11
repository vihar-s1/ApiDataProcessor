package com.VersatileDataProcessor.DataConsumer.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value(value = "${database.endpoint.add}")
    private String ESAddEndPoint;

    @Bean
    public WebClient.Builder getWebClientBuilder() {
        return WebClient.builder()
                .baseUrl(ESAddEndPoint)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
