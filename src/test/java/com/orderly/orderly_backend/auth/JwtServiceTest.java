package com.orderly.orderly_backend.auth;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.orderly.orderly_backend.exception.InvalidResetTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    // Minimum 32 bytes required by Nimbus MACSigner for HS256
    private static final String TEST_SECRET  = "test-secret-minimum-256-bits-for-hs256-compliance!!!!!";
    private static final String OTHER_SECRET = "other-secret-minimum-256-bits-for-hs256-compliance!!!!";

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("irrelevant")
                .build();
    }

    // -------------------------------------------------------------------------
    // issueAuthToken
    // -------------------------------------------------------------------------

    @Test
    void issueAuthToken_returnsNonNullTokenAndExpiry() {
        AuthTokenResult result = jwtService.issueAuthToken(testUser);

        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresAt()).isNotNull();
    }

    @Test
    void issueAuthToken_expiryIsApproximatelySevenDaysFromNow() {
        Instant lowerBound = Instant.now().plus(6, ChronoUnit.DAYS).plus(23, ChronoUnit.HOURS);
        AuthTokenResult result = jwtService.issueAuthToken(testUser);
        Instant upperBound = Instant.now().plus(7, ChronoUnit.DAYS).plus(1, ChronoUnit.MINUTES);

        assertThat(result.expiresAt()).isAfter(lowerBound).isBefore(upperBound);
    }

    @Test
    void issueAuthToken_tokenSubjectIsUserId() throws Exception {
        AuthTokenResult result = jwtService.issueAuthToken(testUser);

        JWTClaimsSet claims = SignedJWT.parse(result.token()).getJWTClaimsSet();
        assertThat(claims.getSubject()).isEqualTo(testUser.getId().toString());
    }

    @Test
    void issueAuthToken_tokenHasNoPurposeClaimAndNoJti() throws Exception {
        AuthTokenResult result = jwtService.issueAuthToken(testUser);

        JWTClaimsSet claims = SignedJWT.parse(result.token()).getJWTClaimsSet();
        assertThat(claims.getStringClaim("purpose")).isNull();
        assertThat(claims.getJWTID()).isNull();
    }

    // -------------------------------------------------------------------------
    // issueResetToken
    // -------------------------------------------------------------------------

    @Test
    void issueResetToken_returnsNonNullString() {
        assertThat(jwtService.issueResetToken(testUser)).isNotBlank();
    }

    @Test
    void issueResetToken_tokenSubjectIsUserId() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(jwtService.issueResetToken(testUser)).getJWTClaimsSet();

        assertThat(claims.getSubject()).isEqualTo(testUser.getId().toString());
    }

    @Test
    void issueResetToken_tokenHasNonEmptyJti() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(jwtService.issueResetToken(testUser)).getJWTClaimsSet();

        assertThat(claims.getJWTID()).isNotBlank();
    }

    @Test
    void issueResetToken_tokenHasPasswordResetPurposeClaim() throws Exception {
        JWTClaimsSet claims = SignedJWT.parse(jwtService.issueResetToken(testUser)).getJWTClaimsSet();

        assertThat(claims.getStringClaim("purpose")).isEqualTo("password-reset");
    }

    @Test
    void issueResetToken_expiresInApproximatelyOneHour() throws Exception {
        Instant lowerBound = Instant.now().plus(59, ChronoUnit.MINUTES);
        String token = jwtService.issueResetToken(testUser);
        Instant upperBound = Instant.now().plus(61, ChronoUnit.MINUTES);

        Instant expiry = SignedJWT.parse(token).getJWTClaimsSet().getExpirationTime().toInstant();
        assertThat(expiry).isAfter(lowerBound).isBefore(upperBound);
    }

    // -------------------------------------------------------------------------
    // parseResetToken
    // -------------------------------------------------------------------------

    @Test
    void parseResetToken_validToken_returnsClaimsWithCorrectSubject() {
        String token = jwtService.issueResetToken(testUser);

        JWTClaimsSet claims = jwtService.parseResetToken(token);

        assertThat(claims.getSubject()).isEqualTo(testUser.getId().toString());
    }

    @Test
    void parseResetToken_expiredToken_throwsInvalidResetTokenException() throws Exception {
        String expiredToken = signedToken(testUser.getId(), "password-reset",
                Instant.now().minus(2, ChronoUnit.HOURS), TEST_SECRET);

        assertThatThrownBy(() -> jwtService.parseResetToken(expiredToken))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void parseResetToken_authTokenPassedIn_throwsInvalidResetTokenException() {
        // Auth tokens have no purpose claim — must not be accepted by the reset endpoint
        String authToken = jwtService.issueAuthToken(testUser).token();

        assertThatThrownBy(() -> jwtService.parseResetToken(authToken))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void parseResetToken_missingPurposeClaim_throwsInvalidResetTokenException() throws Exception {
        String tokenNoPurpose = signedToken(testUser.getId(), null,
                Instant.now().plus(1, ChronoUnit.HOURS), TEST_SECRET);

        assertThatThrownBy(() -> jwtService.parseResetToken(tokenNoPurpose))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void parseResetToken_tamperedSignature_throwsInvalidResetTokenException() {
        String token = jwtService.issueResetToken(testUser);
        // Replace the last three signature characters with valid base64url chars to ensure
        // the token still parses structurally but fails MAC verification
        String tampered = token.substring(0, token.length() - 3) + "aaa";

        assertThatThrownBy(() -> jwtService.parseResetToken(tampered))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void parseResetToken_tokenSignedWithDifferentSecret_throwsInvalidResetTokenException() {
        JwtService otherService = new JwtService(OTHER_SECRET);
        String token = otherService.issueResetToken(testUser);

        assertThatThrownBy(() -> jwtService.parseResetToken(token))
                .isInstanceOf(InvalidResetTokenException.class);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    /**
     * Builds and signs a JWT manually so tests can craft tokens with specific
     * properties (e.g. past expiry, missing purpose) without going through JwtService.
     */
    private String signedToken(UUID userId, String purpose, Instant expiry, String secret) throws Exception {
        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .jwtID(UUID.randomUUID().toString())
                .issueTime(new Date())
                .expirationTime(Date.from(expiry));
        if (purpose != null) {
            builder.claim("purpose", purpose);
        }
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), builder.build());
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
