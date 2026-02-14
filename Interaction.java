package com.escoladoestudante.reco.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "interaction",
  indexes = {
    @Index(name = "ix_interaction_user_time", columnList = "userId,createdAt"),
    @Index(name = "ix_interaction_content_time", columnList = "contentId,createdAt")
  }
)
public class Interaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long contentId;

  @Column(nullable = false)
  private String kind;

  @Column(nullable = false)
  private double value;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private String algoBucket;

  public Long getId() { return id; }
  public Long getUserId() { return userId; }
  public Long getContentId() { return contentId; }
  public String getKind() { return kind; }
  public double getValue() { return value; }
  public Instant getCreatedAt() { return createdAt; }
  public String getAlgoBucket() { return algoBucket; }

  public void setUserId(Long userId) { this.userId = userId; }
  public void setContentId(Long contentId) { this.contentId = contentId; }
  public void setKind(String kind) { this.kind = kind; }
  public void setValue(double value) { this.value = value; }
  public void setAlgoBucket(String algoBucket) { this.algoBucket = algoBucket; }
}
