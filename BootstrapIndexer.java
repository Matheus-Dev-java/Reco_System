package com.escoladoestudante.reco.jobs;

import com.escoladoestudante.reco.repo.ContentRepository;
import com.escoladoestudante.reco.service.ContentService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Index Flyway-seeded contents into Qdrant on startup.
 *
 * Without this, the API works for "cold start" recommendations, but any personalized flow
 * (after interactions) will try to read vectors from Qdrant and can fail if the points do not exist.
 */
@Component
public class BootstrapIndexer {
  private static final Logger log = LoggerFactory.getLogger(BootstrapIndexer.class);

  private final ContentRepository contents;
  private final ContentService contentService;
  private final int maxToIndex;

  public BootstrapIndexer(
      ContentRepository contents,
      ContentService contentService,
      @Value("${reco.bootstrap.index-max:200}") int maxToIndex
  ) {
    this.contents = contents;
    this.contentService = contentService;
    this.maxToIndex = maxToIndex;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    // Run async so app starts fast.
    Executors.newSingleThreadExecutor().submit(() -> {
      try {
        var list = contents.findAll();
        int n = 0;
        for (var c : list) {
          if (n >= maxToIndex) break;
          try {
            contentService.indexExisting(c.getId());
            n++;
          } catch (Exception e) {
            log.warn("Failed to index content {} into Qdrant: {}", c.getId(), e.getMessage());
          }
        }
        log.info("Bootstrap indexing completed: {} contents indexed", n);
      } catch (Exception e) {
        log.warn("Bootstrap indexing failed: {}", e.getMessage());
      }
    });
  }
}
