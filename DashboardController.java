package com.escoladoestudante.reco.api;

import com.escoladoestudante.reco.api.dto.DashboardResponse;
import com.escoladoestudante.reco.service.MetricsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
  private final MetricsService metrics;
  public DashboardController(MetricsService metrics) { this.metrics = metrics; }

  @GetMapping
  public DashboardResponse dashboard() {
    double usdPer1M = 0.13;
    var s = metrics.snapshot24h(usdPer1M);
    return new DashboardResponse(s.requests24h(), s.cacheHitRate24h(), s.tokens24h(), s.costUsd24h());
  }
}
