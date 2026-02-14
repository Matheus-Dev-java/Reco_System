package com.escoladoestudante.reco.service;

import com.escoladoestudante.reco.entity.AppUser;
import com.escoladoestudante.reco.repo.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
  private final AppUserRepository repo;

  public UserService(AppUserRepository repo) { this.repo = repo; }

  @Transactional
  public AppUser create(String externalId) {
    return repo.findByExternalId(externalId).orElseGet(() -> {
      var u = new AppUser();
      u.setExternalId(externalId);
      return repo.save(u);
    });
  }

  public AppUser get(long id) { return repo.findById(id).orElseThrow(); }
}
