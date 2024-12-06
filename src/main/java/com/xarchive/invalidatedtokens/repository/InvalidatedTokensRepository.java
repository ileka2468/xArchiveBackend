package com.xarchive.invalidatedtokens.repository;

import com.xarchive.invalidatedtokens.entity.Invalidatedtoken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvalidatedTokensRepository extends JpaRepository<Invalidatedtoken, Integer> {
    public Invalidatedtoken findByTokenHash(String tokenHash);
}
