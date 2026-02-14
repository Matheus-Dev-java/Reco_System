package com.escoladoestudante.reco.jobs;

import com.escoladoestudante.reco.repo.ContentRepository;
import com.escoladoestudante.reco.repo.InteractionRepository;
import com.escoladoestudante.reco.service.ContentService;
import com.escoladoestudante.reco.service.QdrantService;
import com.escoladoestudante.reco.service.RecommendationService;
import java.time.Instant;
import java.util.concurrent.Executors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchJobs {
  private final QdrantService qdrant;
  private final ContentRepository contents;
  private final InteractionRepository interactions;
  private final ContentService contentService;
  private final RecommendationService recommender;

  public BatchJobs(QdrantService qdrant,
                   ContentRepository contents,
                   InteractionRepository interactions,
                   ContentService contentService,
                   RecommendationService recommender) {
    this.qdrant = qdrant;
    this.contents = contents;
    this.interactions = interactions;
    this.contentService = contentService;
    this.recommender = recommender;
  }

  @Scheduled(initialDelay = 5000, fixedDelay = 60000)
  public void ensureQdrantCollection() { qdrant.ensureCollection(); }

  @Scheduled(cron = "0 0 3 * * *", zone = "America/Bahia")
  public void nightlyReindexAndWarmup() {
    var since = Instant.now().minusSeconds(7 * 86400);
    var updated = contents.findUpdatedSince(since);

    var exec = Executors.newVirtualThreadPerTaskExecutor();
    try {
      updated.forEach(c -> exec.submit(() -> {
        contentService.create(c);
        return null;
      }));
    } finally {
      exec.close();
    }

    var topUsers = interactions.topUsersSince(Instant.now().minusSeconds(86400)).stream().limit(1000).toList();
    topUsers.forEach(o -> {
      try {
        var userId = ((Number) o[0]).longValue();
        recommender.recommendForUser(userId);
      } catch (Exception ignored) {}
    });
  }
}
