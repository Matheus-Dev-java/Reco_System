package com.escoladoestudante.reco.repo;

import com.escoladoestudante.reco.entity.Content;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentRepository extends JpaRepository<Content, Long> {
  @Query("select c from Content c where c.updatedAt >= :since order by c.updatedAt desc")
  List<Content> findUpdatedSince(Instant since);
}
