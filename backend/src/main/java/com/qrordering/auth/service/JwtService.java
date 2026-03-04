package com.qrordering.auth.service;

import com.qrordering.auth.config.JwtProperties;
import com.qrordering.auth.converter.JwtPrincipalConverter;
import com.qrordering.auth.converter.JwtPrincipalConverter.JwtClaimValues;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT generation and validation. Principal ↔ claims mapping is delegated to {@link JwtPrincipalConverter}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final JwtPrincipalConverter jwtPrincipalConverter;

    public String generateToken(UserDetails userDetails) {
        JwtClaimValues v = jwtPrincipalConverter.toClaimValues(userDetails);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration());

        var builder = Jwts.builder()
                .subject(userDetails.getUsername())
                .claim(JwtPrincipalConverter.CLAIM_USER_ID, v.userId())
                .claim(JwtPrincipalConverter.CLAIM_TENANT_ID, v.tenantId())
                .claim(JwtPrincipalConverter.CLAIM_USERNAME, v.username())
                .claim(JwtPrincipalConverter.CLAIM_USER_TYPE, v.userType())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey());
        if (v.role() != null) {
            builder.claim(JwtPrincipalConverter.CLAIM_ROLE, v.role());
        }
        return builder.compact();
    }

    /**
     * Validate token and return claims. Throws on invalid or expired token.
     */
    public Claims validateAndGetClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.debug("Invalid JWT: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Build UserDetails from JWT claims (after validation).
     */
    public UserDetails buildPrincipalFromClaims(Claims claims) {
        return jwtPrincipalConverter.toPrincipal(claims);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
