package com.escoladoestudante.reco.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String externalId;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public String getExternalId() { return externalId; }
  public Instant getCreatedAt() { return createdAt; }
  public void setExternalId(String externalId) { this.externalId = externalId; }
}
