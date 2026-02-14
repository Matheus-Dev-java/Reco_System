package com.escoladoestudante.reco.repo;

import com.escoladoestudante.reco.entity.Interaction;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
  @Query("select i from Interaction i where i.userId = :userId order by i.createdAt desc")
  List<Interaction> findRecentByUser(Long userId);

  @Query("select i.contentId, count(i.id) from Interaction i where i.createdAt >= :since group by i.contentId order by count(i.id) desc")
  List<Object[]> topContentSince(Instant since);

  @Query("select i.userId, count(i.id) from Interaction i where i.createdAt >= :since group by i.userId order by count(i.id) desc")
  List<Object[]> topUsersSince(Instant since);
}
