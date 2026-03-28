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
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    public EnrollmentController(EnrollmentRepository enrollmentRepository,
                                CourseRepository courseRepository, UserRepository userRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String search) {
        List<Course> courses = courseRepository.findAll();
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("filterStatus", status);
        model.addAttribute("search", search);

        long pendingCount = enrollmentRepository.countByStatus("PENDING");
        model.addAttribute("pendingCount", pendingCount);

        if (courseId != null) {
            List<Enrollment> enrollments;
            if (status != null && !status.isBlank() && !status.equals("ALL")) {
                enrollments = enrollmentRepository.findByCourseIdAndStatus(courseId, status);
            } else {
                enrollments = enrollmentRepository.findByCourseId(courseId);
            }

            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                enrollments = enrollments.stream()
                        .filter(e -> (e.getStudent().getFullName() != null && e.getStudent().getFullName().toLowerCase().contains(q))
                                || e.getStudent().getEmail().toLowerCase().contains(q))
                        .toList();
            }

            model.addAttribute("enrollments", enrollments);
        }

        model.addAttribute("students", userRepository.findByRole(Role.STUDENT));
        return "enrollments/list";
    }

    @PostMapping("/create")
    public String create(@RequestParam Long studentId, @RequestParam Long courseId, RedirectAttributes ra) {
        Optional<User> student = userRepository.findById(studentId);
        Optional<Course> course = courseRepository.findById(courseId);

        if (student.isEmpty() || course.isEmpty()) {
            ra.addFlashAttribute("error", "Student hoặc Course không hợp lệ.");
            return "redirect:/enrollments";
        }

        if (enrollmentRepository.findByStudentAndCourse(student.get(), course.get()).isPresent()) {
            ra.addFlashAttribute("error", "Student này đã được enroll vào course rồi.");
            return "redirect:/enrollments";
        }

        Enrollment e = new Enrollment();
        e.setStudent(student.get());
        e.setCourse(course.get());
        e.setStatus("PENDING");
        enrollmentRepository.save(e);
        ra.addFlashAttribute("success", "Đã tạo enrollment thành công!");
        return "redirect:/enrollments?courseId=" + courseId;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes ra) {
        enrollmentRepository.findById(id).ifPresent(e -> {
            e.setStatus("APPROVED");
            enrollmentRepository.save(e);
        });
        ra.addFlashAttribute("success", "Đã approve enrollment.");
        return "redirect:/enrollments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        enrollmentRepository.findById(id).ifPresent(e -> {
            e.setStatus("REJECTED");
            e.setRejectReason(reason);
            enrollmentRepository.save(e);
        });
        ra.addFlashAttribute("success", "Đã reject enrollment.");
        return "redirect:/enrollments";
    }

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable Long id, RedirectAttributes ra) {
        enrollmentRepository.deleteById(id);
        ra.addFlashAttribute("success", "Đã xóa enrollment.");
        return "redirect:/enrollments";
    }
}
