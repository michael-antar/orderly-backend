package com.orderly.orderly_backend.auth;

public interface EmailService {

    void sendPasswordReset(String toEmail, String resetToken);
}
