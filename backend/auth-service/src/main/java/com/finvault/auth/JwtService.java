package com.finvault.auth;

import com.finvault.common.domain.Role;
import com.finvault.common.domain.UserAccount;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final KeyPair keyPair;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(KeyPair keyPair,
                      @Value("${app.jwt.issuer:finvault}") String issuer,
                      @Value("${app.jwt.access-token-minutes:15}") long accessTokenMinutes) {
        this.keyPair = keyPair;
        this.issuer = issuer;
        this.accessTtl = Duration.ofMinutes(accessTokenMinutes);
    }

    public String generateAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTtl);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(issuer)
            .subject(user.getEmail())
            .jwtID(UUID.randomUUID().toString())
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiresAt))
            .claim("uid", user.getId())
            .claim("role", user.getRole().name())
            .claim("email", user.getEmail())
            .build();
        SignedJWT jwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("finvault-runtime-rsa").build(),
            claims
        );
        try {
            jwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
            return jwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Unable to sign JWT", ex);
        }
    }

    public JwtClaims validate(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            boolean validSignature = jwt.verify(new RSASSAVerifier((RSAPublicKey) keyPair.getPublic()));
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (!validSignature || !issuer.equals(claims.getIssuer()) || claims.getExpirationTime().before(new Date())) {
                throw new BadCredentialsException("Invalid JWT");
            }
            return new JwtClaims(
                ((Number) claims.getClaim("uid")).longValue(),
                claims.getStringClaim("email"),
                Role.valueOf(claims.getStringClaim("role")),
                claims.getJWTID(),
                claims.getExpirationTime().toInstant()
            );
        } catch (ParseException | JOSEException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid JWT", ex);
        }
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    public record JwtClaims(Long userId, String email, Role role, String jwtId, Instant expiresAt) {
    }
}

