package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.domain.InteractionKind;
import com.escoladoestudante.reco.entity.Interaction;
import com.escoladoestudante.reco.repo.InteractionRepository;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionService {
  private final InteractionRepository repo;
  private final RedisCacheService redis;
  private final AbTestingService ab;
  private static final Set<String> ALLOWED = Set.of("VIEW", "LIKE", "SHARE", "BOOKMARK");

  public InteractionService(InteractionRepository repo, RedisCacheService redis, AbTestingService ab) {
    this.repo = repo;
    this.redis = redis;
    this.ab = ab;
  }

  @Transactional
  public Interaction record(long userId, long contentId, String kind, double value) {
    if (!ALLOWED.contains(kind)) throw new IllegalArgumentException("Invalid kind");
    InteractionKind.fromCode(kind);

    var i = new Interaction();
    i.setUserId(userId);
    i.setContentId(contentId);
    i.setKind(kind);
    i.setValue(value);
    i.setAlgoBucket(ab.bucketForUser(userId));
    var saved = repo.save(i);

    redis.del("l1:reco:u:" + userId + ":a:COSINE");
    redis.del("l1:reco:u:" + userId + ":a:DOT");
    return saved;
  }
}
