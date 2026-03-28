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

@Controller
@RequestMapping("/profile")
public class ProfileController {

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
        // Check email duplicate
        Optional<User> emailUser = userRepository.findByEmail(email);
        if (emailUser.isPresent() && !emailUser.get().getId().equals(user.getId())) {
            ra.addFlashAttribute("error", "Email đã được dùng bởi tài khoản khác.");
            return "redirect:/profile/edit";
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        userRepository.save(user);
        ra.addFlashAttribute("profileSuccess", "Cập nhật profile thành công!");
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

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "redirect:/profile/change-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu mới không khớp.");
            return "redirect:/profile/change-password";
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            ra.addFlashAttribute("error", "Mật khẩu mới không được trùng mật khẩu cũ.");
            return "redirect:/profile/change-password";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        ra.addFlashAttribute("passwordChanged", "true");
        return "redirect:/profile/change-password?changed=true";
    }
}
