package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

@Controller
public class MainController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TestRepository testRepository;
    private final StudentErrorRepository studentErrorRepository;
    private final StudentResultRepository resultRepository;
    private final QuestionRepository questionRepository;
    private final ErrorTestMappingRepository errorTestMappingRepository;
    private final LessonRepository lessonRepository;

    public MainController(UserRepository userRepository, CourseRepository courseRepository,
                          EnrollmentRepository enrollmentRepository, TestRepository testRepository,
                          StudentErrorRepository studentErrorRepository, StudentResultRepository resultRepository,
                          QuestionRepository questionRepository, ErrorTestMappingRepository errorTestMappingRepository,
                          LessonRepository lessonRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.testRepository = testRepository;
        this.studentErrorRepository = studentErrorRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
        this.errorTestMappingRepository = errorTestMappingRepository;
        this.lessonRepository = lessonRepository;
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

        model.addAttribute("isAdmin", hasRole(auth, "ROLE_ADMIN"));
        model.addAttribute("isStaff", hasRole(auth, "ROLE_ACADEMIC_STAFF"));
        model.addAttribute("pendingEnrollmentCount", enrollmentRepository.countByStatus(EnrollmentStatus.PENDING));
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
            model.addAttribute("pendingEnrollments", enrollmentRepository.findByStatus(EnrollmentStatus.PENDING).stream().limit(10).toList());
        }
        return "dashboard";
    }

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
                        enrollmentStatusMap.put(e.getCourse().getId(), e.getStatus().name());
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
        if (student == null || student.getRole() != Role.STUDENT) {
            ra.addFlashAttribute("error", "Only students can register for courses.");
            return "redirect:/portal/courses";
        }

        Optional<Course> course = courseRepository.findById(courseId);
        if (course.isEmpty()) {
            ra.addFlashAttribute("error", "Course not found.");
            return "redirect:/portal/courses";
        }

        if (!isCourseOpenForEnrollment(course.get())) {
            ra.addFlashAttribute("error", "This course is not open for registration.");
            return "redirect:/portal/courses";
        }

        if (enrollmentRepository.existsByStudentAndCourse(student, course.get())) {
            ra.addFlashAttribute("error", "You have already registered for this course.");
            return "redirect:/portal/courses";
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course.get());
        enrollment.setStatus(EnrollmentStatus.PENDING);

        try {
            enrollmentRepository.save(enrollment);
            ra.addFlashAttribute("success", "Enrollment request submitted for " + course.get().getName());
        } catch (DataIntegrityViolationException ex) {
            ra.addFlashAttribute("error", "You have already registered for this course.");
        }
        return "redirect:/portal/courses";
    }

    @GetMapping({"/portal/tests", "/student/tests"})
    public String myTests(Model model, Authentication auth) {
        User student = getCurrentUser(auth);
        model.addAttribute("currentUser", student);

        List<Test> courseAssessments = new ArrayList<>();
        List<Test> remedialTests = new ArrayList<>();
        if (student != null) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(student);
            if (enrollments != null) {
                for (Enrollment e : enrollments) {
                    if (e != null && e.getStatus() == EnrollmentStatus.APPROVED && e.getCourse() != null) {
                        courseAssessments.addAll(testRepository.findByCourseIdAndAssessmentType(e.getCourse().getId(), AssessmentType.COURSE_ASSESSMENT));
                    }
                }
            }
            remedialTests = findRecommendedRemedialTests(student);
        }

        model.addAttribute("tests", mergeDistinctTests(courseAssessments, remedialTests));
        model.addAttribute("courseAssessments", mergeDistinctTests(courseAssessments, List.of()));
        model.addAttribute("remedialTests", mergeDistinctTests(remedialTests, List.of()));
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
            Test test = testOpt.get();
            if (test.isRemedialTest()) {
                enrolled = canAccessRemedialTest(student, test);
            } else {
                enrolled = enrollmentRepository.findByStudentAndCourse(student, test.getCourse())
                        .stream().anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED);
            }
        }

        if (!enrolled) {
            ra.addFlashAttribute("error", "You do not have access to this test.");
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
            List<ResultQuestionDetail> details = new ArrayList<>();
            int questionNumber = 1;
            for (Question q : questions) {
                String ans = normalizeAnswer(answers.get("q_" + q.getId()));
                boolean isCorrect = ans != null && ans.equalsIgnoreCase(normalizeAnswer(q.getCorrectAnswer()));
                if (isCorrect) {
                    correct++;
                }
                details.add(buildDetail(q, questionNumber++, ans, isCorrect));
            }
            double score = questions.isEmpty() ? 0 : (double) correct / questions.size() * 10.0;
            StudentResult res = new StudentResult();
            res.setStudent(getCurrentUser(auth));
            res.setTest(testOpt.get());
            res.setScore(score);
            res.setAnswerDetails(details);
            resultRepository.save(res);
            ra.addFlashAttribute("success", "Test submitted successfully. Score: " + String.format("%.1f", score));
        }
        return "redirect:/portal/tests";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseGet(() -> userRepository.findByEmail(auth.getName()).orElse(null));
    }

    private ResultQuestionDetail buildDetail(Question question, int questionNumber, String studentAnswer, boolean isCorrect) {
        ResultQuestionDetail detail = new ResultQuestionDetail();
        detail.setQuestionId(question.getId());
        detail.setQuestionNumber(questionNumber);
        detail.setQuestionType(question.getQuestionType().name());
        detail.setQuestionContent(question.getContent());
        detail.setCorrectAnswer(question.getCorrectAnswer());
        detail.setStudentAnswer(studentAnswer != null ? studentAnswer : "");
        detail.setCorrect(isCorrect);
        detail.setOptions(Stream.of(
                        optionLabel("A", question.getOptionA()),
                        optionLabel("B", question.getOptionB()),
                        optionLabel("C", question.getOptionC()),
                        optionLabel("D", question.getOptionD()))
                .filter(Objects::nonNull)
                .toList());
        return detail;
    }

    private String optionLabel(String key, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return key + ". " + value;
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return null;
        }
        String normalized = answer.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private boolean canAccessRemedialTest(User student, Test test) {
        if (student == null || test == null || test.getId() == null) {
            return false;
        }
        Set<Long> studentErrorTypeIds = studentErrorRepository.findByStudent(student).stream()
                .map(StudentError::getErrorType)
                .filter(Objects::nonNull)
                .map(ErrorType::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (studentErrorTypeIds.isEmpty()) {
            return false;
        }
        return errorTestMappingRepository.findByTestId(test.getId()).stream()
                .map(ErrorTestMapping::getErrorType)
                .filter(Objects::nonNull)
                .map(ErrorType::getId)
                .anyMatch(studentErrorTypeIds::contains)
                || lessonRepository.findByTestId(test.getId()).stream()
                .filter(lesson -> lesson.getErrorType() != null && lesson.getCourse() != null)
                .filter(lesson -> studentErrorTypeIds.contains(lesson.getErrorType().getId()))
                .anyMatch(lesson -> enrollmentRepository.findByStudentAndCourse(student, lesson.getCourse()).stream()
                        .anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED));
    }

    private List<Test> findRecommendedRemedialTests(User student) {
        if (student == null) {
            return new ArrayList<>();
        }
        Set<Long> studentErrorTypeIds = studentErrorRepository.findByStudent(student).stream()
                .map(StudentError::getErrorType)
                .filter(Objects::nonNull)
                .map(ErrorType::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (studentErrorTypeIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Test> mappedByErrorType = errorTestMappingRepository.findAll().stream()
                .filter(mapping -> mapping.getErrorType() != null && mapping.getTest() != null)
                .filter(mapping -> studentErrorTypeIds.contains(mapping.getErrorType().getId()))
                .map(ErrorTestMapping::getTest)
                .filter(Test::isRemedialTest)
                .toList();
        List<Test> mappedByLesson = lessonRepository.findAll().stream()
                .filter(lesson -> lesson.getTest() != null
                        && lesson.getErrorType() != null
                        && studentErrorTypeIds.contains(lesson.getErrorType().getId()))
                .map(Lesson::getTest)
                .filter(Test::isRemedialTest)
                .toList();
        return mergeDistinctTests(mappedByErrorType, mappedByLesson).stream()
                .sorted(Comparator.comparing(Test::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Test> mergeDistinctTests(List<Test> first, List<Test> second) {
        Map<Long, Test> merged = new LinkedHashMap<>();
        for (Test test : first) {
            if (test != null && test.getId() != null) {
                merged.put(test.getId(), test);
            }
        }
        for (Test test : second) {
            if (test != null && test.getId() != null) {
                merged.putIfAbsent(test.getId(), test);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private boolean isCourseOpenForEnrollment(Course course) {
        if (course.getStatus() != CourseStatus.OPEN) {
            return false;
        }

        LocalDate today = LocalDate.now();
        if (course.getStartDate() != null && course.getStartDate().isAfter(today)) {
            return false;
        }
        return course.getEndDate() == null || !course.getEndDate().isBefore(today);
    }
}

