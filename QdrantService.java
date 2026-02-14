package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.util.JsonUtil;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class QdrantService {
  private final WebClient qdrant;
  private final String collection;
  private final int vectorSize;

  public QdrantService(WebClient qdrantWebClient,
                       @Value("${reco.qdrant.collection}") String collection,
                       @Value("${reco.qdrant.vector-size}") int vectorSize) {
    this.qdrant = qdrantWebClient;
    this.collection = collection;
    this.vectorSize = vectorSize;
  }

  public void ensureCollection() {
    var get = qdrant.get().uri("/collections/{c}", collection)
        .retrieve()
        .bodyToMono(String.class)
        .onErrorReturn("")
        .block();
    if (get != null && get.contains("\"status\":\"ok\"")) return;

    var body = Map.of("vectors", Map.of("size", vectorSize, "distance", "Cosine"));
    qdrant.put().uri("/collections/{c}", collection)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(String.class)
        .block();
  }

  public void upsertPoint(long id, List<Double> vector, Map<String, Object> payload) {
    var body = Map.of("points", List.of(Map.of("id", id, "vector", vector, "payload", payload)));
    qdrant.put().uri("/collections/{c}/points?wait=true", collection)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(String.class)
        .block();
  }

  @Cacheable(cacheNames = "qdrantPointLocal", key = "'p:' + #id")
  public Point getPoint(long id) {
    var resp = qdrant.get().uri("/collections/{c}/points/{id}?with_vector=true&with_payload=true", collection, id)
        .retrieve()
        .bodyToMono(String.class)
        .block();
    if (resp == null) throw new IllegalStateException("Qdrant get point returned null");
    var parsed = JsonUtil.fromJson(resp, Map.class);
    var result = (Map<?, ?>) parsed.get("result");
    if (result == null) throw new IllegalStateException("Qdrant missing result");
    var vector = (List<Double>) result.get("vector");
    var payload = (Map<String, Object>) result.get("payload");
    return new Point(id, vector, payload);
  }

  public List<ScoredPoint> search(List<Double> vector, int limit, Map<String, Object> filter) {
    var body = Map.of("vector", vector, "limit", limit, "with_vector", true, "with_payload", true, "filter", filter);
    var resp = qdrant.post().uri("/collections/{c}/points/search", collection)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(String.class)
        .block();
    if (resp == null) throw new IllegalStateException("Qdrant search returned null");
    var parsed = JsonUtil.fromJson(resp, Map.class);
    var result = (List<Map<String, Object>>) parsed.get("result");
    if (result == null) return List.of();
    return result.stream().map(m -> {
      var id = ((Number) m.get("id")).longValue();
      var score = ((Number) m.get("score")).doubleValue();
      var vec = (List<Double>) m.get("vector");
      var payload = (Map<String, Object>) m.get("payload");
      return new ScoredPoint(id, score, vec, payload);
    }).toList();
  }

  public record Point(long id, List<Double> vector, Map<String, Object> payload) {}
  public record ScoredPoint(long id, double score, List<Double> vector, Map<String, Object> payload) {}
}
