package org.woftnw.DreamvisitorHub.data.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.woftnw.DreamvisitorHub.data.type.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {}
