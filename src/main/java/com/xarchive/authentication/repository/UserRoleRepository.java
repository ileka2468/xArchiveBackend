package com.xarchive.authentication.repository;

import com.xarchive.authentication.entity.UserRole;
import com.xarchive.authentication.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}
