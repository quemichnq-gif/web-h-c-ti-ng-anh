package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.PasswordResetMailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailService passwordResetMailService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          PasswordResetMailService passwordResetMailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetMailService = passwordResetMailService;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String startPasswordReset(@RequestParam String email, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());

        if (userOpt.isEmpty()) {
            return "redirect:/forgot-password?error=not_found";
        }

        User user = userOpt.get();
        String verificationCode = generateVerificationCode();
        user.setResetVerificationCode(verificationCode);
        user.setResetCodeExpiresAt(LocalDateTime.now().plusMinutes(10));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);

        userRepository.save(user);

        boolean delivered = passwordResetMailService.sendVerificationCode(user.getEmail(), verificationCode);
        if (!delivered) {
            ra.addFlashAttribute("verificationCode", verificationCode);
            ra.addFlashAttribute("mailFallback", true);
        }

        return "redirect:/verify-reset-code?email=" + user.getEmail();
    }

    @GetMapping("/verify-reset-code")
    public String verifyResetCodePage(@RequestParam(required = false) String email, Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/forgot-password?error=missing_email";
        }
        model.addAttribute("email", email);
        return "verify-reset-code";
    }

    @PostMapping("/verify-reset-code")
    public String verifyResetCode(@RequestParam String email,
                                  @RequestParam String code) {
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());
        if (userOpt.isEmpty()) {
            return "redirect:/forgot-password?error=not_found";
        }

        User user = userOpt.get();
        if (user.getResetCodeExpiresAt() == null || user.getResetCodeExpiresAt().isBefore(LocalDateTime.now())) {
            return "redirect:/forgot-password?error=expired";
        }
        if (user.getResetVerificationCode() == null || !user.getResetVerificationCode().equals(code.trim())) {
            return "redirect:/verify-reset-code?email=" + user.getEmail() + "&error=invalid_code";
        }

        user.setResetToken(UUID.randomUUID().toString());
        user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setResetVerificationCode(null);
        user.setResetCodeExpiresAt(null);
        userRepository.save(user);

        return "redirect:/reset-password?token=" + user.getResetToken();
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            return "redirect:/forgot-password?error=missing_token";
        }

        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty() || userOpt.get().getResetTokenExpiresAt() == null
                || userOpt.get().getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return "redirect:/forgot-password?error=expired";
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String completePasswordReset(@RequestParam String token,
                                        @RequestParam String password,
                                        @RequestParam String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            return "redirect:/reset-password?token=" + token + "&error=password_mismatch";
        }

        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isEmpty() || userOpt.get().getResetTokenExpiresAt() == null
                || userOpt.get().getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return "redirect:/forgot-password?error=expired";
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(password));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        user.setResetVerificationCode(null);
        user.setResetCodeExpiresAt(null);
        userRepository.save(user);

        return "redirect:/login?reset=success";
    }

    @PostMapping("/register")
    public String registerAccount(@RequestParam String fullName,
                                  @RequestParam String identifier,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword) {
        
        if (!password.equals(confirmPassword)) {
            return "redirect:/register?error=password_mismatch";
        }

        if (userRepository.findByEmail(email).isPresent() || userRepository.findByUsername(identifier).isPresent()) {
            return "redirect:/register?error=exists";
        }

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(identifier);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(Role.STUDENT);
        user.setStatus("ACTIVE");

        userRepository.save(user);

        return "redirect:/login?registered=true#signin";
    }

    private String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
