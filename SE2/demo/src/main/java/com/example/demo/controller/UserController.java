package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.Enrollment;
import com.example.demo.model.StudentError;
import com.example.demo.model.StudentResult;
import com.example.demo.repository.*;
import com.example.demo.service.AuditLogService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
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
    private final AuditLogService auditLogService;

    public UserController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          EnrollmentRepository enrollmentRepository,
                          StudentResultRepository resultRepository,
                          StudentErrorRepository errorRepository,
                          AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enrollmentRepository = enrollmentRepository;
        this.resultRepository = resultRepository;
        this.errorRepository = errorRepository;
        this.auditLogService = auditLogService;
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
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "User not found."); return "redirect:/users"; }

        User user = opt.get();
        model.addAttribute("user", user);

        if (user.getRole() == Role.STUDENT) {
            model.addAttribute("enrollments", enrollmentRepository.findByStudent(user));
            model.addAttribute("results", resultRepository.findByStudent(user));
            model.addAttribute("errors", errorRepository.findByStudent(user));
            return "students/detail";
        }

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
            ra.addFlashAttribute("error", "Username '" + username + "' already exists.");
            return "redirect:/users/create";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Email '" + email + "' is already in use.");
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
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log("USER_CREATED", "USER", user.getId(),
                "Created user '" + user.getUsername() + "' with role " + user.getRole() + " and status " + user.getStatus() + ".");

        ra.addFlashAttribute("success", "Account '" + username + "' created successfully.");
        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "User not found."); return "redirect:/users"; }
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
        if (opt.isEmpty()) { ra.addFlashAttribute("error", "User not found."); return "redirect:/users"; }

        User user = opt.get();
        String oldEmail = user.getEmail();
        Role oldRole = user.getRole();
        String oldStatus = user.getStatus();
        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent() && !byEmail.get().getId().equals(id)) {
            ra.addFlashAttribute("error", "This email is already used by another account.");
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
        auditLogService.log("USER_UPDATED", "USER", user.getId(),
                "Updated user '" + user.getUsername() + "' from email " + safe(oldEmail)
                        + ", role " + safe(oldRole != null ? oldRole.name() : null)
                        + ", status " + safe(oldStatus)
                        + " to email " + safe(user.getEmail())
                        + ", role " + safe(user.getRole() != null ? user.getRole().name() : null)
                        + ", status " + safe(user.getStatus()) + ".");
        ra.addFlashAttribute("success", "Account updated successfully.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/users";
        }
        User user = userOpt.get();
        user.setStatus("INACTIVE");
        userRepository.save(user);
        auditLogService.log("USER_DEACTIVATED", "USER", user.getId(),
                "Deactivated user '" + user.getUsername() + "'.");
        ra.addFlashAttribute("success", "Account deactivated successfully.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/users";
        }
        User user = userOpt.get();
        user.setStatus("ACTIVE");
        userRepository.save(user);
        auditLogService.log("USER_ACTIVATED", "USER", user.getId(),
                "Activated user '" + user.getUsername() + "'.");
        ra.addFlashAttribute("success", "Account activated successfully.");
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            ra.addFlashAttribute("error", "User not found.");
            return "redirect:/users";
        }
        User user = userOpt.get();
        try {
            List<Enrollment> linkedEnrollments = enrollmentRepository.findAll().stream()
                    .filter(enrollment -> (enrollment.getStudent() != null
                            && id.equals(enrollment.getStudent().getId()))
                            || (enrollment.getAcademicStaff() != null
                            && id.equals(enrollment.getAcademicStaff().getId())))
                    .toList();
            List<StudentResult> results = resultRepository.findByStudent(user);
            List<StudentError> errors = errorRepository.findByStudent(user);

            if (!linkedEnrollments.isEmpty()) {
                enrollmentRepository.deleteAll(linkedEnrollments);
            }
            if (!results.isEmpty()) {
                resultRepository.deleteAll(results);
            }
            if (!errors.isEmpty()) {
                errorRepository.deleteAll(errors);
            }

            userRepository.delete(user);
            auditLogService.log("USER_DELETED", "USER", id,
                    "Deleted user '" + user.getUsername() + "' with role " + user.getRole() + ".");
            ra.addFlashAttribute("success", "Account deleted successfully.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "This account could not be deleted. Please remove linked records first.");
        }
        return "redirect:/users";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
