package com.xarchive.billing.repository;

import com.xarchive.billing.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Integer> {

    Optional<Plan> findByStripePriceId(String stripePriceId);
}
