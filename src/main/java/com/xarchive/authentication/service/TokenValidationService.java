package com.xarchive.authentication.service;

import com.xarchive.invalidatedtokens.service.InvalidateTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenValidationService {
    @Autowired
    private InvalidateTokenService invalidateTokenService;

    public boolean isTokenBlacklisted(String token) {
        return invalidateTokenService.loadTokenInvalidatedByJWT(token) != null;
    }
}
