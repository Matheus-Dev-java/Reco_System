package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.entity.Content;
import com.escoladoestudante.reco.repo.ContentRepository;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {
  private final ContentRepository repo;
  private final EmbeddingService embeddings;
  private final QdrantService qdrant;

  public ContentService(ContentRepository repo, EmbeddingService embeddings, QdrantService qdrant) {
    this.repo = repo;
    this.embeddings = embeddings;
    this.qdrant = qdrant;
  }

  @Transactional
  public Content create(Content c) {
    c.touch();
    var saved = repo.save(c);
    var text = buildEmbeddingText(saved);
    var emb = embeddings.embeddingForText(text, null);
    qdrant.upsertPoint(saved.getId(), emb.vector(), Map.of(
        "category", saved.getCategory(),
        "tags", saved.getTags(),
        "type", saved.getType().name()
    ));
    return saved;
  }

  public String buildEmbeddingText(Content c) {
    var prompt = """
        Título: %s
        Descrição: %s
        Categoria: %s
        Tags: %s
        """;
    return String.format(prompt, c.getTitle(), c.getDescription(), c.getCategory(), c.getTags());
  }

  /**
   * Index existing content (already in Postgres) into Qdrant.
   *
   * Useful because Flyway seeds insert directly into Postgres, so Qdrant may not have those points yet.
   */
  @Transactional
  public void indexExisting(long contentId) {
    var c = repo.findById(contentId).orElseThrow();
    var text = buildEmbeddingText(c);
    var emb = embeddings.embeddingForText(text, null);
    qdrant.upsertPoint(c.getId(), emb.vector(), Map.of(
        "category", c.getCategory(),
        "tags", c.getTags(),
        "type", c.getType().name()
    ));
  }
}
