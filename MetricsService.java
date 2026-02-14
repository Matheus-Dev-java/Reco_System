package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.entity.MetricEvent;
import com.escoladoestudante.reco.repo.MetricEventRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MetricsService {
  private final MetricEventRepository repo;

  public MetricsService(MetricEventRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public void record(String type, Long userId, Long contentId, String algo, boolean cacheHit, long openAiTokens) {
    var m = new MetricEvent();
    m.setType(type);
    m.setUserId(userId);
    m.setContentId(contentId);
    m.setAlgo(algo);
    m.setCacheHit(cacheHit);
    m.setOpenAiTokens(openAiTokens);
    repo.save(m);
  }

  public Dashboard snapshot24h(double usdPer1MTokens) {
    var since = Instant.now().minusSeconds(24 * 3600);
    var req = repo.countTypeSince("RECO_REQUEST", since);
    var hit = repo.cacheHitRate("RECO_REQUEST", since);
    var tokens = repo.tokensSince(since);
    long t = tokens == null ? 0L : tokens;
    double cost = (t / 1_000_000.0) * usdPer1MTokens;
    return new Dashboard(req, hit == null ? 0.0 : hit, t, cost);
  }

  public record Dashboard(long requests24h, double cacheHitRate24h, long tokens24h, double costUsd24h) {}
}
