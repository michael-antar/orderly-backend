package com.orderly.orderly_backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsedResetTokenRepository extends JpaRepository<UsedResetToken, Long> {

    boolean existsByJti(String jti);
}
