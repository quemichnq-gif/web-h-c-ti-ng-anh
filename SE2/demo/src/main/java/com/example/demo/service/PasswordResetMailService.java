package com.example.demo.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PasswordResetMailService(JavaMailSender mailSender,
                                    @Value("${app.mail.from:no-reply@academic-portal.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public boolean sendVerificationCode(String email, String verificationCode) {
        String subject = "Your password reset verification code";
        String text = """
                Use the verification code below to continue resetting your password:

                %s

                This code expires in 10 minutes.
                If you did not request a password reset, you can ignore this email.
                """.formatted(verificationCode);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
            log.info("Password reset verification code sent to {}", email);
            return true;
        } catch (MessagingException | RuntimeException ex) {
            log.warn("Failed to send password reset email to {}. Falling back to log-only delivery.", email, ex);
            log.info("Password reset verification code for {} is {}", email, verificationCode);
            return false;
        }
    }
}
