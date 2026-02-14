package com.escoladoestudante.reco.api;

import com.escoladoestudante.reco.api.dto.InteractionRequest;
import com.escoladoestudante.reco.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {
  private final InteractionService service;
  public InteractionController(InteractionService service) { this.service = service; }

  @PostMapping
  public void record(@Valid @RequestBody InteractionRequest req) {
    service.record(req.userId(), req.contentId(), req.kind(), req.value());
  }
}
