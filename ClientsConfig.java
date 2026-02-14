package com.escoladoestudante.reco.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientsConfig {
  @Bean
  public WebClient openAiWebClient(@Value("${reco.openai.base-url}") String baseUrl,
                                   @Value("${reco.openai.api-key}") String apiKey) {
    var strategies = ExchangeStrategies.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
        .build();
    return WebClient.builder()
        .baseUrl(baseUrl)
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  public WebClient qdrantWebClient(@Value("${reco.qdrant.base-url}") String baseUrl) {
    var strategies = ExchangeStrategies.builder()
        .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
        .build();
    return WebClient.builder()
        .baseUrl(baseUrl)
        .exchangeStrategies(strategies)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
