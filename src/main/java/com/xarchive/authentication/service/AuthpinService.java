package com.xarchive.authentication.service;

import com.xarchive.authentication.entity.Authpin;
import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.repository.AuthpinRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthpinService {

    private final AuthpinRepository authpinRepository;

    public AuthpinService(AuthpinRepository authpinRepository) {
        this.authpinRepository = authpinRepository;
    }

    // Generate a new PIN for a user
    @Transactional
    public Authpin createPin(User user, String pinCode, Instant expiryTs) {
        Authpin authpin = new Authpin();
        authpin.setUser(user);
        authpin.setPinCode(pinCode);
        authpin.setExpiryTs(expiryTs);
        authpin.setCreatedAt(Instant.now());
        authpin.setVerified(false);
        return authpinRepository.save(authpin);
    }

    @Transactional
    public Authpin createPin(User user, String pinCode, Instant expiryTs, String intent, String data) {
        Authpin authpin = new Authpin();
        authpin.setUser(user);
        authpin.setPinCode(pinCode);
        authpin.setExpiryTs(expiryTs);
        authpin.setCreatedAt(Instant.now());
        authpin.setVerified(false);
        authpin.setIntent(intent);
        authpin.setData(data);
        return authpinRepository.save(authpin);
    }


    // Validate a PIN for a user
    @Transactional
    public boolean validatePin(User user, String pinCode) {
        Optional<Authpin> optionalAuthpin = authpinRepository.findByUserAndPinCode(user, pinCode);

        if (optionalAuthpin.isPresent()) {
            Authpin authpin = optionalAuthpin.get();
            if (!authpin.getVerified() && authpin.getExpiryTs().isAfter(Instant.now())) {
                authpin.setVerified(true); // Mark as verified
                authpinRepository.save(authpin);
                return true;
            }
        }
        return false;
    }

    // Clean up expired PINs
    @Transactional
    public void cleanupExpiredPins() {
        authpinRepository.deleteByExpiryTsBefore(Instant.now());
    }

    // Check if a PIN is valid
    public boolean isValidPin(String pinCode) {
        return authpinRepository.existsValidPin(pinCode, Instant.now());
    }
}
