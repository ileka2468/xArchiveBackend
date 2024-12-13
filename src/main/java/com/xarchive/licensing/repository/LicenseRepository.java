package com.xarchive.licensing.repository;

import com.xarchive.licensing.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Integer> {
    Optional<License> findByUserId(Integer userId);
    Optional<License> findByStripeSubscriptionId(String stripeSubscriptionId);
}