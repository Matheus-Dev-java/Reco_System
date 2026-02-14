package com.escoladoestudante.reco.service;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class RedisCacheService {
  private final @Qualifier("stringRedisTemplate") RedisTemplate<String, String> redis;
  private final Duration l1;
  private final Duration l2;
  private final Duration l3;

  public RedisCacheService(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redis, org.springframework.core.env.Environment env) {
    this.redis = redis;
    this.l1 = Duration.ofSeconds(env.getProperty("reco.cache.l1-ttl-seconds", Long.class, 300L));
    this.l2 = Duration.ofSeconds(env.getProperty("reco.cache.l2-ttl-seconds", Long.class, 86400L));
    this.l3 = Duration.ofSeconds(env.getProperty("reco.cache.l3-ttl-seconds", Long.class, 3600L));
  }

  public Optional<String> get(String key) {
    return Optional.ofNullable(redis.opsForValue().get(key));
  }

  public void setL1(String key, String value) { redis.opsForValue().set(key, value, l1); }
  public void setL2(String key, String value) { redis.opsForValue().set(key, value, l2); }
  public void setL3(String key, String value) { redis.opsForValue().set(key, value, l3); }
  public void del(String key) { redis.delete(key); }
}
