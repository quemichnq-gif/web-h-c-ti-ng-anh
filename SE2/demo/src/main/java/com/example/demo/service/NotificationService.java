package com.example.demo.service;

import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.Role;
import com.example.demo.model.StudentResult;
import com.example.demo.model.User;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.StudentResultRepository;
import com.example.demo.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationService {

    private final EnrollmentRepository enrollmentRepository;
    private final TestRepository testRepository;
    private final StudentResultRepository studentResultRepository;

    public NotificationService(EnrollmentRepository enrollmentRepository,
                               TestRepository testRepository,
                               StudentResultRepository studentResultRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.testRepository = testRepository;
        this.studentResultRepository = studentResultRepository;
    }

    public List<NotificationItem> buildNotifications(User current) {
        if (current == null) {
            return List.of();
        }

        List<NotificationItem> items = new ArrayList<>();
        String name = current.getFullName() != null && !current.getFullName().isBlank()
                ? current.getFullName()
                : current.getUsername();

        items.add(new NotificationItem(
                "welcome_back",
                "Welcome back",
                "Just now",
                "Hello " + name + ", your workspace is ready.",
                "/",
                "info"));

        if (current.getRole() == Role.STUDENT) {
            long approvedEnrollments = enrollmentRepository.findByStudent(current).stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.APPROVED)
                    .count();
            List<StudentResult> recentResults = studentResultRepository.findByStudent(current);

            items.add(new NotificationItem(
                    "student_enrollments_" + approvedEnrollments,
                    "Course access updated",
                    "Today",
                    "You currently have " + approvedEnrollments + " approved course enrollment(s).",
                    "/portal/courses",
                    "success"));
            items.add(new NotificationItem(
                    "student_results_" + recentResults.size(),
                    "Assessment results available",
                    "Today",
                    "You have " + recentResults.size() + " test result record(s) available.",
                    "/portal/tests",
                    "success"));
        } else {
            items.add(new NotificationItem(
                    "pending_enrollments_" + enrollmentRepository.countByStatus(EnrollmentStatus.PENDING),
                    "Pending enrollments",
                    "Today",
                    "There are " + enrollmentRepository.countByStatus(EnrollmentStatus.PENDING) + " pending enrollment request(s).",
                    "/enrollments",
                    "warning"));
            items.add(new NotificationItem(
                    "assessments_ready_" + testRepository.count(),
                    "Assessments ready",
                    "Today",
                    "The portal currently has " + testRepository.count() + " assessment(s) available.",
                    "/assessments",
                    "info"));
        }

        items.add(new NotificationItem(
                "security_reminder",
                "Security reminder",
                "Today",
                "Use a strong password and keep your account details up to date.",
                "/member/settings",
                "danger"));

        return items;
    }

    public record NotificationItem(String key,
                                   String title,
                                   String time,
                                   String detail,
                                   String link,
                                   String tone) {
    }
}
