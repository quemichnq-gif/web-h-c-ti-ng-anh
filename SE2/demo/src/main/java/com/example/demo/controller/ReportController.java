package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
    public String viewReports(Model model) {
        // Tab 1: User Statistics
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long staffCount = userRepository.countByRole(Role.ACADEMIC_STAFF);
        long studentCount = userRepository.countByRole(Role.STUDENT);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("staffCount", staffCount);
        model.addAttribute("studentCount", studentCount);

        long activeUsers = userRepository.countByStatus("ACTIVE");
        long inactiveUsers = userRepository.countByStatus("INACTIVE");
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("inactiveUsers", inactiveUsers);

        // Tab 2: Course Statistics
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

        // Tab 3: Error Statistics
        List<ErrorType> errorTypes = errorTypeRepository.findAll();
        Map<String, Long> errorTypeCounts = errorTypes.stream()
                .collect(Collectors.toMap(ErrorType::getName, et -> studentErrorRepository.findAll().stream()
                        .filter(se -> se.getErrorType().getId().equals(et.getId())).count()));

        List<Map.Entry<String, Long>> topErrors = errorTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .toList();
        model.addAttribute("topErrors", topErrors);

        // Tab 4: Assessment Statistics
        List<Test> allTests = testRepository.findAll();
        Map<Long, Double> avgScores = allTests.stream()
                .collect(Collectors.toMap(Test::getId, t -> {
                    Double avg = studentResultRepository.findAverageScoreByTestId(t.getId());
                    return avg != null ? avg : 0.0;
                }));
        model.addAttribute("avgScores", avgScores);
        model.addAttribute("allTests", allTests);

        // Students with low scores (under 6)
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