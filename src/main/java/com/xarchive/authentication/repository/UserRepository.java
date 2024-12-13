package com.xarchive.authentication.repository;

import com.xarchive.authentication.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);
    Optional<User> findByStripeCustomerId(String username);
}
