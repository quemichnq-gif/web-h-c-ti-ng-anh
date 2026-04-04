package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/students")
public class StudentController {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentResultRepository resultRepository;
    private final StudentErrorRepository errorRepository;

    public StudentController(UserRepository userRepository,
                             EnrollmentRepository enrollmentRepository,
                             StudentResultRepository resultRepository,
                             StudentErrorRepository errorRepository) {
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.resultRepository = resultRepository;
        this.errorRepository = errorRepository;
    }

    @GetMapping
    public String list(Model model, @RequestParam(required = false) String search) {
        List<User> students = userRepository.findByRole(Role.STUDENT);
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            students = students.stream()
                    .filter(s -> (s.getFullName() != null && s.getFullName().toLowerCase().contains(q)) ||
                                 s.getUsername().toLowerCase().contains(q))
                    .toList();
        }
        model.addAttribute("students", students);
        model.addAttribute("search", search);
        return "students/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<User> studentOpt = userRepository.findById(id);
        if (studentOpt.isEmpty() || studentOpt.get().getRole() != Role.STUDENT) {
            ra.addFlashAttribute("error", "Student not found.");
            return "redirect:/students";
        }

        User student = studentOpt.get();
        model.addAttribute("user", student);
        model.addAttribute("student", student);
        model.addAttribute("enrollments", enrollmentRepository.findByStudent(student));
        model.addAttribute("results", resultRepository.findByStudent(student));
        model.addAttribute("errors", errorRepository.findByStudent(student));

        return "students/detail";
    }
}
