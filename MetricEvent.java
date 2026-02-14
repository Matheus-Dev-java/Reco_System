package com.escoladoestudante.reco.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "metric_event",
  indexes = {
    @Index(name = "ix_metric_time", columnList = "createdAt"),
    @Index(name = "ix_metric_type_time", columnList = "type,createdAt")
  }
)
public class MetricEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String type;

  @Column
  private Long userId;

  @Column
  private Long contentId;

  @Column
  private String algo;

  @Column(nullable = false)
  private boolean cacheHit;

  @Column(nullable = false)
  private long openAiTokens;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getType() { return type; }
  public Long getUserId() { return userId; }
  public Long getContentId() { return contentId; }
  public String getAlgo() { return algo; }
  public boolean isCacheHit() { return cacheHit; }
  public long getOpenAiTokens() { return openAiTokens; }
  public Instant getCreatedAt() { return createdAt; }

  public void setType(String type) { this.type = type; }
  public void setUserId(Long userId) { this.userId = userId; }
  public void setContentId(Long contentId) { this.contentId = contentId; }
  public void setAlgo(String algo) { this.algo = algo; }
  public void setCacheHit(boolean cacheHit) { this.cacheHit = cacheHit; }
  public void setOpenAiTokens(long openAiTokens) { this.openAiTokens = openAiTokens; }
}
