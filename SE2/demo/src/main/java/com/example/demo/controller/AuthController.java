package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
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

        return "redirect:/login?registered=true";
    }
}
