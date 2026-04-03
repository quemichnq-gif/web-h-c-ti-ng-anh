package com.example.demo.controller;

import com.example.demo.model.Course;
import com.example.demo.model.Lesson;
import com.example.demo.model.LessonQuizQuestion;
import com.example.demo.model.User;
import com.example.demo.repository.CourseRepository;
import com.example.demo.repository.EnrollmentRepository;
import com.example.demo.repository.LessonQuizQuestionRepository;
import com.example.demo.repository.LessonRepository;
import com.example.demo.repository.UserRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Controller
public class LessonController {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonQuizQuestionRepository lessonQuizQuestionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final Path uploadRoot = Paths.get("uploads", "lesson-files");

    public LessonController(CourseRepository courseRepository,
                            LessonRepository lessonRepository,
                            LessonQuizQuestionRepository lessonQuizQuestionRepository,
                            EnrollmentRepository enrollmentRepository,
                            UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.lessonQuizQuestionRepository = lessonQuizQuestionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/lessons")
    public String listLessons(@RequestParam(required = false) Long courseId,
                              @RequestParam(required = false) String search,
                              Model model) {
        List<Course> courses = courseRepository.findAll().stream()
                .sorted(Comparator.comparing(course -> safe(course.getName())))
                .toList();

        List<Lesson> lessons = lessonRepository.findAll().stream()
                .filter(lesson -> courseId == null || (lesson.getCourse() != null && Objects.equals(lesson.getCourse().getId(), courseId)))
                .filter(lesson -> {
                    if (search == null || search.isBlank()) {
                        return true;
                    }
                    String query = search.toLowerCase();
                    return safe(lesson.getTitle()).contains(query)
                            || safe(lesson.getSummary()).contains(query)
                            || (lesson.getCourse() != null && (safe(lesson.getCourse().getName()).contains(query) || safe(lesson.getCourse().getCode()).contains(query)));
                })
                .sorted(Comparator
                        .comparing((Lesson lesson) -> lesson.getCourse() != null ? safe(lesson.getCourse().getName()) : "")
                        .thenComparing(lesson -> lesson.getSortOrder() != null ? lesson.getSortOrder() : 0)
                        .thenComparing(Lesson::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

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
        model.addAttribute("quizQuestions", List.<LessonQuizQuestion>of());
        return "lessons/form";
    }

    @PostMapping("/lessons/create")
    public String createLesson(@RequestParam Long courseId,
                               @RequestParam String title,
                               @RequestParam(required = false) String summary,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "1") Integer sortOrder,
                               @RequestParam(required = false) MultipartFile attachment,
                               @RequestParam(required = false) List<String> quizQuestionTexts,
                               @RequestParam(required = false) List<String> optionAs,
                               @RequestParam(required = false) List<String> optionBs,
                               @RequestParam(required = false) List<String> optionCs,
                               @RequestParam(required = false) List<String> optionDs,
                               @RequestParam(required = false) List<String> correctAnswers,
                               @RequestParam(required = false) List<String> explanations,
                               RedirectAttributes ra) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Course khong ton tai.");
            return "redirect:/lessons/create";
        }

        Lesson lesson = new Lesson();
        lesson.setCourse(courseOpt.get());
        lesson.setTitle(title);
        lesson.setSummary(summary);
        lesson.setContent(content);
        lesson.setSortOrder(sortOrder != null ? sortOrder : 1);
        saveAttachment(lesson, attachment);
        lessonRepository.save(lesson);
        saveLessonQuiz(lesson, quizQuestionTexts, optionAs, optionBs, optionCs, optionDs, correctAnswers, explanations);

        ra.addFlashAttribute("success", "Da tao lesson '" + title + "' thanh cong.");
        return "redirect:/lessons?courseId=" + courseId;
    }

    @GetMapping("/lessons/{lessonId}/edit")
    public String editLessonForm(@PathVariable Long lessonId, Model model, RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Lesson khong ton tai.");
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
        model.addAttribute("quizQuestions", lessonQuizQuestionRepository.findByLessonIdOrderBySortOrderAscIdAsc(lessonId));
        return "lessons/form";
    }

    @PostMapping("/lessons/{lessonId}/edit")
    public String updateLesson(@PathVariable Long lessonId,
                               @RequestParam Long courseId,
                               @RequestParam String title,
                               @RequestParam(required = false) String summary,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "1") Integer sortOrder,
                               @RequestParam(required = false) MultipartFile attachment,
                               @RequestParam(required = false) List<String> quizQuestionTexts,
                               @RequestParam(required = false) List<String> optionAs,
                               @RequestParam(required = false) List<String> optionBs,
                               @RequestParam(required = false) List<String> optionCs,
                               @RequestParam(required = false) List<String> optionDs,
                               @RequestParam(required = false) List<String> correctAnswers,
                               @RequestParam(required = false) List<String> explanations,
                               RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (lessonOpt.isEmpty() || courseOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Khong tim thay lesson hoac course.");
            return "redirect:/lessons";
        }

        Lesson lesson = lessonOpt.get();
        lesson.setCourse(courseOpt.get());
        lesson.setTitle(title);
        lesson.setSummary(summary);
        lesson.setContent(content);
        lesson.setSortOrder(sortOrder != null ? sortOrder : 1);
        replaceAttachmentIfNeeded(lesson, attachment);
        lessonRepository.save(lesson);

