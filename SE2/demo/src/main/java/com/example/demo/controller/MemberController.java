package com.example.demo.controller;

import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.Role;
import com.example.demo.model.StudentResult;
import com.example.demo.model.User;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.StudentResultRepository;
import com.example.demo.repository.TestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class MemberController {
    private static final String PASSWORD_RULE = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TestRepository testRepository;
    private final StudentResultRepository studentResultRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    public MemberController(UserRepository userRepository,
                            EnrollmentRepository enrollmentRepository,
                            TestRepository testRepository,
                            StudentResultRepository studentResultRepository,
                            PasswordEncoder passwordEncoder,
                            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.testRepository = testRepository;
        this.studentResultRepository = studentResultRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
    }

    @GetMapping("/member/home")
    public String home() {
        return "redirect:/";
    }

    @GetMapping("/member/notifications")
    public String notifications(Authentication authentication, Model model) {
        User current = resolveCurrentUser(authentication).orElse(null);
        model.addAttribute("notifications", notificationService.buildNotifications(current));
        return "member/notifications";
    }

    @GetMapping("/member/settings")
    public String settings(Authentication authentication, Model model) {
        User current = resolveCurrentUser(authentication).orElse(null);
        model.addAttribute("displayName", current != null ? current.getFullName() : "");
        model.addAttribute("email", current != null ? current.getEmail() : "");
        model.addAttribute("phone", current != null ? current.getPhone() : "");
        model.addAttribute("roleLabel", current != null && current.getRole() != null ? current.getRole().name() : "USER");
        return "member/settings";
    }

    @PostMapping("/member/settings/profile")
    public String updateProfile(Authentication authentication,
                                @RequestParam String displayName,
                                @RequestParam String email,
                                @RequestParam(required = false) String phone,
                                RedirectAttributes ra) {
        Optional<User> currentOpt = resolveCurrentUser(authentication);
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }

        User current = currentOpt.get();
        Optional<User> emailOwner = userRepository.findByEmail(email);
        if (emailOwner.isPresent() && !emailOwner.get().getId().equals(current.getId())) {
            return "redirect:/member/settings?message=profile_error";
        }

        current.setFullName(displayName);
        current.setEmail(email);
        current.setPhone(phone);
        userRepository.save(current);
        return "redirect:/member/settings?message=profile_saved";
    }

    @PostMapping("/member/settings/password")
    public String updatePassword(Authentication authentication,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword) {
        Optional<User> currentOpt = resolveCurrentUser(authentication);
        if (currentOpt.isEmpty()) {
            return "redirect:/login";
        }

        User current = currentOpt.get();
        if (!passwordEncoder.matches(currentPassword, current.getPassword())) {
            return "redirect:/member/settings?message=password_error";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/member/settings?message=password_error";
        }
        if (!newPassword.matches(PASSWORD_RULE)) {
            return "redirect:/member/settings?message=password_error";
        }

        current.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(current);
        return "redirect:/member/settings?message=password_saved";
    }

    private Optional<User> resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()));
    }

}
