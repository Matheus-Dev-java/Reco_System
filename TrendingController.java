package com.escoladoestudante.reco.api;

import com.escoladoestudante.reco.api.dto.TrendingResponse;
import com.escoladoestudante.reco.repo.InteractionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trending")
public class TrendingController {
  private final InteractionRepository interactions;
  public TrendingController(InteractionRepository interactions) { this.interactions = interactions; }

  @GetMapping
  public TrendingResponse trending24h() {
    var since = Instant.now().minusSeconds(24 * 3600);
    List<Long> ids = interactions.topContentSince(since).stream().limit(20).map(r -> ((Number) r[0]).longValue()).toList();
    return new TrendingResponse(ids);
  }
}
