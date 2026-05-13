package com.orderly.orderly_backend.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.orderly.orderly_backend.exception.InvalidResetTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final long AUTH_TOKEN_DAYS = 7;
    private static final long RESET_TOKEN_HOURS = 1;
    private static final String PURPOSE_CLAIM = "purpose";
    private static final String RESET_PURPOSE = "password-reset";

    private final byte[] secret;

    public JwtService(@Value("${app.jwt.secret}") String jwtSecret) {
        this.secret = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Issues a signed auth JWT for the given user.
     * Returns both the compact token string and the expiry instant so the
     * caller can populate AuthResponse.expiresAt without re-deriving the constant.
     */
    public AuthTokenResult issueAuthToken(User user) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plus(AUTH_TOKEN_DAYS, ChronoUnit.DAYS);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer());

            return new AuthTokenResult(signedJWT.serialize(), expiresAt);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue auth token", e);
        }
    }

    /**
     * Issues a signed password-reset JWT for the given user.
     * Includes a jti for replay prevention and a purpose claim so a normal
     * auth token cannot be submitted to the reset endpoint.
     */
    public String issueResetToken(User user) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plus(RESET_TOKEN_HOURS, ChronoUnit.HOURS);

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(expiresAt))
                    .claim(PURPOSE_CLAIM, RESET_PURPOSE)
                    .build();

            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(signer());

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to issue reset token", e);
        }
    }

    /**
     * Parses and validates a password-reset JWT.
     * Verifies the signature, expiry, and presence of the purpose claim.
     * Throws InvalidResetTokenException on any failure so the caller never
     * has to deal with raw JWT exceptions.
     */
    public JWTClaimsSet parseResetToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(secret);
            if (!signedJWT.verify(verifier)) {
                throw new InvalidResetTokenException();
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime() == null
                    || claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                throw new InvalidResetTokenException();
            }

            if (!RESET_PURPOSE.equals(claims.getStringClaim(PURPOSE_CLAIM))) {
                throw new InvalidResetTokenException();
            }

            return claims;
        } catch (InvalidResetTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidResetTokenException();
        }
    }

    private JWSSigner signer() throws Exception {
        return new MACSigner(secret);
    }
}
