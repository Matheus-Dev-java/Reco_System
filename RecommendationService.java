package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.api.dto.RecommendationItem;
import com.escoladoestudante.reco.api.dto.RecommendationResponse;
import com.escoladoestudante.reco.entity.Content;
import com.escoladoestudante.reco.entity.Interaction;
import com.escoladoestudante.reco.repo.ContentRepository;
import com.escoladoestudante.reco.repo.InteractionRepository;
import com.escoladoestudante.reco.util.Hashes;
import com.escoladoestudante.reco.util.JsonUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {
  private final InteractionRepository interactions;
  private final ContentRepository contents;
  private final QdrantService qdrant;
  private final RedisCacheService redis;
  private final AbTestingService ab;
  private final MetricsService metrics;
  private final ContentService contentService;

  private final int topk;
  private final int candidatePool;
  private final int maxPerCategory;
  private final double halfLifeDays;
  private final double implicitLikeReadRatio;

  public RecommendationService(InteractionRepository interactions,
                              ContentRepository contents,
                              QdrantService qdrant,
                              RedisCacheService redis,
                              AbTestingService ab,
                              MetricsService metrics,
                              ContentService contentService,
                              @Value("${reco.recommender.topk}") int topk,
                              @Value("${reco.recommender.candidate-pool}") int candidatePool,
                              @Value("${reco.recommender.max-per-category}") int maxPerCategory,
                              @Value("${reco.recommender.decay-half-life-days}") double halfLifeDays,
                              @Value("${reco.recommender.implicit-like-read-ratio}") double implicitLikeReadRatio) {
    this.interactions = interactions;
    this.contents = contents;
    this.qdrant = qdrant;
    this.redis = redis;
    this.ab = ab;
    this.metrics = metrics;
    this.contentService = contentService;
    this.topk = topk;
    this.candidatePool = candidatePool;
    this.maxPerCategory = maxPerCategory;
    this.halfLifeDays = halfLifeDays;
    this.implicitLikeReadRatio = implicitLikeReadRatio;
  }

  public RecommendationResponse recommendForUser(long userId) {
    var algo = ab.algoForBucket(ab.bucketForUser(userId));
    var key = "l1:reco:u:" + userId + ":a:" + algo;
    var cached = redis.get(key);
    if (cached.isPresent()) {
      var items = JsonUtil.fromJson(cached.get(), RecommendationItem[].class);
      metrics.record("RECO_REQUEST", userId, null, algo, true, 0);
      return new RecommendationResponse(userId, algo, true, List.of(items));
    }

    var rec = compute(userId, algo);
    redis.setL1(key, JsonUtil.toJson(rec.items()));
    metrics.record("RECO_REQUEST", userId, null, algo, false, 0);
    return rec;
  }

  public RecommendationResponse moreLikeThis(long contentId, long userId) {
    var algo = "COSINE";
    var key = "l3:morelike:" + contentId + ":a:" + algo;
    var cached = redis.get(key);
    if (cached.isPresent()) {
      var items = JsonUtil.fromJson(cached.get(), RecommendationItem[].class);
      metrics.record("MORE_LIKE_THIS", userId, contentId, algo, true, 0);
      return new RecommendationResponse(userId, algo, true, List.of(items));
    }

    var v = contentVector(contentId);
    var res = qdrant.search(v, candidatePool, Map.of());
    var reranked = diversifyAndExplain(userId, algo, res, Set.of(contentId));
    var top = reranked.stream().limit(topk).toList();
    redis.setL3(key, JsonUtil.toJson(top));
    metrics.record("MORE_LIKE_THIS", userId, contentId, algo, false, 0);
    return new RecommendationResponse(userId, algo, false, top);
  }

  private RecommendationResponse compute(long userId, String algo) {
    var recent = interactions.findRecentByUser(userId);
    if (recent.isEmpty()) return new RecommendationResponse(userId, algo, false, coldStart());

    var seen = recent.stream().map(Interaction::getContentId).collect(java.util.stream.Collectors.toSet());
    var userVector = buildUserVector(recent);

    var qKey = "l3:q:" + Hashes.sha256(algo + ":" + quantizedKey(userVector));
    var cachedQ = redis.get(qKey);
    List<QdrantService.ScoredPoint> candidates;
    if (cachedQ.isPresent()) {
      var ids = JsonUtil.fromJson(cachedQ.get(), long[].class);
      candidates = Arrays.stream(ids)
          .mapToObj(this::safeGetPoint)
          .map(p -> new QdrantService.ScoredPoint(p.id(), 0.0, p.vector(), p.payload()))
          .toList();
    } else {
      candidates = qdrant.search(userVector, candidatePool, Map.of());
      var ids = candidates.stream().mapToLong(QdrantService.ScoredPoint::id).toArray();
      redis.setL3(qKey, JsonUtil.toJson(ids));
    }

    var reranked = diversifyAndExplain(userId, algo, candidates, seen);
    var top = reranked.stream().limit(topk).toList();
    return new RecommendationResponse(userId, algo, false, top);
  }

  private List<RecommendationItem> coldStart() {
    var since = Instant.now().minus(Duration.ofDays(7));
    var top = interactions.topContentSince(since).stream().limit(topk).map(r -> ((Number) r[0]).longValue()).toList();
    if (top.isEmpty()) {
      return contents.findAll().stream().limit(topk)
          .map(c -> new RecommendationItem(c.getId(), 0.0, c.getCategory(), c.getTitle(), "Cold start: popular"))
          .toList();
    }
    var map = contents.findAllById(top).stream().collect(java.util.stream.Collectors.toMap(Content::getId, c -> c));
    return top.stream().map(id -> {
      var c = map.get(id);
      if (c == null) return new RecommendationItem(id, 0.0, "unknown", "unknown", "Cold start: popular");
      return new RecommendationItem(id, 0.0, c.getCategory(), c.getTitle(), "Cold start: popular");
    }).toList();
  }

  private List<Double> buildUserVector(List<Interaction> recent) {
    var base = contentVector(recent.getFirst().getContentId());
    var acc = VectorMath.zeros(base.size());
    var now = Instant.now();
    var exec = Executors.newVirtualThreadPerTaskExecutor();
    try {
      var tasks = recent.stream().limit(80).map(i -> exec.submit(() -> {
        var v = contentVector(i.getContentId());
        var w = weight(i, now);
        return new Pair(v, w);
      })).toList();

      double wsum = 0.0;
      for (var f : tasks) {
        var p = f.get();
        VectorMath.addScaled(acc, p.v, p.w);
        wsum += p.w;
      }
      if (wsum > 0.0) VectorMath.scale(acc, 1.0 / wsum);
      return acc;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      exec.close();
    }
  }

  private double weight(Interaction i, Instant now) {
    var ageDays = Math.max(0.0, Duration.between(i.getCreatedAt(), now).toSeconds() / 86400.0);
    var decay = Math.pow(0.5, ageDays / halfLifeDays);

    var kind = com.escoladoestudante.reco.domain.InteractionKind.fromCode(i.getKind());
    double base = switch (kind) {
      case com.escoladoestudante.reco.domain.InteractionKind.Like __ -> 3.0;
      case com.escoladoestudante.reco.domain.InteractionKind.Bookmark __ -> 2.5;
      case com.escoladoestudante.reco.domain.InteractionKind.Share __ -> 2.0;
      case com.escoladoestudante.reco.domain.InteractionKind.View __ -> 1.0;
    };

    if ("VIEW".equals(i.getKind()) && i.getValue() >= implicitLikeReadRatio) base = 2.0;
    return base * decay;
  }

  private List<Double> contentVector(long contentId) {
    var key = "l2:emb:content:" + contentId;
    var cached = redis.get(key);
    if (cached.isPresent()) return JsonUtil.fromJson(cached.get(), List.class);
    var p = safeGetPoint(contentId);
    redis.setL2(key, JsonUtil.toJson(p.vector()));
    return p.vector();
  }

  private QdrantService.Point safeGetPoint(long contentId) {
    try {
      return qdrant.getPoint(contentId);
    } catch (Exception e) {
      // Flyway seeds insert rows in Postgres without creating the corresponding Qdrant points.
      // If a point is missing, index it on the fly and retry.
      contentService.indexExisting(contentId);
      return qdrant.getPoint(contentId);
    }
  }

  private List<RecommendationItem> diversifyAndExplain(long userId, String algo, List<QdrantService.ScoredPoint> candidates, Set<Long> seen) {
    var pool = candidates.stream().filter(c -> !seen.contains(c.id())).limit(candidatePool).toList();
    if (pool.isEmpty()) return List.of();

    var recent = interactions.findRecentByUser(userId).stream().limit(20).toList();
    var recentIds = recent.stream().map(Interaction::getContentId).distinct().limit(10).toList();
    var recentVecs = recentIds.stream().map(this::contentVector).toList();
    var userVec = average(recentVecs);

    var contentMap = contents.findAllById(pool.stream().map(QdrantService.ScoredPoint::id).toList()).stream()
        .collect(java.util.stream.Collectors.toMap(Content::getId, c -> c));

    record Scored(RecommendationItem item, List<Double> vec) {}
    var exec = Executors.newVirtualThreadPerTaskExecutor();
    try {
      var computed = pool.stream().map(p -> exec.submit(() -> {
        var c = contentMap.get(p.id());
        var cat = c == null ? "unknown" : c.getCategory();
        var title = c == null ? "unknown" : c.getTitle();
        var vec = p.vector();
        var score = switch (algo) {
          case "DOT" -> VectorMath.dot(vec, userVec);
          default -> VectorMath.cosine(vec, userVec);
        };
        var reason = explain(vec, recentVecs, recentIds, contentMap);
        return new Scored(new RecommendationItem(p.id(), score, cat, title, reason), vec);
      })).toList();

      var scored = new ArrayList<Scored>(computed.size());
      for (var f : computed) scored.add(f.get());
      scored.sort(java.util.Comparator.comparingDouble((Scored s) -> s.item.score()).reversed());

      var perCat = new HashMap<String, Integer>();
      var result = new ArrayList<RecommendationItem>();
      for (var s : scored) {
        if (result.size() >= topk) break;
        var n = perCat.getOrDefault(s.item.category(), 0);
        if (n >= maxPerCategory) continue;
        if (tooSimilar(result, s.vec)) continue;
        perCat.put(s.item.category(), n + 1);
        result.add(s.item);
      }
      return result;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      exec.close();
    }
  }

  private boolean tooSimilar(List<RecommendationItem> chosen, List<Double> candidateVector) {
    for (var it : chosen) {
      var v = contentVector(it.contentId());
      if (VectorMath.cosine(v, candidateVector) > 0.97) return true;
    }
    return false;
  }

  private List<Double> average(List<List<Double>> vectors) {
    if (vectors.isEmpty()) return List.of();
    var acc = VectorMath.zeros(vectors.getFirst().size());
    for (var v : vectors) VectorMath.addScaled(acc, v, 1.0);
    VectorMath.scale(acc, 1.0 / vectors.size());
    return acc;
  }

  private String explain(List<Double> candidate, List<List<Double>> recentVectors, List<Long> recentIds, Map<Long, Content> contentMap) {
    if (recentVectors.isEmpty()) return "Recomendado para você";
    int best = 0;
    double bestScore = -1.0;
    for (int i = 0; i < recentVectors.size(); i++) {
      var s = VectorMath.cosine(candidate, recentVectors.get(i));
      if (s > bestScore) { bestScore = s; best = i; }
    }
    var cid = recentIds.get(Math.min(best, recentIds.size() - 1));
    var c = contentMap.get(cid);
    var title = c == null ? ("conteúdo " + cid) : c.getTitle();
    return "Recomendado porque você interagiu com \"" + title + "\"";
  }

  private String quantizedKey(List<Double> v) {
    var sb = new StringBuilder();
    int step = Math.max(1, v.size() / 64);
    for (int i = 0; i < v.size(); i += step) {
      var q = Math.round(v.get(i) * 100.0) / 100.0;
      sb.append(q).append(',');
    }
    return sb.toString();
  }

  private record Pair(List<Double> v, double w) {}
}
