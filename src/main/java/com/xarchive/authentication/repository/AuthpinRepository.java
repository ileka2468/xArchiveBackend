package com.xarchive.authentication.repository;

import com.xarchive.authentication.entity.Authpin;
import com.xarchive.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AuthpinRepository extends JpaRepository<Authpin, Integer> {

    // Find the latest unverified PIN for a specific user
    Optional<Authpin> findFirstByUserAndVerifiedFalseOrderByCreatedAtDesc(User user);

    Optional<Authpin> findTopByUserAndPinCodeAndVerifiedTrueOrderByCreatedAtDesc(User user, String pinCode);


    // Find a PIN by user and PIN code
    Optional<Authpin> findByUserAndPinCode(User user, String pinCode);

    // Delete expired PINs
    void deleteByExpiryTsBefore(Instant now);

    // Check if a specific PIN exists and is unexpired
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
            "FROM Authpin a WHERE a.pinCode = :pinCode AND a.expiryTs > :now AND a.verified = false")
    boolean existsValidPin(String pinCode, Instant now);
}
