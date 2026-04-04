package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern PASSWORD_LENGTH_PATTERN = Pattern.compile("^.{8,}$");
    private static final Pattern PASSWORD_UPPER_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern PASSWORD_LOWER_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern PASSWORD_DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern PASSWORD_SYMBOL_PATTERN = Pattern.compile(".*[^a-zA-Z0-9].*");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String view(Model model, Authentication auth) {
        userRepository.findByUsername(auth.getName())
                .ifPresent(u -> model.addAttribute("user", u));
        return "profile/view";
    }

    @GetMapping("/edit")
    public String editForm(Model model, Authentication auth) {
        userRepository.findByUsername(auth.getName())
                .ifPresent(u -> model.addAttribute("user", u));
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                Authentication auth,
                                RedirectAttributes ra) {
        Optional<User> opt = userRepository.findByUsername(auth.getName());
        if (opt.isEmpty()) return "redirect:/profile";

        User user = opt.get();
        String normalizedFullName = fullName == null ? "" : fullName.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPhone = phone == null ? null : phone.trim();

        if (normalizedFullName.isBlank()) {
            ra.addFlashAttribute("error", "Full name is required.");
            return "redirect:/profile/edit";
        }
        if (normalizedEmail.isBlank() || !isValidEmail(normalizedEmail)) {
            ra.addFlashAttribute("error", "Please enter a valid email address.");
            return "redirect:/profile/edit";
        }
        if (normalizedPhone != null && !normalizedPhone.isBlank() && !PHONE_PATTERN.matcher(normalizedPhone).matches()) {
            ra.addFlashAttribute("error", "Phone number must contain exactly 10 digits.");
            return "redirect:/profile/edit";
        }

        Optional<User> emailUser = userRepository.findByEmail(normalizedEmail);
        if (emailUser.isPresent() && !emailUser.get().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "This email is already used by another account.");
            return "redirect:/profile/edit";
        }

        user.setFullName(normalizedFullName);
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone == null || normalizedPhone.isBlank() ? null : normalizedPhone);
        userRepository.save(user);
        ra.addFlashAttribute("profileSuccess", "Profile updated successfully!");
        return "redirect:/profile/edit";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "profile/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication auth,
                                 RedirectAttributes ra) {
        Optional<User> opt = userRepository.findByUsername(auth.getName());
        if (opt.isEmpty()) return "redirect:/login";

        User user = opt.get();

        String current = currentPassword == null ? "" : currentPassword;
        String next = newPassword == null ? "" : newPassword;
        String confirm = confirmPassword == null ? "" : confirmPassword;

        if (!passwordEncoder.matches(current, user.getPassword())) {
            ra.addFlashAttribute("error", "Current password is incorrect.");
            return "redirect:/profile/change-password";
        }
        if (!next.equals(confirm)) {
            ra.addFlashAttribute("error", "New password and confirmation do not match.");
            return "redirect:/profile/change-password";
        }
        if (!isStrongPassword(next)) {
            ra.addFlashAttribute("error", "Password must be at least 8 characters and include uppercase, lowercase, number, and symbol.");
            return "redirect:/profile/change-password";
        }
        if (passwordEncoder.matches(next, user.getPassword())) {
            ra.addFlashAttribute("error", "New password must be different from the current password.");
            return "redirect:/profile/change-password";
        }

        user.setPassword(passwordEncoder.encode(next));
        userRepository.save(user);
        ra.addFlashAttribute("passwordChanged", "true");
        return "redirect:/profile/change-password?changed=true";
    }

    private boolean isValidEmail(String email) {
        return email.contains("@") && email.indexOf('@') > 0 && email.indexOf('@') < email.length() - 1 && email.contains(".");
    }

    private boolean isStrongPassword(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_LENGTH_PATTERN.matcher(password).matches()
                && PASSWORD_UPPER_PATTERN.matcher(password).matches()
                && PASSWORD_LOWER_PATTERN.matcher(password).matches()
                && PASSWORD_DIGIT_PATTERN.matcher(password).matches()
                && PASSWORD_SYMBOL_PATTERN.matcher(password).matches();
    }
}
