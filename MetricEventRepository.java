package com.escoladoestudante.reco.repo;

import com.escoladoestudante.reco.entity.MetricEvent;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MetricEventRepository extends JpaRepository<MetricEvent, Long> {
  @Query("select count(m.id) from MetricEvent m where m.type = :type and m.createdAt >= :since")
  long countTypeSince(String type, Instant since);

  @Query("select avg(case when m.cacheHit = true then 1.0 else 0.0 end) from MetricEvent m where m.type = :type and m.createdAt >= :since")
  Double cacheHitRate(String type, Instant since);

  @Query("select sum(m.openAiTokens) from MetricEvent m where m.createdAt >= :since")
  Long tokensSince(Instant since);
}
