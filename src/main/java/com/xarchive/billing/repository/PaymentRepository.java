package com.xarchive.billing.repository;

import com.xarchive.authentication.entity.User;
import com.xarchive.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    public Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    public List<Payment> findAllByUserId(Integer userId);
}