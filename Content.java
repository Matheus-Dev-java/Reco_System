package com.escoladoestudante.reco.entity;

import com.escoladoestudante.reco.domain.ContentType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "content")
public class Content {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContentType type;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 2000)
  private String description;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false)
  private String tags;

  @Column(nullable = false)
  private String url;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  public Long getId() { return id; }
  public ContentType getType() { return type; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public String getCategory() { return category; }
  public String getTags() { return tags; }
  public String getUrl() { return url; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void setType(ContentType type) { this.type = type; }
  public void setTitle(String title) { this.title = title; }
  public void setDescription(String description) { this.description = description; }
  public void setCategory(String category) { this.category = category; }
  public void setTags(String tags) { this.tags = tags; }
  public void setUrl(String url) { this.url = url; }
  public void touch() { this.updatedAt = Instant.now(); }
}
