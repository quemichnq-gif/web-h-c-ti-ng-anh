package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ReportController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentErrorRepository studentErrorRepository;
    private final StudentResultRepository studentResultRepository;
    private final TestRepository testRepository;
    private final ErrorTypeRepository errorTypeRepository;

    public ReportController(UserRepository userRepository, CourseRepository courseRepository,
                            EnrollmentRepository enrollmentRepository,
                            StudentErrorRepository studentErrorRepository,
                            StudentResultRepository studentResultRepository,
                            TestRepository testRepository,
                            ErrorTypeRepository errorTypeRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentErrorRepository = studentErrorRepository;
        this.studentResultRepository = studentResultRepository;
        this.testRepository = testRepository;
        this.errorTypeRepository = errorTypeRepository;
    }

    @GetMapping("/admin/reports")
    public String viewReports(Model model,
                              @RequestParam(defaultValue = "users") String tab) {
        Set<String> validTabs = Set.of("users", "courses", "errors", "assessments");
        String activeTab = validTabs.contains(tab) ? tab : "users";
        model.addAttribute("activeTab", activeTab);

        long adminCount = userRepository.countByRole(Role.ADMIN);
        long staffCount = userRepository.countByRole(Role.ACADEMIC_STAFF);
        long studentCount = userRepository.countByRole(Role.STUDENT);
        long totalUsers = adminCount + staffCount + studentCount;
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("staffCount", staffCount);
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("totalUsers", totalUsers);

        long activeUsers = userRepository.countByStatus("ACTIVE");
        long inactiveUsers = userRepository.countByStatus("INACTIVE");
        long activeRate = totalUsers == 0 ? 0 : Math.round((activeUsers * 100.0) / totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);
        model.addAttribute("activeRate", activeRate);

        long totalCourses = courseRepository.count();
        model.addAttribute("totalCourses", totalCourses);

        List<Course> allCourses = courseRepository.findAll();
        Map<Long, Long> enrollmentPerCourse = allCourses.stream()
                .collect(Collectors.toMap(Course::getId, enrollmentRepository::countByCourse));
        model.addAttribute("enrollmentPerCourse", enrollmentPerCourse);

        List<Course> topCourses = allCourses.stream()
                .sorted((c1, c2) -> enrollmentPerCourse.get(c2.getId()).compareTo(enrollmentPerCourse.get(c1.getId())))
                .limit(5)
                .toList();
        model.addAttribute("topCourses", topCourses);
        long maxCourseEnrollment = topCourses.stream()
                .map(Course::getId)
                .map(enrollmentPerCourse::get)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        model.addAttribute("maxCourseEnrollment", maxCourseEnrollment);

        List<ErrorType> errorTypes = errorTypeRepository.findAll();
        List<StudentError> allStudentErrors = studentErrorRepository.findAll();
        Map<String, Long> errorTypeCounts = errorTypes.stream()
                .collect(Collectors.toMap(ErrorType::getName, et -> allStudentErrors.stream()
                        .filter(se -> se.getErrorType() != null && Objects.equals(se.getErrorType().getId(), et.getId()))
                        .count()));

        List<Map.Entry<String, Long>> topErrors = errorTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .toList();
        model.addAttribute("topErrors", topErrors);
        long maxErrorOccurrences = topErrors.stream()
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        model.addAttribute("maxErrorOccurrences", maxErrorOccurrences);

        List<Test> allTests = testRepository.findAll();
        Map<Long, Double> avgScores = allTests.stream()
                .collect(Collectors.toMap(Test::getId, t -> {
                    Double avg = studentResultRepository.findAverageScoreByTestId(t.getId());
                    return avg != null ? avg : 0.0;
                }));
        model.addAttribute("avgScores", avgScores);
        model.addAttribute("allTests", allTests);

        List<StudentResult> lowResults = studentResultRepository.findAll().stream()
                .filter(r -> !r.isPassed())
                .limit(20)
                .toList();
        model.addAttribute("lowResults", lowResults);

        return "reports/view";
    }

    @GetMapping("/reports/my")
    public String myReport(Model model, Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        if (currentUser == null) return "redirect:/login";

        if (currentUser.getRole() == Role.STUDENT) {
            List<StudentResult> results = studentResultRepository.findByStudent(currentUser);
            List<StudentError> errors = studentErrorRepository.findByStudent(currentUser);
            long passCount = results.stream().filter(StudentResult::isPassed).count();
            long failCount = results.size() - passCount;
            Double avg = studentResultRepository.findAverageScoreByStudent(currentUser);

            model.addAttribute("results", results);
            model.addAttribute("errors", errors);
            model.addAttribute("passCount", passCount);
            model.addAttribute("failCount", failCount);
            model.addAttribute("avgScore", avg != null ? String.format("%.1f", avg) : "0.0");
            model.addAttribute("currentUser", currentUser);
            return "reports/my-report";
        }

        // Admin và Staff redirect về trang report đầy đủ
        return "redirect:/admin/reports";
    }
}
