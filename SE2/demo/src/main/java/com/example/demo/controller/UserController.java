package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentResultRepository resultRepository;
    private final StudentErrorRepository errorRepository;

    public UserController(UserRepository userRepository, 
                          PasswordEncoder passwordEncoder,
                          EnrollmentRepository enrollmentRepository,
                          StudentResultRepository resultRepository,
                          StudentErrorRepository errorRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enrollmentRepository = enrollmentRepository;
        this.resultRepository = resultRepository;
        this.errorRepository = errorRepository;
    }

    @GetMapping
    public String listUsers(Model model,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "0") int page) {
        List<User> users = userRepository.findAll();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getUsername() != null && u.getUsername().toLowerCase().contains(q))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q)))
                    .toList();
        }

        if (role != null && !role.isBlank() && !role.equals("ALL")) {
            users = users.stream()
                    .filter(u -> u.getRole() != null && u.getRole().name().equals(role))
                    .toList();
        }

        if (status != null && !status.isBlank() && !status.equals("ALL")) {
            users = users.stream()
                    .filter(u -> status.equals(u.getStatus()))
                    .toList();
        }

        int pageSize = 10;
        int totalItems = users.size();
        int totalPages = (int) Math.ceil((double) totalItems / pageSize);
        int fromIdx = page * pageSize;
        int toIdx = Math.min(fromIdx + pageSize, totalItems);
        List<User> pagedUsers = (fromIdx < totalItems) ? users.subList(fromIdx, toIdx) : List.of();

        model.addAttribute("users", pagedUsers);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("fromIdx", fromIdx + 1);
        model.addAttribute("toIdx", toIdx);
        model.addAttribute("search", search);
        model.addAttribute("filterRole", role);
        model.addAttribute("filterStatus", status);
        model.addAttribute("roles", Arrays.asList(Role.values()));
        return "users/list";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "User không tồn tại."); return "redirect:/users"; }
        
        User user = opt.get();
        model.addAttribute("user", user);
        
        // If they are a student, fetch additional academic data
        if (user.getRole() == Role.STUDENT) {
            model.addAttribute("enrollments", enrollmentRepository.findByStudent(user));
            model.addAttribute("results", resultRepository.findByStudent(user));
            model.addAttribute("errors", errorRepository.findByStudent(user));
            return "students/detail"; // Reuse the student detail template
        }
        
        // Otherwise, show a basic profile view (we can create users/detail.html or just use the same)
        return "users/detail";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("roles", Arrays.asList(Role.ACADEMIC_STAFF, Role.STUDENT));
        return "users/create";
    }

    @PostMapping("/create")
    public String createUser(@RequestParam String fullName,
                             @RequestParam String username,
                             @RequestParam String email,
                             @RequestParam(required = false) String phone,
                             @RequestParam String rawPassword,
                             @RequestParam String role,
                             @RequestParam(defaultValue = "ACTIVE") String status,
                             RedirectAttributes ra) {
        if (userRepository.findByUsername(username).isPresent()) {
            ra.addFlashAttribute("error", "Username '" + username + "' đã tồn tại.");
            return "redirect:/users/create";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email '" + email + "' đã được sử dụng.");
            return "redirect:/users/create";
        }

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(Role.valueOf(role));
        user.setStatus(status);
        userRepository.save(user);

        ra.addFlashAttribute("success", "Tạo tài khoản " + username + " thành công!");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "User không tồn tại."); return "redirect:/users"; }
        model.addAttribute("user", opt.get());
        model.addAttribute("roles", Arrays.asList(Role.ACADEMIC_STAFF, Role.STUDENT));
        return "users/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id,
                             @RequestParam String fullName,
                             @RequestParam String email,
                             @RequestParam(required = false) String phone,
                             @RequestParam String role,
                             @RequestParam String status,
                             @RequestParam(required = false) String rawPassword,
                             RedirectAttributes ra) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "User không tồn tại."); return "redirect:/users"; }

        User user = opt.get();
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent() && !byEmail.get().getId().equals(id)) {
            ra.addFlashAttribute("error", "Email đã được dùng bởi tài khoản khác.");
            return "redirect:/users/" + id + "/edit";
        }

        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(Role.valueOf(role));
        user.setStatus(status);
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        userRepository.save(user);
        ra.addFlashAttribute("success", "Cập nhật tài khoản thành công!");
        return "redirect:/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            u.setStatus("INACTIVE");
            userRepository.save(u);
        });
        ra.addFlashAttribute("success", "Đã vô hiệu hoá tài khoản.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            u.setStatus("ACTIVE");
            userRepository.save(u);
        });
        ra.addFlashAttribute("success", "Đã kích hoạt tài khoản.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa tài khoản.");
        return "redirect:/users";
    }
}
