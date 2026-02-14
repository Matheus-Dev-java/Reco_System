package com.escoladoestudante.reco.api;

import com.escoladoestudante.reco.api.dto.ContentResponse;
import com.escoladoestudante.reco.api.dto.CreateContentRequest;
import com.escoladoestudante.reco.entity.Content;
import com.escoladoestudante.reco.repo.ContentRepository;
import com.escoladoestudante.reco.service.ContentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
public class ContentController {
  private final ContentRepository repo;
  private final ContentService contentService;

  public ContentController(ContentRepository repo, ContentService contentService) {
    this.repo = repo;
    this.contentService = contentService;
  }

  @PostMapping
  public ContentResponse create(@Valid @RequestBody CreateContentRequest req) {
    var c = new Content();
    c.setType(req.type());
    c.setTitle(req.title());
    c.setDescription(req.description());
    c.setCategory(req.category());
    c.setTags(req.tags());
    c.setUrl(req.url());
    var saved = contentService.create(c);
    return map(saved);
  }

  @GetMapping("/{id}")
  public ContentResponse get(@PathVariable long id) { return map(repo.findById(id).orElseThrow()); }

  @GetMapping
  public List<ContentResponse> list() { return repo.findAll().stream().map(this::map).toList(); }

  private ContentResponse map(Content c) {
    return new ContentResponse(c.getId(), c.getType(), c.getTitle(), c.getDescription(), c.getCategory(), c.getTags(), c.getUrl(), c.getCreatedAt(), c.getUpdatedAt());
  }
}
