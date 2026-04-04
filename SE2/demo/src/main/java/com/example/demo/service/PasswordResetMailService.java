package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetMailService {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailService.class);

    public void sendVerificationCode(String email, String verificationCode) {
        log.info("Password reset verification code for {} is {}", email, verificationCode);
    }
}
