package com.sports.management.result.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider implements InitializingBean {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void afterPropertiesSet() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET / jwt.secret must be configured with at least 32 bytes");
        }
    }

    public String generateInternalSyncToken() {
        try {
            Date now = new Date();
            Date expiry = new Date(now.getTime() + 60_000L);
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject("result-sync-worker")
                    .claim("role", "ROLE_INTERNAL_RESULT_SYNC")
                    .claim("scope", "internal:result-sync")
                    .issueTime(now)
                    .expirationTime(expiry)
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(jwtSecret.getBytes(StandardCharsets.UTF_8)));
            return "Bearer " + signedJWT.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign internal sync token", ex);
        }
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtSecret.getBytes(StandardCharsets.UTF_8));
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            Object role = signedJWT.getJWTClaimsSet().getClaim("role");
            return role != null ? role.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
