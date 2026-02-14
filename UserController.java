package com.escoladoestudante.reco.api;

import com.escoladoestudante.reco.api.dto.CreateUserRequest;
import com.escoladoestudante.reco.api.dto.UserResponse;
import com.escoladoestudante.reco.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService users;

  public UserController(UserService users) { this.users = users; }

  @PostMapping
  public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
    var u = users.create(req.externalId());
    return new UserResponse(u.getId(), u.getExternalId(), u.getCreatedAt());
  }

  @GetMapping("/{id}")
  public UserResponse get(@PathVariable long id) {
    var u = users.get(id);
    return new UserResponse(u.getId(), u.getExternalId(), u.getCreatedAt());
  }
}
