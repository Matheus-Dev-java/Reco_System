package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.util.Hashes;
import com.escoladoestudante.reco.util.JsonUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class EmbeddingService {
  private final WebClient openai;
  private final RedisCacheService redisCache;
  private final MetricsService metrics;
  private final String model;
  private final int maxChars;
  private final String apiKey;
  private final int vectorSize;

  public EmbeddingService(WebClient openAiWebClient,
                          RedisCacheService redisCache,
                          MetricsService metrics,
                          @Value("${reco.openai.embedding-model}") String model,
                          @Value("${reco.openai.max-chars}") int maxChars,
                          @Value("${reco.openai.api-key:}") String apiKey,
                          @Value("${reco.embedding.vector-size:3072}") int vectorSize) {
    this.openai = openAiWebClient;
    this.redisCache = redisCache;
    this.metrics = metrics;
    this.model = model;
    this.maxChars = maxChars;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.vectorSize = vectorSize;
  }

  public EmbeddingResult embeddingForText(String text, Long userIdForMetrics) {
    var trimmed = text == null ? "" : text;
    if (trimmed.length() > maxChars) trimmed = trimmed.substring(0, maxChars);
    var hash = Hashes.sha256("emb:" + trimmed);
    var key = "l2:emb:" + hash;

    var cached = redisCache.get(key);
    if (cached.isPresent()) {
      var r = JsonUtil.fromJson(cached.get(), EmbeddingResult.class);
      metrics.record("EMBEDDING", userIdForMetrics, null, model, true, 0);
      return r;
    }

    var local = embeddingLocal(trimmed);
    redisCache.setL2(key, JsonUtil.toJson(local));
    metrics.record("EMBEDDING", userIdForMetrics, null, model, false, local.estimatedTokens());
    return local;
  }

  @Cacheable(cacheNames = "embeddingLocal", key = "T(com.escoladoestudante.reco.util.Hashes).sha256('loc:' + #text)")
  public EmbeddingResult embeddingLocal(String text) {
    // If no OpenAI key is configured, generate a deterministic local embedding.
    if (apiKey.isBlank()) {
      return deterministicEmbedding(text);
    }
    var req = Map.of("model", model, "input", text);

    var resp = openai.post()
        .uri("/embeddings")
        .bodyValue(req)
        .retrieve()
        .bodyToMono(OpenAiEmbeddingResponse.class)
        .block();

    if (resp == null || resp.data == null || resp.data.isEmpty()) throw new IllegalStateException("Empty embeddings response");
    var v = resp.data.getFirst().embedding;
    long estTokens = Math.max(1L, (long) Math.ceil(text.length() / 4.0));
    return new EmbeddingResult(v, estTokens, Instant.now().toString());
  }

  private EmbeddingResult deterministicEmbedding(String text) {
    // Stable pseudo-random vector from SHA-256(text). Useful for running locally without OpenAI.
    byte[] seedBytes;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      seedBytes = md.digest(("seed:" + text).getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    long seed = ByteBuffer.wrap(seedBytes, 0, 8).getLong();
    var rng = new java.util.SplittableRandom(seed);
    var vec = new java.util.ArrayList<Double>(vectorSize);
    double norm2 = 0.0;
    for (int i = 0; i < vectorSize; i++) {
      // [-1, 1]
      double v = rng.nextDouble() * 2.0 - 1.0;
      vec.add(v);
      norm2 += v * v;
    }
    double norm = Math.sqrt(Math.max(1e-12, norm2));
    for (int i = 0; i < vec.size(); i++) {
      vec.set(i, vec.get(i) / norm);
    }
    return new EmbeddingResult(vec, 0, Instant.now().toString());
  }

  public record EmbeddingResult(List<Double> vector, long estimatedTokens, String at) {}

  public static class OpenAiEmbeddingResponse {
    public List<Datum> data;
    public static class Datum { public List<Double> embedding; }
  }
}
