package com.escoladoestudante.reco.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
    var t = new RedisTemplate<String, String>();
    t.setConnectionFactory(cf);
    var s = new StringRedisSerializer();
    t.setKeySerializer(s);
    t.setValueSerializer(s);
    t.setHashKeySerializer(s);
    t.setHashValueSerializer(s);
    t.afterPropertiesSet();
    return t;
  }
}