        lessonQuizQuestionRepository.deleteByLessonId(lessonId);
        saveLessonQuiz(lesson, quizQuestionTexts, optionAs, optionBs, optionCs, optionDs, correctAnswers, explanations);

        ra.addFlashAttribute("success", "Da cap nhat lesson '" + title + "'.");
        return "redirect:/lessons?courseId=" + courseId;
    }

    @PostMapping("/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long lessonId, RedirectAttributes ra) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        Long courseId = null;
        if (lessonOpt.isPresent()) {
            Lesson lesson = lessonOpt.get();
            courseId = lesson.getCourse() != null ? lesson.getCourse().getId() : null;
            deleteStoredAttachment(lesson);
            lessonQuizQuestionRepository.deleteByLessonId(lessonId);
            lessonRepository.delete(lesson);
        }
        ra.addFlashAttribute("success", "Da xoa lesson.");
        return courseId != null ? "redirect:/lessons?courseId=" + courseId : "redirect:/lessons";
    }

    @GetMapping("/lessons/{lessonId}/file")
    public ResponseEntity<ByteArrayResource> downloadLessonAttachment(@PathVariable Long lessonId) throws IOException {
        return buildAttachmentResponse(lessonId);
    }

    @GetMapping("/portal/courses/{courseId}/lessons")
    public String studentLessons(@PathVariable Long courseId,
                                 Authentication auth,
                                 Model model,
                                 RedirectAttributes ra) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        User student = getCurrentUser(auth);
        if (courseOpt.isEmpty() || student == null) {
            ra.addFlashAttribute("error", "Khong tim thay khoa hoc.");
            return "redirect:/portal/courses";
        }

        boolean approved = enrollmentRepository.findByStudentAndCourse(student, courseOpt.get()).stream()
                .anyMatch(e -> "APPROVED".equals(e.getStatus()));
        if (!approved) {
            ra.addFlashAttribute("error", "Ban can duoc duyet dang ky khoa hoc de xem noi dung.");
            return "redirect:/portal/courses";
        }

        List<Lesson> lessons = lessonRepository.findByCourseIdOrderBySortOrderAscCreatedAtAsc(courseId);
        model.addAttribute("course", courseOpt.get());
        model.addAttribute("lessonViews", buildLessonViews(lessons));
        return "student/course-lessons";
    }

    @GetMapping("/portal/courses/{courseId}/lessons/{lessonId}/file")
    public ResponseEntity<ByteArrayResource> studentDownloadLessonAttachment(@PathVariable Long courseId,
                                                                             @PathVariable Long lessonId,
                                                                             Authentication auth) throws IOException {
        User student = getCurrentUser(auth);
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (student == null || courseOpt.isEmpty()) {
            throw new IOException("Unauthorized");
        }
        boolean approved = enrollmentRepository.findByStudentAndCourse(student, courseOpt.get()).stream()
                .anyMatch(e -> "APPROVED".equals(e.getStatus()));
        if (!approved) {
            throw new IOException("Unauthorized");
        }
        return buildAttachmentResponse(lessonId);
    }

    private void replaceAttachmentIfNeeded(Lesson lesson, MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return;
        }
        deleteStoredAttachment(lesson);
        saveAttachment(lesson, attachment);
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

    private void saveAttachment(Lesson lesson, MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(uploadRoot);
            String originalName = attachment.getOriginalFilename() != null ? attachment.getOriginalFilename() : "lesson-file";
            String storedName = LocalDateTime.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + originalName;
            Files.copy(attachment.getInputStream(), uploadRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            lesson.setAttachmentOriginalName(originalName);
            lesson.setAttachmentStoredName(storedName);
            lesson.setAttachmentContentType(attachment.getContentType());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store lesson attachment", ex);
        }
    }

    private void saveLessonQuiz(Lesson lesson,
                                List<String> quizQuestionTexts,
                                List<String> optionAs,
                                List<String> optionBs,
                                List<String> optionCs,
                                List<String> optionDs,
                                List<String> correctAnswers,
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
            question.setOptionA(getValue(optionAs, i));
            question.setOptionB(getValue(optionBs, i));
            question.setOptionC(getValue(optionCs, i));
            question.setOptionD(getValue(optionDs, i));
            question.setCorrectAnswer(getValue(correctAnswers, i));
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
                .map(lesson -> new LessonView(lesson, lessonQuizQuestionRepository.findByLessonIdOrderBySortOrderAscIdAsc(lesson.getId())))
                .toList();
    }

    private ResponseEntity<ByteArrayResource> buildAttachmentResponse(Long lessonId) throws IOException {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty() || !lessonOpt.get().hasAttachment()) {
            throw new IOException("Attachment not found");
        }
        Lesson lesson = lessonOpt.get();
        byte[] bytes = Files.readAllBytes(uploadRoot.resolve(lesson.getAttachmentStoredName()));
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(lesson.getAttachmentContentType() != null ? MediaType.parseMediaType(lesson.getAttachmentContentType()) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + lesson.getAttachmentOriginalName() + "\"")
                .contentLength(bytes.length)
                .body(resource);
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

    public record LessonView(Lesson lesson, List<LessonQuizQuestion> quizQuestions) {
    }
}
