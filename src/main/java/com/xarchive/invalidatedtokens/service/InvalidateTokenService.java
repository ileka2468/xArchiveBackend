package com.xarchive.invalidatedtokens.service;

import com.xarchive.authentication.security.JwtTokenProvider;
import com.xarchive.invalidatedtokens.entity.Invalidatedtoken;
import com.xarchive.invalidatedtokens.repository.InvalidatedTokensRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

@Service
public class InvalidateTokenService {
    @Autowired
    @Lazy
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private InvalidatedTokensRepository invalidatedTokensRepository;

    public Invalidatedtoken loadTokenInvalidatedByJWT(String token) {
        if (token == null) {
            return null;
        }
        String tokenHash = DigestUtils.sha256Hex(token);
        return invalidatedTokensRepository.findByTokenHash(tokenHash);
    }

    public boolean invalidateTokens(String accessToken, String refreshToken) {

        if (loadTokenInvalidatedByJWT(accessToken) == null && loadTokenInvalidatedByJWT(refreshToken) == null) {
            String accessTokenHash = DigestUtils.sha256Hex(accessToken);
            Date accessTokenExpiration = jwtTokenProvider.getExpirationDateFromJWT(accessToken);
            OffsetDateTime accessTokenInvalidatedAt = OffsetDateTime.now(ZoneOffset.UTC);

            String refreshTokenHash = DigestUtils.sha256Hex(refreshToken);
            Date refreshTokenExpiration = jwtTokenProvider.getExpirationDateFromJWT(refreshToken);
            OffsetDateTime refreshTokenInvalidatedAt = OffsetDateTime.now(ZoneOffset.UTC);

            Invalidatedtoken invalidatedAccessToken = new Invalidatedtoken();
            Invalidatedtoken invalidatedRefreshToken = new Invalidatedtoken();

            // Set access token fields
            invalidatedAccessToken.setTokenHash(accessTokenHash);
            invalidatedAccessToken.setExpiryTime(accessTokenExpiration.toInstant().atOffset(ZoneOffset.UTC));
            invalidatedAccessToken.setInvalidatedAt(accessTokenInvalidatedAt);

            // Set refresh token fields
            invalidatedRefreshToken.setTokenHash(refreshTokenHash);
            invalidatedRefreshToken.setExpiryTime(refreshTokenExpiration.toInstant().atOffset(ZoneOffset.UTC));
            invalidatedRefreshToken.setInvalidatedAt(refreshTokenInvalidatedAt);

            // Save invalidated tokens
            invalidatedTokensRepository.save(invalidatedAccessToken);
            invalidatedTokensRepository.save(invalidatedRefreshToken);

            return true;
        }
        return false;
    }
}
