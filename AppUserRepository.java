package com.escoladoestudante.reco.repo;

import com.escoladoestudante.reco.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
  Optional<AppUser> findByExternalId(String externalId);
}
