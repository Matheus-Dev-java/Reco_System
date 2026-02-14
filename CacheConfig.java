package com.escoladoestudante.reco.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
  @Bean
  public CacheManager caffeineCacheManager() {
    var mgr = new CaffeineCacheManager("embeddingLocal", "qdrantPointLocal");
    mgr.setCaffeine(Caffeine.newBuilder()
        .maximumSize(50_000)
        .expireAfterAccess(Duration.ofHours(6)));
    return mgr;
  }
}
