package com.escoladoestudante.reco.api;

import com.escoladoestudante.reco.api.dto.RecommendationResponse;
import com.escoladoestudante.reco.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
  private final RecommendationService service;
  public RecommendationController(RecommendationService service) { this.service = service; }

  @GetMapping("/{userId}")
  public RecommendationResponse recommend(@PathVariable long userId) {
    return service.recommendForUser(userId);
  }

  @GetMapping("/{userId}/more-like/{contentId}")
  public RecommendationResponse moreLikeThis(@PathVariable long userId, @PathVariable long contentId) {
    return service.moreLikeThis(contentId, userId);
  }
}
