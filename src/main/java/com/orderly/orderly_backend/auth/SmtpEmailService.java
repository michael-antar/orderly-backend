package com.orderly.orderly_backend.auth;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void sendPasswordReset(String toEmail, String resetToken) {
        try {
            String resetLink = frontendBaseUrl + "/reset-password?token=" + resetToken;

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Reset your Orderly password");
            helper.setText(
                    "<p>Click the link below to reset your password. This link expires in 1 hour.</p>"
                    + "<p><a href=\"" + resetLink + "\">Reset password</a></p>"
                    + "<p>If you did not request a password reset, you can safely ignore this email.</p>",
                    true
            );

            mailSender.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send password reset email", e);
        }
    }
}
