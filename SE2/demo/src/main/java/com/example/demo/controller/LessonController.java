package com.example.demo.controller;

import com.example.demo.model.Course;
import com.example.demo.model.CourseStatus;
import com.example.demo.model.EnrollmentStatus;
import com.example.demo.model.BloomLevel;
import com.example.demo.model.ErrorType;
import com.example.demo.model.Lesson;
import com.example.demo.model.LessonQuizQuestion;
import com.example.demo.model.QuestionType;
import com.example.demo.model.StudentError;
import com.example.demo.model.Test;
import com.example.demo.model.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.ErrorTypeRepository;
import com.example.demo.repository.LessonQuizQuestionRepository;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.StudentErrorRepository;
import com.example.demo.repository.TestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.AuditLogService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Controller
public class LessonController {
    private static final long MAX_ATTACHMENT_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final List<String> ALLOWED_ATTACHMENT_CONTENT_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );
    private static final List<String> ALLOWED_IMAGE_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonQuizQuestionRepository lessonQuizQuestionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final StudentErrorRepository studentErrorRepository;
    private final TestRepository testRepository;
    private final AuditLogService auditLogService;
    private final Path uploadRoot = Paths.get("uploads", "lesson-files");
    private final Path imageUploadRoot = Paths.get("uploads", "lesson-images");

    public LessonController(CourseRepository courseRepository,
                            LessonRepository lessonRepository,
                            LessonQuizQuestionRepository lessonQuizQuestionRepository,
                            EnrollmentRepository enrollmentRepository,
                            UserRepository userRepository,
                            ErrorTypeRepository errorTypeRepository,
                            StudentErrorRepository studentErrorRepository,
                            TestRepository testRepository,
                            AuditLogService auditLogService) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.lessonQuizQuestionRepository = lessonQuizQuestionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.studentErrorRepository = studentErrorRepository;
        this.testRepository = testRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/lessons")
    public String listLessons(@RequestParam(required = false) Long courseId,
                              @RequestParam(required = false) String search,
                              Model model) {
        List<Course> courses = courseRepository.findAll().stream()
                .sorted(Comparator.comparing(course -> safe(course.getName())))
                .toList();

        List<Lesson> lessons = courseId != null
                ? lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId)
                : lessonRepository.findAll().stream()
                .sorted(Comparator
                        .comparing((Lesson lesson) -> lesson.getCourse() != null ? safe(lesson.getCourse().getName()) : "")
                        .thenComparing(lesson -> lesson.getSortOrder() != null ? lesson.getSortOrder() : 0)
                        .thenComparing(Lesson::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        if (search != null && !search.isBlank()) {
            String query = search.toLowerCase();
            lessons = lessons.stream()
                    .filter(lesson -> safe(lesson.getTitle()).contains(query)
                            || safe(lesson.getSummary()).contains(query)
                            || (lesson.getCourse() != null
                            && (safe(lesson.getCourse().getName()).contains(query)
                            || safe(lesson.getCourse().getCode()).contains(query))))
                    .toList();
        }

        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("search", search);
        model.addAttribute("lessonViews", buildLessonViews(lessons));
        return "lessons/list";
    }

    @GetMapping("/lessons/create")
    public String createLessonForm(@RequestParam(required = false) Long courseId, Model model) {
        model.addAttribute("courses", courseRepository.findAll().stream()
                .sorted(Comparator.comparing(course -> safe(course.getName())))
                .toList());
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("formAction", "/lessons/create");
        model.addAttribute("pageTitle", "Create Lesson");
        model.addAttribute("pageSubtitle", "Create lesson content, upload a study file, and attach review questions.");
        model.addAttribute("submitLabel", "Create Lesson");
        model.addAttribute("lesson", new Lesson());
        model.addAttribute("bloomLevels", BloomLevel.values());
        model.addAttribute("quizQuestions", List.<LessonQuizQuestion>of());
        return "lessons/form";
    }

    @PostMapping("/lessons/create")
    public String createLesson(@RequestParam Long courseId,
                               @RequestParam(required = false) List<String> bloomErrorTypeNames,
                               @RequestParam(required = false) List<String> bloomErrorTypeDescriptions,
                               @RequestParam String code,
                               @RequestParam String title,
                               @RequestParam(required = false) String summary,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "1") Integer sortOrder,
                               @RequestParam(required = false) MultipartFile lessonImage,
                               @RequestParam(required = false) MultipartFile attachment,
                               @RequestParam(required = false) List<String> quizQuestionTexts,
                               @RequestParam(required = false) List<String> optionAs,
                               @RequestParam(required = false) List<String> optionBs,
                               @RequestParam(required = false) List<String> optionCs,
                               @RequestParam(required = false) List<String> optionDs,
                               @RequestParam(required = false) List<String> questionTypes,
                               @RequestParam(required = false) List<String> correctAnswers,
                               @RequestParam(required = false) List<String> bloomLevels,
                               @RequestParam(required = false) List<String> explanations,
                               RedirectAttributes ra) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Course not found.");
            return "redirect:/lessons/create";
        }
        String normalizedCode = normalizeLessonCode(code);
        if (normalizedCode == null) {
            ra.addFlashAttribute("error", "Lesson code is required.");
            return "redirect:/lessons/create?courseId=" + courseId;
        }
        if (lessonRepository.existsByCodeIgnoreCase(normalizedCode)) {
            ra.addFlashAttribute("error", "Lesson code already exists.");
            return "redirect:/lessons/create?courseId=" + courseId;
        }
        if (!isCourseEditableForLessons(courseOpt.get())) {
            ra.addFlashAttribute("error", "Lessons can only be managed for draft, open, or in-progress courses that have not ended.");
            return "redirect:/lessons/create?courseId=" + courseId;
        }
        Lesson lesson = new Lesson();
        lesson.setCourse(courseOpt.get());
        lesson.setErrorType(null);
        lesson.setTest(null);
        applyBloomErrorTypes(lesson, bloomErrorTypeNames, bloomErrorTypeDescriptions);
        lesson.setCode(normalizedCode);
        lesson.setTitle(title);
        lesson.setSummary(summary);
        lesson.setContent(sanitizeLessonContent(content));
        try {
            shiftLessonsForPlacement(courseId, normalizeSortOrder(courseId, sortOrder, false), null, null);
            lesson.setSortOrder(normalizeSortOrder(courseId, sortOrder, false));
            saveLessonImage(lesson, lessonImage);
            saveAttachment(lesson, attachment);
            lessonRepository.save(lesson);
            saveLessonQuiz(lesson, quizQuestionTexts, optionAs, optionBs, optionCs, optionDs, questionTypes, correctAnswers, bloomLevels, explanations);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/lessons/create?courseId=" + courseId;
        }
        auditLogService.log("LESSON_CREATED", "LESSON", lesson.getId(),
                "Created lesson '" + lesson.getTitle() + "' (" + lesson.getCode()
                        + ") for course '" + safe(lesson.getCourse() != null ? lesson.getCourse().getCode() : null) + "'.");

        ra.addFlashAttribute("success", "Lesson '" + title + "' created successfully.");
        return "redirect:/lessons?courseId=" + courseId;
    }

    @GetMapping("/lessons/{lessonId}/edit")
    public String editLessonForm(@PathVariable Long lessonId, Model model, RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Lesson not found.");
            return "redirect:/lessons";
        }

        Lesson lesson = lessonOpt.get();
        model.addAttribute("courses", courseRepository.findAll().stream()
                .sorted(Comparator.comparing(course -> safe(course.getName())))
                .toList());
        model.addAttribute("selectedCourseId", lesson.getCourse() != null ? lesson.getCourse().getId() : null);
        model.addAttribute("formAction", "/lessons/" + lessonId + "/edit");
        model.addAttribute("pageTitle", "Edit Lesson");
        model.addAttribute("pageSubtitle", "Update lesson content, replace files, or refresh the review quiz.");
        model.addAttribute("submitLabel", "Save Changes");
        model.addAttribute("lesson", lesson);
        model.addAttribute("bloomLevels", BloomLevel.values());
        model.addAttribute("quizQuestions", lessonQuizQuestionRepository.findByLessonIdOrderBySortOrderAsc(lessonId));
        return "lessons/form";
    }

    @PostMapping("/lessons/{lessonId}/edit")
    public String updateLesson(@PathVariable Long lessonId,
                               @RequestParam Long courseId,
                               @RequestParam(required = false) List<String> bloomErrorTypeNames,
                               @RequestParam(required = false) List<String> bloomErrorTypeDescriptions,
                               @RequestParam String code,
                               @RequestParam String title,
                               @RequestParam(required = false) String summary,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "1") Integer sortOrder,
                               @RequestParam(required = false) MultipartFile lessonImage,
                               @RequestParam(required = false) MultipartFile attachment,
                               @RequestParam(required = false) List<String> quizQuestionTexts,
                               @RequestParam(required = false) List<String> optionAs,
                               @RequestParam(required = false) List<String> optionBs,
                               @RequestParam(required = false) List<String> optionCs,
                               @RequestParam(required = false) List<String> optionDs,
                               @RequestParam(required = false) List<String> questionTypes,
                               @RequestParam(required = false) List<String> correctAnswers,
                               @RequestParam(required = false) List<String> bloomLevels,
                               @RequestParam(required = false) List<String> explanations,
                               RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (lessonOpt.isEmpty() || courseOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Lesson or course not found.");
            return "redirect:/lessons";
        }
        String normalizedCode = normalizeLessonCode(code);
        if (normalizedCode == null) {
            ra.addFlashAttribute("error", "Lesson code is required.");
            return "redirect:/lessons/" + lessonId + "/edit";
        }
        if (lessonRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, lessonId)) {
            ra.addFlashAttribute("error", "Lesson code already exists.");
            return "redirect:/lessons/" + lessonId + "/edit";
        }
        if (!isCourseEditableForLessons(courseOpt.get())) {
            ra.addFlashAttribute("error", "Lessons can only be managed for draft, open, or in-progress courses that have not ended.");
            return "redirect:/lessons?courseId=" + courseId;
        }
        Lesson lesson = lessonOpt.get();
        String previousTitle = lesson.getTitle();
        String previousCode = lesson.getCode();
        String previousCourseCode = lesson.getCourse() != null ? lesson.getCourse().getCode() : null;
        Long previousCourseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        Integer previousSortOrder = lesson.getSortOrder();
        int targetSortOrder = normalizeSortOrder(courseId, sortOrder, !Objects.equals(previousCourseId, courseId));
        try {
            shiftLessonsForPlacement(courseId, targetSortOrder, previousCourseId, lesson);
            lesson.setCourse(courseOpt.get());
            lesson.setErrorType(null);
            lesson.setTest(null);
            applyBloomErrorTypes(lesson, bloomErrorTypeNames, bloomErrorTypeDescriptions);
            lesson.setCode(normalizedCode);
            lesson.setTitle(title);
            lesson.setSummary(summary);
            lesson.setContent(sanitizeLessonContent(content));
            lesson.setSortOrder(targetSortOrder);
            replaceLessonImageIfNeeded(lesson, lessonImage);
            replaceAttachmentIfNeeded(lesson, attachment);
            lessonRepository.save(lesson);

            lessonQuizQuestionRepository.deleteByLessonId(lessonId);
            saveLessonQuiz(lesson, quizQuestionTexts, optionAs, optionBs, optionCs, optionDs, questionTypes, correctAnswers, bloomLevels, explanations);
        } catch (IllegalArgumentException ex) {
            if (previousCourseId != null && previousSortOrder != null) {
                lesson.setCourse(courseRepository.findById(previousCourseId).orElse(lesson.getCourse()));
                lesson.setSortOrder(previousSortOrder);
            }
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/lessons/" + lessonId + "/edit";
        }
        auditLogService.log("LESSON_UPDATED", "LESSON", lesson.getId(),
                "Updated lesson from '" + safe(previousTitle) + "' (" + safe(previousCode) + ") in course '"
                        + safe(previousCourseCode) + "' to '" + lesson.getTitle() + "' (" + lesson.getCode()
                        + ") in course '" + safe(lesson.getCourse() != null ? lesson.getCourse().getCode() : null) + "'.");

        ra.addFlashAttribute("success", "Lesson '" + title + "' updated successfully.");
        return "redirect:/lessons?courseId=" + courseId;
    }

    @PostMapping("/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long lessonId, RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        Long courseId = null;
        if (lessonOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Lesson not found.");
            return "redirect:/lessons";
        }

        Lesson lesson = lessonOpt.get();
        courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
        int deletedOrder = lesson.getSortOrder();
        deleteStoredLessonImage(lesson);
        deleteStoredAttachment(lesson);
        lessonQuizQuestionRepository.deleteByLessonId(lessonId);
        lessonRepository.delete(lesson);
        reorderLessonsAfterDeletion(courseId, deletedOrder);
        auditLogService.log("LESSON_DELETED", "LESSON", lessonId,
                "Deleted lesson '" + lesson.getTitle() + "' (" + lesson.getCode()
                        + ") from course '" + safe(lesson.getCourse() != null ? lesson.getCourse().getCode() : null) + "'.");

        ra.addFlashAttribute("success", "Lesson deleted successfully.");
        return courseId != null ? "redirect:/lessons?courseId=" + courseId : "redirect:/lessons";
    }

    @GetMapping("/lessons/{lessonId}/file")
    public ResponseEntity<ByteArrayResource> downloadLessonAttachment(@PathVariable Long lessonId) throws IOException {
        return buildAttachmentResponse(lessonId);
    }

    @GetMapping("/lessons/{lessonId}/image")
    public ResponseEntity<ByteArrayResource> viewLessonImage(@PathVariable Long lessonId) throws IOException {
        return buildLessonImageResponse(lessonId);
    }

    @GetMapping("/portal/courses/{courseId}/lessons")
    public String studentLessons(@PathVariable Long courseId,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes ra) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        User student = getCurrentUser(auth);
        if (courseOpt.isEmpty() || student == null) {
            ra.addFlashAttribute("error", "Course not found.");
            return "redirect:/portal/courses";
        }

        boolean approved = enrollmentRepository.findByStudentAndCourse(student, courseOpt.get()).stream()
                .anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED);
        if (!approved) {
            ra.addFlashAttribute("error", "Your enrollment must be approved before you can view this content.");
            return "redirect:/portal/courses";
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        model.addAttribute("course", courseOpt.get());
        model.addAttribute("lessonViews", buildLessonViews(lessons));
        return "student/course-lessons";
    }

    @GetMapping("/portal/courses/{courseId}/lessons/{lessonId}")
    public String viewLesson(@PathVariable Long courseId,
                             @PathVariable Long lessonId,
                             Authentication auth,
                             Model model,
                             RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        User student = getCurrentUser(auth);
        if (lessonOpt.isEmpty() || student == null) {
            ra.addFlashAttribute("error", "Lesson not found.");
            return "redirect:/portal/courses";
        }

        Lesson lesson = lessonOpt.get();
        if (lesson.getCourse() == null || !Objects.equals(lesson.getCourse().getId(), courseId)) {
            ra.addFlashAttribute("error", "Lesson not found.");
            return "redirect:/portal/courses";
        }

        boolean approved = enrollmentRepository.findByStudentAndCourse(student, lesson.getCourse()).stream()
                .anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED);
        if (!approved) {
            ra.addFlashAttribute("error", "You are not enrolled in this course.");
            return "redirect:/portal/courses";
        }

        List<Lesson> allLessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        int currentIndex = -1;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).getId().equals(lessonId)) {
                currentIndex = i;
                break;
            }
        }

        model.addAttribute("lesson", lesson);
        model.addAttribute("prevLesson", currentIndex > 0 ? allLessons.get(currentIndex - 1) : null);
        model.addAttribute("nextLesson", currentIndex >= 0 && currentIndex < allLessons.size() - 1 ? allLessons.get(currentIndex + 1) : null);
        model.addAttribute("quizQuestions", lessonQuizQuestionRepository.findByLessonIdOrderBySortOrderAsc(lessonId));
        return "student/lesson-view";
    }

    @GetMapping("/portal/courses/{courseId}/lessons/{lessonId}/file")
    public ResponseEntity<ByteArrayResource> studentDownloadLessonAttachment(@PathVariable Long courseId,
                                                                             @PathVariable Long lessonId,
                                                                             Authentication auth) throws IOException {
        User student = getCurrentUser(auth);
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (student == null || courseOpt.isEmpty() || lessonOpt.isEmpty()) {
            throw new IOException("Unauthorized");
        }
        Lesson lesson = lessonOpt.get();
        if (lesson.getCourse() == null || !Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new IOException("Unauthorized");
        }

        boolean approved = enrollmentRepository.findByStudentAndCourse(student, courseOpt.get()).stream()
                .anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED);
        if (!approved) {
            throw new IOException("Unauthorized");
        }

        return buildAttachmentResponse(lesson);
    }

    @PostMapping("/portal/courses/{courseId}/lessons/{lessonId}/review")
    public String submitLessonReview(@PathVariable Long courseId,
                                     @PathVariable Long lessonId,
                                     @RequestParam Map<String, String> answers,
                                     Authentication auth,
                                     RedirectAttributes ra) {
        User student = getCurrentUser(auth);
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (student == null || lessonOpt.isEmpty() || courseOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Lesson review could not be submitted.");
            return "redirect:/portal/courses";
        }

        Lesson lesson = lessonOpt.get();
        if (lesson.getCourse() == null || !Objects.equals(lesson.getCourse().getId(), courseId)) {
            ra.addFlashAttribute("error", "Lesson review could not be submitted.");
            return "redirect:/portal/courses";
        }

        boolean approved = enrollmentRepository.findByStudentAndCourse(student, courseOpt.get()).stream()
                .anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED);
        if (!approved) {
            ra.addFlashAttribute("error", "You are not enrolled in this course.");
            return "redirect:/portal/courses";
        }

        List<LessonQuizQuestion> questions = lessonQuizQuestionRepository.findByLessonIdOrderBySortOrderAsc(lessonId);
        if (questions.isEmpty()) {
            ra.addFlashAttribute("error", "This lesson does not have a review quiz yet.");
            return "redirect:/portal/courses/" + courseId + "/lessons";
        }

        int correctAnswers = 0;
        Map<BloomLevel, Integer> wrongCountsByBloom = initializeBloomCounts();
        for (LessonQuizQuestion question : questions) {
            String answer = normalizeAnswer(answers.get("q_" + question.getId()));
            if (question.isCorrect(answer)) {
                correctAnswers++;
            } else {
                BloomLevel bloomLevel = question.getBloomLevel() != null ? question.getBloomLevel() : BloomLevel.REMEMBER;
                wrongCountsByBloom.merge(bloomLevel, 1, Integer::sum);
            }
        }

        int wrongAnswers = questions.size() - correctAnswers;
        if (wrongAnswers > 0) {
            int errorTypeCount = registerLessonReviewErrorsByBloom(student, lesson, wrongCountsByBloom);
            ra.addFlashAttribute("error",
                    "Review submitted: " + correctAnswers + "/" + questions.size()
                            + ". Da ghi nhan " + errorTypeCount + " loai loi tu lesson review quiz cua ban.");
            return "redirect:/portal/tests";
        }

        ra.addFlashAttribute("success",
                "Review submitted successfully: " + correctAnswers + "/" + questions.size() + ". You can continue to the next lesson.");
        return "redirect:/portal/courses/" + courseId + "/lessons";
    }

    @GetMapping("/portal/courses/{courseId}/lessons/{lessonId}/image")
    public ResponseEntity<ByteArrayResource> studentViewLessonImage(@PathVariable Long courseId,
                                                                    @PathVariable Long lessonId,
                                                                    Authentication auth) throws IOException {
        User student = getCurrentUser(auth);
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (student == null || courseOpt.isEmpty() || lessonOpt.isEmpty()) {
            throw new IOException("Unauthorized");
        }
        Lesson lesson = lessonOpt.get();
        if (lesson.getCourse() == null || !Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new IOException("Unauthorized");
        }
        boolean approved = enrollmentRepository.findByStudentAndCourse(student, courseOpt.get()).stream()
                .anyMatch(e -> e.getStatus() == EnrollmentStatus.APPROVED);
        if (!approved) {
            throw new IOException("Unauthorized");
        }
        return buildLessonImageResponse(lesson);
    }

    private int normalizeSortOrder(Long courseId, Integer requestedSortOrder, boolean movingFromAnotherCourse) {
        int lessonCount = (int) lessonRepository.countByCourseId(courseId);
        int maxSortOrder = movingFromAnotherCourse ? lessonCount + 1 : Math.max(lessonCount, 1);
        int sortOrder = requestedSortOrder != null ? requestedSortOrder : maxSortOrder;
        if (sortOrder < 1) {
            return 1;
        }
        return Math.min(sortOrder, maxSortOrder);
    }

    private void shiftLessonsForPlacement(Long targetCourseId,
                                          int targetSortOrder,
                                          Long previousCourseId,
                                          Lesson lessonBeingUpdated) {
        if (lessonBeingUpdated == null || lessonBeingUpdated.getId() == null) {
            shiftLessonsDown(targetCourseId, targetSortOrder, null);
            return;
        }

        boolean sameCourse = Objects.equals(previousCourseId, targetCourseId);
        int previousSortOrder = lessonBeingUpdated.getSortOrder();

        if (!sameCourse) {
            reorderLessonsAfterDeletion(previousCourseId, previousSortOrder);
            shiftLessonsDown(targetCourseId, targetSortOrder, null);
            return;
        }

        if (previousSortOrder == targetSortOrder) {
            return;
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(targetCourseId);
        for (Lesson lesson : lessons) {
            if (lesson.getId().equals(lessonBeingUpdated.getId())) {
                continue;
            }
            int currentOrder = lesson.getSortOrder();
            if (targetSortOrder < previousSortOrder
                    && currentOrder >= targetSortOrder
                    && currentOrder < previousSortOrder) {
                lesson.setSortOrder(currentOrder + 1);
                lessonRepository.save(lesson);
            } else if (targetSortOrder > previousSortOrder
                    && currentOrder <= targetSortOrder
                    && currentOrder > previousSortOrder) {
                lesson.setSortOrder(currentOrder - 1);
                lessonRepository.save(lesson);
            }
        }
    }

    private void shiftLessonsDown(Long courseId, int fromSortOrder, Long excludedLessonId) {
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId);
        for (Lesson lesson : lessons) {
            if (excludedLessonId != null && lesson.getId().equals(excludedLessonId)) {
                continue;
            }
            if (lesson.getSortOrder() >= fromSortOrder) {
                lesson.setSortOrder(lesson.getSortOrder() + 1);
                lessonRepository.save(lesson);
            }
        }
    }

    private void reorderLessonsAfterDeletion(Long courseId, int deletedOrder) {
        if (courseId == null) {
            return;
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdAndSortOrderGreaterThanOrderBySortOrderAsc(courseId, deletedOrder);
        for (Lesson lesson : lessons) {
            lesson.setSortOrder(lesson.getSortOrder() - 1);
            lessonRepository.save(lesson);
        }
    }

    private void replaceAttachmentIfNeeded(Lesson lesson, MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return;
        }
        deleteStoredAttachment(lesson);
        saveAttachment(lesson, attachment);
    }

    private void replaceLessonImageIfNeeded(Lesson lesson, MultipartFile lessonImage) {
        if (lessonImage == null || lessonImage.isEmpty()) {
            return;
        }
        deleteStoredLessonImage(lesson);
        saveLessonImage(lesson, lessonImage);
    }

    private void deleteStoredAttachment(Lesson lesson) {
        if (lesson.hasAttachment()) {
            try {
                Files.deleteIfExists(uploadRoot.resolve(lesson.getAttachmentStoredName()));
            } catch (IOException ignored) {
            }
            lesson.setAttachmentOriginalName(null);
            lesson.setAttachmentStoredName(null);
            lesson.setAttachmentContentType(null);
        }
    }

    private void deleteStoredLessonImage(Lesson lesson) {
        if (lesson.hasImage()) {
            try {
                Files.deleteIfExists(imageUploadRoot.resolve(lesson.getImageStoredName()));
            } catch (IOException ignored) {
            }
            lesson.setImageOriginalName(null);
            lesson.setImageStoredName(null);
            lesson.setImageContentType(null);
        }
    }

    private void saveAttachment(Lesson lesson, MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return;
        }
        validateAttachment(attachment);
        try {
            Files.createDirectories(uploadRoot);
            String originalName = sanitizeFilename(attachment.getOriginalFilename());
            String storedName = LocalDateTime.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + originalName;
            Files.copy(attachment.getInputStream(), uploadRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            lesson.setAttachmentOriginalName(originalName);
            lesson.setAttachmentStoredName(storedName);
            lesson.setAttachmentContentType(attachment.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store lesson attachment", ex);
        }
    }

    private void saveLessonImage(Lesson lesson, MultipartFile lessonImage) {
        if (lessonImage == null || lessonImage.isEmpty()) {
            return;
        }
        validateLessonImage(lessonImage);
        try {
            Files.createDirectories(imageUploadRoot);
            String originalName = sanitizeFilename(lessonImage.getOriginalFilename());
            String storedName = LocalDateTime.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + originalName;
            Files.copy(lessonImage.getInputStream(), imageUploadRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            lesson.setImageOriginalName(originalName);
            lesson.setImageStoredName(storedName);
            lesson.setImageContentType(lessonImage.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store lesson image", ex);
        }
    }

    private void saveLessonQuiz(Lesson lesson,
                                List<String> quizQuestionTexts,
                                List<String> optionAs,
                                List<String> optionBs,
                                List<String> optionCs,
                                List<String> optionDs,
                                List<String> questionTypes,
                                List<String> correctAnswers,
                                List<String> bloomLevels,
                                List<String> explanations) {
        if (quizQuestionTexts == null) {
            return;
        }
        for (int i = 0; i < quizQuestionTexts.size(); i++) {
            String questionText = quizQuestionTexts.get(i);
            if (questionText == null || questionText.isBlank()) {
                continue;
            }
            LessonQuizQuestion question = new LessonQuizQuestion();
            question.setLesson(lesson);
            question.setSortOrder(i + 1);
            question.setQuestionText(questionText);
            QuestionType questionType = parseQuestionType(getValue(questionTypes, i));
            question.setQuestionType(questionType);
            question.setOptionA(questionType == QuestionType.MULTIPLE_CHOICE ? getValue(optionAs, i) : "");
            question.setOptionB(questionType == QuestionType.MULTIPLE_CHOICE ? getValue(optionBs, i) : "");
            question.setOptionC(questionType == QuestionType.MULTIPLE_CHOICE ? getValue(optionCs, i) : "");
            question.setOptionD(questionType == QuestionType.MULTIPLE_CHOICE ? getValue(optionDs, i) : "");
            question.setCorrectAnswer(getValue(correctAnswers, i));
            question.setBloomLevel(parseBloomLevel(getValue(bloomLevels, i)));
            question.setExplanation(getValue(explanations, i));
            lessonQuizQuestionRepository.save(question);
        }
    }

    private String getValue(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return "";
        }
        return values.get(index);
    }

    private List<LessonView> buildLessonViews(List<Lesson> lessons) {
        return lessons.stream()
                .map(lesson -> new LessonView(lesson, lessonQuizQuestionRepository.findByLessonIdOrderBySortOrderAsc(lesson.getId())))
                .toList();
    }

    private ResponseEntity<ByteArrayResource> buildAttachmentResponse(Long lessonId) throws IOException {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty() || !lessonOpt.get().hasAttachment()) {
            throw new IOException("Attachment not found");
        }
        return buildAttachmentResponse(lessonOpt.get());
    }

    private ResponseEntity<ByteArrayResource> buildAttachmentResponse(Lesson lesson) throws IOException {
        if (!lesson.hasAttachment()) {
            throw new IOException("Attachment not found");
        }
        byte[] bytes = Files.readAllBytes(uploadRoot.resolve(lesson.getAttachmentStoredName()));
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(lesson.getAttachmentContentType() != null
                        ? MediaType.parseMediaType(lesson.getAttachmentContentType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + lesson.getAttachmentOriginalName() + "\"")
                .contentLength(bytes.length)
                .body(resource);
    }

    private ResponseEntity<ByteArrayResource> buildLessonImageResponse(Long lessonId) throws IOException {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty() || !lessonOpt.get().hasImage()) {
            throw new IOException("Image not found");
        }
        return buildLessonImageResponse(lessonOpt.get());
    }

    private ResponseEntity<ByteArrayResource> buildLessonImageResponse(Lesson lesson) throws IOException {
        if (!lesson.hasImage()) {
            throw new IOException("Image not found");
        }
        byte[] bytes = Files.readAllBytes(imageUploadRoot.resolve(lesson.getImageStoredName()));
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(lesson.getImageContentType() != null
                        ? MediaType.parseMediaType(lesson.getImageContentType())
                        : MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentLength(bytes.length)
                .body(resource);
    }

    private boolean isCourseEditableForLessons(Course course) {
        if (course == null || course.getStatus() == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        if (course.getEndDate() != null && course.getEndDate().isBefore(today)) {
            return false;
        }
        return course.getStatus() == CourseStatus.DRAFT
                || course.getStatus() == CourseStatus.OPEN
                || course.getStatus() == CourseStatus.IN_PROGRESS;
    }

    private void validateAttachment(MultipartFile attachment) {
        if (attachment.getSize() > MAX_ATTACHMENT_SIZE_BYTES) {
            throw new IllegalArgumentException("Attachment size must be 10 MB or smaller.");
        }
        String contentType = attachment.getContentType();
        if (contentType == null || !ALLOWED_ATTACHMENT_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only PDF, DOC, DOCX, and TXT files are allowed.");
        }
    }

    private void validateLessonImage(MultipartFile lessonImage) {
        if (lessonImage.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Lesson image size must be 5 MB or smaller.");
        }
        String contentType = lessonImage.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPG, PNG, WEBP, and GIF images are allowed.");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String raw = originalFilename != null ? originalFilename : "lesson-file";
        String sanitized = Paths.get(raw).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "lesson-file" : sanitized;
    }

    private String normalizeLessonCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private ErrorType resolveErrorType(Long errorTypeId) {
        if (errorTypeId == null) {
            return null;
        }
        return errorTypeRepository.findById(errorTypeId).orElse(null);
    }

    private String normalizeErrorTypeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim();
    }

    private Test resolveRemedialTest(Long remedialTestId) {
        if (remedialTestId == null) {
            return null;
        }
        return testRepository.findById(remedialTestId)
                .filter(Test::isRemedialTest)
                .orElse(null);
    }

    private void registerLessonReviewError(User student, Lesson lesson) {
        if (student == null || lesson == null || lesson.getErrorType() == null
                || student.getId() == null || lesson.getErrorType().getId() == null) {
            return;
        }
        if (studentErrorRepository.existsByStudentIdAndErrorTypeId(student.getId(), lesson.getErrorType().getId())) {
            return;
        }
        StudentError studentError = new StudentError();
        studentError.setStudent(student);
        studentError.setErrorType(lesson.getErrorType());
        studentErrorRepository.save(studentError);
    }

    private BloomLevel parseBloomLevel(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return BloomLevel.REMEMBER;
        }
        try {
            return BloomLevel.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return BloomLevel.REMEMBER;
        }
    }

    private QuestionType parseQuestionType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return QuestionType.MULTIPLE_CHOICE;
        }
        try {
            return QuestionType.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return QuestionType.MULTIPLE_CHOICE;
        }
    }

    private Map<BloomLevel, Integer> initializeBloomCounts() {
        Map<BloomLevel, Integer> counts = new java.util.EnumMap<>(BloomLevel.class);
        for (BloomLevel bloomLevel : BloomLevel.values()) {
            counts.put(bloomLevel, 0);
        }
        return counts;
    }

    private int registerLessonReviewErrorsByBloom(User student, Lesson lesson, Map<BloomLevel, Integer> wrongCountsByBloom) {
        if (student == null || lesson == null || student.getId() == null) {
            return 0;
        }
        int createdCount = 0;
        for (Map.Entry<BloomLevel, Integer> entry : wrongCountsByBloom.entrySet()) {
            if (entry.getValue() == null || entry.getValue() <= 0) {
                continue;
            }
            ErrorType errorType = resolveOrCreateBloomErrorType(lesson, entry.getKey());
            if (errorType == null || errorType.getId() == null) {
                continue;
            }
            if (studentErrorRepository.existsByStudentIdAndErrorTypeId(student.getId(), errorType.getId())) {
                continue;
            }
            StudentError studentError = new StudentError();
            studentError.setStudent(student);
            studentError.setErrorType(errorType);
            studentErrorRepository.save(studentError);
            createdCount++;
        }
        return createdCount;
    }

    private ErrorType resolveOrCreateBloomErrorType(Lesson lesson, BloomLevel bloomLevel) {
        if (lesson != null) {
            ErrorType lessonErrorType = lesson.getErrorTypeForBloomLevel(bloomLevel);
            if (lessonErrorType != null) {
                return lessonErrorType;
            }
        }
        String name = "Lesson Review - " + bloomLevel.getLabel();
        return errorTypeRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    ErrorType errorType = new ErrorType();
                    errorType.setName(name);
                    errorType.setDescription("Automatically created when a student answers lesson review quiz questions incorrectly at Bloom level " + bloomLevel.getLabel() + ".");
                    return errorTypeRepository.save(errorType);
                });
    }

    private void applyBloomErrorTypes(Lesson lesson,
                                      List<String> bloomErrorTypeNames,
                                      List<String> bloomErrorTypeDescriptions) {
        lesson.setRememberErrorType(resolveOrCreateLessonBloomErrorType(BloomLevel.REMEMBER, bloomErrorTypeNames, bloomErrorTypeDescriptions));
        lesson.setUnderstandErrorType(resolveOrCreateLessonBloomErrorType(BloomLevel.UNDERSTAND, bloomErrorTypeNames, bloomErrorTypeDescriptions));
        lesson.setApplyErrorType(resolveOrCreateLessonBloomErrorType(BloomLevel.APPLY, bloomErrorTypeNames, bloomErrorTypeDescriptions));
        lesson.setAnalyzeErrorType(resolveOrCreateLessonBloomErrorType(BloomLevel.ANALYZE, bloomErrorTypeNames, bloomErrorTypeDescriptions));
        lesson.setEvaluateErrorType(resolveOrCreateLessonBloomErrorType(BloomLevel.EVALUATE, bloomErrorTypeNames, bloomErrorTypeDescriptions));
        lesson.setCreateErrorType(resolveOrCreateLessonBloomErrorType(BloomLevel.CREATE, bloomErrorTypeNames, bloomErrorTypeDescriptions));
    }

    private ErrorType resolveOrCreateLessonBloomErrorType(BloomLevel bloomLevel,
                                                          List<String> bloomErrorTypeNames,
                                                          List<String> bloomErrorTypeDescriptions) {
        int index = bloomLevel.ordinal();
        String normalizedName = normalizeErrorTypeName(getValue(bloomErrorTypeNames, index));
        if (normalizedName == null) {
            return null;
        }
        String description = getValue(bloomErrorTypeDescriptions, index);
        return errorTypeRepository.findByNameIgnoreCase(normalizedName)
                .orElseGet(() -> {
                    ErrorType errorType = new ErrorType();
                    errorType.setName(normalizedName);
                    errorType.setDescription(description != null && !description.isBlank()
                            ? description.trim()
                            : "Created directly from lesson Bloom review mapping.");
                    return errorTypeRepository.save(errorType);
                });
    }

    private String sanitizeLessonContent(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String sanitized = content
                .replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<style.*?>.*?</style>", "")
                .replaceAll("(?i)on\\w+\\s*=\\s*(['\"]).*?\\1", "")
                .replaceAll("(?i)javascript:", "");
        return sanitized.trim();
    }

    private User getCurrentUser(Authentication auth) {
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .or(() -> userRepository.findByEmail(auth.getName()))
                .orElse(null);
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String normalizeAnswer(String answer) {
        if (answer == null) {
            return null;
        }
        String normalized = answer.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record LessonView(Lesson lesson, List<LessonQuizQuestion> quizQuestions) {
    }
}
