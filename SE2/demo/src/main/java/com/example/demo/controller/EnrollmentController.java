package com.example.demo.controller;

import com.example.demo.model.Course;
import com.example.demo.model.CourseStatus;
import com.example.demo.model.Enrollment;
import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public EnrollmentController(EnrollmentRepository enrollmentRepository,
                                CourseRepository courseRepository,
                                UserRepository userRepository,
                                AuditLogService auditLogService) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
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
        model.addAttribute("pendingCount", enrollmentRepository.countByStatus(EnrollmentStatus.PENDING));
        model.addAttribute("pendingEnrollments", enrollmentRepository.findByStatus(EnrollmentStatus.PENDING));
        model.addAttribute("statusOptions", EnrollmentStatus.values());

        List<Enrollment> enrollments;
        EnrollmentStatus filterStatus = parseStatus(status);

        if (courseId != null) {
            if (filterStatus != null) {
                enrollments = enrollmentRepository.findByCourseIdAndStatus(courseId, filterStatus);
            } else {
                enrollments = enrollmentRepository.findByCourseId(courseId);
            }
        } else {
            if (filterStatus != null) {
                enrollments = enrollmentRepository.findByStatus(filterStatus);
            } else {
                enrollments = enrollmentRepository.findAll();
            }
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            enrollments = enrollments.stream()
                    .filter(e -> (e.getStudent().getFullName() != null && e.getStudent().getFullName().toLowerCase().contains(q))
                            || (e.getStudent().getUsername() != null && e.getStudent().getUsername().toLowerCase().contains(q))
                            || e.getStudent().getEmail().toLowerCase().contains(q))
                    .toList();
        }

        model.addAttribute("enrollments", enrollments);

        model.addAttribute("students", userRepository.findByRole(Role.STUDENT));
        return "enrollments/list";
    }

    @PostMapping("/create")
    public String create(@RequestParam Long studentId,
                         @RequestParam Long courseId,
                         Authentication authentication,
                         RedirectAttributes ra) {
        Optional<User> student = userRepository.findById(studentId);
        Optional<Course> course = courseRepository.findById(courseId);

        if (student.isEmpty() || course.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid student or course.");
            return "redirect:/enrollments";
        }

        if (student.get().getRole() != Role.STUDENT) {
            ra.addFlashAttribute("error", "Enrollments can only be created for student accounts.");
            return "redirect:/enrollments";
        }

        if (!isCourseOpenForEnrollment(course.get())) {
            ra.addFlashAttribute("error", "This course is not open for enrollment.");
            return "redirect:/enrollments";
        }

        if (enrollmentRepository.existsByStudentAndCourse(student.get(), course.get())) {
            ra.addFlashAttribute("error", "This student is already enrolled in the course.");
            return "redirect:/enrollments?courseId=" + courseId;
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student.get());
        enrollment.setCourse(course.get());
        enrollment.setStatus(EnrollmentStatus.PENDING);
        enrollment.setAcademicStaff(resolveAcademicStaff(authentication).orElse(null));

        try {
            enrollmentRepository.save(enrollment);
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "This enrollment already exists.");
            return "redirect:/enrollments?courseId=" + courseId;
        }
        auditLogService.log("ENROLLMENT_CREATED", "ENROLLMENT", enrollment.getId(),
                "Created quick enrollment for student '" + enrollment.getStudent().getUsername()
                        + "' in course '" + enrollment.getCourse().getCode() + "' with status " + enrollment.getStatus() + ".");

        ra.addFlashAttribute("success", "Enrollment created successfully.");
        return "redirect:/enrollments?courseId=" + courseId;
    }

    @PostMapping("/create-detailed")
    public String createDetailed(@RequestParam Long studentId,
                                 @RequestParam Long courseId,
                                 @RequestParam String enrolledAt,
                                 @RequestParam String status,
                                 @RequestParam(required = false) String note,
                                 @RequestParam(required = false) String rejectReason,
                                 Authentication authentication,
                                 RedirectAttributes ra) {
        Optional<User> student = userRepository.findById(studentId);
        Optional<Course> course = courseRepository.findById(courseId);
        Optional<User> actor = resolveAcademicStaff(authentication);
        EnrollmentStatus enrollmentStatus = parseStatus(status);

        if (student.isEmpty() || course.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid student or course.");
            return "redirect:/enrollments";
        }
        if (actor.isEmpty()) {
            ra.addFlashAttribute("error", "Could not determine the current staff or admin account.");
            return "redirect:/enrollments";
        }
        if (student.get().getRole() != Role.STUDENT) {
            ra.addFlashAttribute("error", "Enrollments can only be created for student accounts.");
            return "redirect:/enrollments";
        }
        if (enrollmentStatus == null) {
            ra.addFlashAttribute("error", "Invalid enrollment status.");
            return "redirect:/enrollments";
        }
        if (!isCourseOpenForEnrollment(course.get())) {
            ra.addFlashAttribute("error", "This course is not open for enrollment.");
            return "redirect:/enrollments";
        }
        if (enrollmentRepository.existsByStudentAndCourse(student.get(), course.get())) {
            ra.addFlashAttribute("error", "This student is already enrolled in the course.");
            return "redirect:/enrollments?courseId=" + courseId;
        }

        LocalDateTime requestedAt;
        try {
            requestedAt = LocalDateTime.parse(enrolledAt);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Invalid enrollment date and time.");
            return "redirect:/enrollments";
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student.get());
        enrollment.setCourse(course.get());
        enrollment.setAcademicStaff(actor.get());
        enrollment.setEnrolledAt(requestedAt);
        enrollment.setStatus(enrollmentStatus);
        enrollment.setNote(cleanText(note));

        if (enrollmentStatus == EnrollmentStatus.REJECTED) {
            enrollment.setRejectReason(cleanText(rejectReason));
            enrollment.setProcessedAt(LocalDateTime.now());
        } else if (enrollmentStatus == EnrollmentStatus.APPROVED) {
            enrollment.setRejectReason(null);
            enrollment.setProcessedAt(LocalDateTime.now());
        }

        try {
            enrollmentRepository.save(enrollment);
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "This enrollment already exists.");
            return "redirect:/enrollments?courseId=" + courseId;
        }
        auditLogService.log("ENROLLMENT_CREATED", "ENROLLMENT", enrollment.getId(),
                "Created detailed enrollment for student '" + enrollment.getStudent().getUsername()
                        + "' in course '" + enrollment.getCourse().getCode() + "' with status " + enrollment.getStatus() + ".");

        ra.addFlashAttribute("success", "Detailed enrollment created successfully.");
        return "redirect:/enrollments?courseId=" + courseId;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @RequestParam(required = false) Long courseId,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String search,
                          Authentication authentication,
                          RedirectAttributes ra) {
        Optional<Enrollment> enrollment = enrollmentRepository.findById(id);
        if (enrollment.isEmpty()) {
            ra.addFlashAttribute("error", "Enrollment not found.");
            return redirectEnrollments(courseId, status, search);
        }

        Enrollment current = enrollment.get();
        if (current.getStatus() != EnrollmentStatus.PENDING) {
            ra.addFlashAttribute("error", "Only PENDING enrollments can be approved.");
            return redirectEnrollments(courseId, status, search);
        }

        if (!isCourseOpenForEnrollment(current.getCourse())) {
            ra.addFlashAttribute("error", "Cannot approve an enrollment for a closed course.");
            return redirectEnrollments(courseId, status, search);
        }

        current.setStatus(EnrollmentStatus.APPROVED);
        current.setAcademicStaff(resolveAcademicStaff(authentication).orElse(current.getAcademicStaff()));
        current.setRejectReason(null);
        current.setProcessedAt(LocalDateTime.now());
        enrollmentRepository.save(current);
        auditLogService.log("ENROLLMENT_APPROVED", "ENROLLMENT", current.getId(),
                "Approved enrollment for student '" + current.getStudent().getUsername()
                        + "' in course '" + current.getCourse().getCode() + "'.");
        ra.addFlashAttribute("success", "Enrollment approved successfully.");
        return redirectEnrollments(courseId, status, search);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) Long courseId,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String search,
                         Authentication authentication,
                         RedirectAttributes ra) {
        Optional<Enrollment> enrollment = enrollmentRepository.findById(id);
        if (enrollment.isEmpty()) {
            ra.addFlashAttribute("error", "Enrollment not found.");
            return redirectEnrollments(courseId, status, search);
        }

        Enrollment current = enrollment.get();
        if (current.getStatus() != EnrollmentStatus.PENDING) {
            ra.addFlashAttribute("error", "Only PENDING enrollments can be rejected.");
            return redirectEnrollments(courseId, status, search);
        }

        current.setStatus(EnrollmentStatus.REJECTED);
        current.setAcademicStaff(resolveAcademicStaff(authentication).orElse(current.getAcademicStaff()));
        current.setRejectReason(cleanText(reason));
        current.setProcessedAt(LocalDateTime.now());
        enrollmentRepository.save(current);
        auditLogService.log("ENROLLMENT_REJECTED", "ENROLLMENT", current.getId(),
                "Rejected enrollment for student '" + current.getStudent().getUsername()
                        + "' in course '" + current.getCourse().getCode() + "'. Reason: " + safe(current.getRejectReason()));
        ra.addFlashAttribute("success", "Enrollment rejected successfully.");
        return redirectEnrollments(courseId, status, search);
    }

    @PostMapping("/{id}/remove")
    public String remove(@PathVariable Long id,
                         @RequestParam(required = false) Long courseId,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String search,
                         Authentication authentication,
                         RedirectAttributes ra) {
        Optional<Enrollment> enrollment = enrollmentRepository.findById(id);
        if (enrollment.isEmpty()) {
            ra.addFlashAttribute("error", "Enrollment not found.");
            return redirectEnrollments(courseId, status, search);
        }

        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            ra.addFlashAttribute("error", "Only admins can delete enrollments.");
            return redirectEnrollments(courseId, status, search);
        }

        Enrollment current = enrollment.get();
        enrollmentRepository.delete(current);
        auditLogService.log("ENROLLMENT_DELETED", "ENROLLMENT", current.getId(),
                "Deleted enrollment for student '" + current.getStudent().getUsername()
                        + "' in course '" + current.getCourse().getCode() + "'.");
        ra.addFlashAttribute("success", "Enrollment deleted successfully.");
        return redirectEnrollments(courseId, status, search);
    }

    private String redirectEnrollments(Long courseId, String status, String search) {
        StringBuilder redirect = new StringBuilder("redirect:/enrollments");
        boolean hasQuery = false;
        if (courseId != null) {
            redirect.append("?courseId=").append(courseId);
            hasQuery = true;
        }
        if (status != null && !status.isBlank()) {
            redirect.append(hasQuery ? "&" : "?").append("status=").append(status);
            hasQuery = true;
        }
        if (search != null && !search.isBlank()) {
            redirect.append(hasQuery ? "&" : "?").append("search=").append(search);
        }
        return redirect.toString();
    }

    private Optional<User> resolveAcademicStaff(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        Optional<User> actor = userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()));
        if (actor.isEmpty()) {
            return Optional.empty();
        }
        Role role = actor.get().getRole();
        return role == Role.ADMIN || role == Role.ACADEMIC_STAFF ? actor : Optional.empty();
    }

    private String cleanText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private EnrollmentStatus parseStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        try {
            return EnrollmentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isCourseOpenForEnrollment(Course course) {
        if (course.getStatus() != CourseStatus.OPEN) {
            return false;
        }

        LocalDate today = LocalDate.now();
        // Allow enrollment as long as the course hasn't ended.
        // It's normal to enroll BEFORE the start date.
        return course.getEndDate() == null || !course.getEndDate().isBefore(today);
    }
}

