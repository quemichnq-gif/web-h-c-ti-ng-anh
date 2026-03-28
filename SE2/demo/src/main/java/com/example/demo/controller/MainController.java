package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
public class MainController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TestRepository testRepository;
    private final StudentErrorRepository studentErrorRepository;
    private final StudentResultRepository resultRepository;
    private final QuestionRepository questionRepository;

    public MainController(UserRepository userRepository, CourseRepository courseRepository,
                          EnrollmentRepository enrollmentRepository, TestRepository testRepository,
                          StudentErrorRepository studentErrorRepository, StudentResultRepository resultRepository,
                          QuestionRepository questionRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.testRepository = testRepository;
        this.studentErrorRepository = studentErrorRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
    }

    @GetMapping("/")
    public String dashboard(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";
        
        boolean isStudent = hasRole(auth, "ROLE_STUDENT");
        User currentUser = getCurrentUser(auth);
        model.addAttribute("currentUser", currentUser);

        if (isStudent && currentUser != null) {
            model.addAttribute("enrollmentCount", enrollmentRepository.countByStudent(currentUser));
            model.addAttribute("resultCount", resultRepository.findByStudent(currentUser).size());
            Double avg = resultRepository.findAverageScoreByStudent(currentUser);
            model.addAttribute("avgScore", avg != null ? String.format("%.1f", avg) : "0.0");
            model.addAttribute("recentResults", resultRepository.findTop5ByStudentOrderBySubmittedAtDesc(currentUser));
            model.addAttribute("myErrors", studentErrorRepository.findByStudent(currentUser));
            return "student/dashboard";
        }

        // Admin/Staff logic
        model.addAttribute("isAdmin", hasRole(auth, "ROLE_ADMIN"));
        model.addAttribute("isStaff", hasRole(auth, "ROLE_ACADEMIC_STAFF"));
        model.addAttribute("pendingEnrollmentCount", enrollmentRepository.countByStatus("PENDING"));
        model.addAttribute("activeTestCount", testRepository.count());
        
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(23, 59, 59);
        model.addAttribute("todayErrorCount", studentErrorRepository.countByCreatedAtBetween(start, end));

        if (hasRole(auth, "ROLE_ADMIN")) {
            model.addAttribute("userCount", userRepository.count());
            model.addAttribute("courseCount", courseRepository.count());
            model.addAttribute("recentErrors", studentErrorRepository.findAllOrderByCreatedAtDesc().stream().limit(10).toList());
        } else {
            model.addAttribute("unassignedErrorCount", studentErrorRepository.countStudentsWithUnassignedErrors());
            model.addAttribute("pendingEnrollments", enrollmentRepository.findByStatus("PENDING").stream().limit(10).toList());
        }
        return "dashboard";
    }

    // --- Student Portal Features (Path prefix ensured) ---
    @GetMapping({"/portal/courses", "/student/courses"})
    public String myCourses(Model model, Authentication auth) {
        User student = getCurrentUser(auth);
        model.addAttribute("currentUser", student);
        
        Map<Long, String> enrollmentStatusMap = new HashMap<>();
        if (student != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
            if (enrollments != null) {
                for (Enrollment e : enrollments) {
                    if (e != null && e.getCourse() != null && e.getStatus() != null) {
                        enrollmentStatusMap.put(e.getCourse().getId(), e.getStatus());
                    }
                }
            }
        }
        
        model.addAttribute("enrollmentStatusMap", enrollmentStatusMap);
        model.addAttribute("allCourses", courseRepository.findAll());
        return "student/courses";
    }

    @PostMapping({"/portal/enroll", "/student/enroll"})
    public String enroll(@RequestParam Long courseId, Authentication auth, RedirectAttributes ra) {
        User student = getCurrentUser(auth);
        courseRepository.findById(courseId).ifPresent(c -> {
            if (enrollmentRepository.findByStudentAndCourse(student, c).isEmpty()) {
                Enrollment e = new Enrollment();
                e.setStudent(student);
                e.setCourse(c);
                e.setStatus("PENDING");
                enrollmentRepository.save(e);
                ra.addFlashAttribute("success", "Đã gửi yêu cầu đăng ký khóa học " + c.getName());
            } else {
                ra.addFlashAttribute("error", "Bạn đã đăng ký khóa học này rồi.");
            }
        });
        return "redirect:/portal/courses";
    }

    @GetMapping({"/portal/tests", "/student/tests"})
    public String myTests(Model model, Authentication auth) {
        User student = getCurrentUser(auth);
        model.addAttribute("currentUser", student);
        
        List<Test> availableTests = new ArrayList<>();
        if (student != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
            if (enrollments != null) {
                for (Enrollment e : enrollments) {
                    if (e != null && "APPROVED".equals(e.getStatus()) && e.getCourse() != null) {
                        availableTests.addAll(testRepository.findByCourseId(e.getCourse().getId()));
                    }
                }
            }
        }
        
        model.addAttribute("tests", availableTests);
        model.addAttribute("results", student != null ? resultRepository.findByStudent(student) : new ArrayList<>());
        return "student/tests";
    }

    @GetMapping({"/portal/tests/{id}/take", "/student/tests/{id}/take"})
    public String takeTest(@PathVariable Long id, Model model, Authentication auth, RedirectAttributes ra) {
        Optional<Test> testOpt = testRepository.findById(id);
        if (testOpt.isEmpty()) return "redirect:/portal/tests";
        
        User student = getCurrentUser(auth);
        model.addAttribute("currentUser", student);
        
        boolean enrolled = false;
        if (student != null) {
            enrolled = enrollmentRepository.findByStudentAndCourse(student, testOpt.get().getCourse())
                .stream().anyMatch(e -> "APPROVED".equals(e.getStatus()));
        }
        
        if (!enrolled) {
            ra.addFlashAttribute("error", "Vui lòng đợi Giáo vụ duyệt đăng ký khóa học.");
            return "redirect:/portal/tests";
        }
        model.addAttribute("test", testOpt.get());
        model.addAttribute("questions", questionRepository.findByTestId(id));
        return "student/take-test";
    }

    @PostMapping({"/portal/tests/{id}/submit", "/student/tests/{id}/submit"})
    public String submitTest(@PathVariable Long id, @RequestParam Map<String, String> answers, Authentication auth, RedirectAttributes ra) {
        Optional<Test> testOpt = testRepository.findById(id);
        if (testOpt.isPresent()) {
            List<Question> questions = questionRepository.findByTestId(id);
            int correct = 0;
            for (Question q : questions) {
                String ans = answers.get("q_" + q.getId());
                if (ans != null && ans.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) correct++;
            }
            double score = questions.isEmpty() ? 0 : (double) correct / questions.size() * 10.0;
            StudentResult res = new StudentResult();
            res.setStudent(getCurrentUser(auth));
            res.setTest(testOpt.get());
            res.setScore(score);
            resultRepository.save(res);
            ra.addFlashAttribute("success", "Đã nộp bài thành công! Điểm: " + String.format("%.1f", score));
        }
        return "redirect:/portal/tests";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseGet(() -> userRepository.findByEmail(auth.getName()).orElse(null));
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }
}
