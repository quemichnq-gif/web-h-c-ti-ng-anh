package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.AuditLogService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/assessments")
public class AssessmentController {
    private static final long MAX_QUESTION_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_QUESTION_AUDIO_SIZE_BYTES = 10L * 1024 * 1024;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final List<String> ALLOWED_AUDIO_TYPES = List.of("audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/ogg", "audio/mp4", "audio/x-m4a");

    private final TestRepository testRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final StudentResultRepository resultRepository;
    private final QuestionRepository questionRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final ErrorTestMappingRepository errorTestMappingRepository;
    private final AuditLogService auditLogService;
    private final Path questionImageRoot = Paths.get("uploads", "question-media", "images");
    private final Path questionAudioRoot = Paths.get("uploads", "question-media", "audio");

    public AssessmentController(TestRepository testRepository, CourseRepository courseRepository,
                                LessonRepository lessonRepository,
                                StudentResultRepository resultRepository, QuestionRepository questionRepository,
                                ErrorTypeRepository errorTypeRepository, ErrorTestMappingRepository errorTestMappingRepository,
                                AuditLogService auditLogService) {
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.resultRepository = resultRepository;
        this.questionRepository = questionRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.errorTestMappingRepository = errorTestMappingRepository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public String list(Model model, @RequestParam(required = false) Long courseId,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String type) {
        List<Course> courses = courseRepository.findAll();
        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("selectedType", type);

        AssessmentType selectedType = parseType(type);
        List<Test> tests;
        if (courseId != null && selectedType != null) {
            tests = testRepository.findByCourseIdAndAssessmentType(courseId, selectedType);
        } else if (courseId != null) {
            tests = testRepository.findByCourseId(courseId);
        } else if (selectedType != null) {
            tests = testRepository.findByAssessmentType(selectedType);
        } else {
            tests = testRepository.findAll();
        }

        if (search != null && !search.isBlank()) {
            final String q = search.toLowerCase();
            tests = tests.stream().filter(t ->
                    safe(t.getTitle()).contains(q)
                            || safe(t.getDescription()).contains(q)
                            || safe(t.getCourse() != null ? t.getCourse().getName() : null).contains(q))
                    .toList();
        }

        tests = tests.stream()
                .sorted(Comparator
                        .comparing(Test::getAssessmentType)
                        .thenComparing(Test::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        Map<Long, Long> qCounts = new HashMap<>();
        for (Test t : tests) {
            if (t.getId() != null) {
                qCounts.put(t.getId(), questionRepository.countByTest(t));
            }
        }

        model.addAttribute("tests", tests);
        model.addAttribute("questionCounts", qCounts);
        model.addAttribute("search", search);
        return "assessments/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("assessmentTypes", AssessmentType.values());
        return "assessments/create";
    }

    @GetMapping("/context")
    @ResponseBody
    public ResponseEntity<AssessmentContextResponse> context(@RequestParam Long courseId) {
        return ResponseEntity.ok(buildAssessmentContext(courseId));
    }

    @PostMapping("/create")
    public String create(@RequestParam String title,
                         @RequestParam String code,
                         @RequestParam(required = false) String description,
                         @RequestParam Integer duration,
                         @RequestParam Long courseId,
                         @RequestParam String assessmentType,
                         @RequestParam(required = false) Long errorTypeId,
                         @RequestParam(required = false) Long targetLessonId,
                         @RequestParam(required = false) List<String> questionTypes,
                         @RequestParam(required = false) List<String> questionContents,
                         @RequestParam(required = false) List<String> correctAnswers,
                         @RequestParam(required = false) List<String> optionAs,
                         @RequestParam(required = false) List<String> optionBs,
                         @RequestParam(required = false) List<String> optionCs,
                         @RequestParam(required = false) List<String> optionDs,
                         @RequestParam(required = false) List<MultipartFile> questionImages,
                         @RequestParam(required = false) List<MultipartFile> questionAudios,
                         RedirectAttributes ra) {

        Optional<Course> course = courseRepository.findById(courseId);
        if (course.isEmpty()) {
            ra.addFlashAttribute("error", "Course not found.");
            return "redirect:/assessments/create";
        }
        String normalizedCode = normalizeAssessmentCode(code);
        if (normalizedCode == null) {
            ra.addFlashAttribute("error", "Assessment code is required.");
            return "redirect:/assessments/create";
        }
        if (testRepository.existsByCodeIgnoreCase(normalizedCode)) {
            ra.addFlashAttribute("error", "Assessment code already exists.");
            return "redirect:/assessments/create";
        }

        Test test = new Test();
        test.setCode(normalizedCode);
        test.setTitle(title);
        test.setDescription(description);
        test.setDuration(duration);
        test.setCourse(course.get());
        test.setAssessmentType(AssessmentType.valueOf(assessmentType));
        try {
            applyTargetLesson(test, targetLessonId);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/assessments/create";
        }
        testRepository.save(test);
        try {
            syncRemedialErrorMapping(test, errorTypeId);
        } catch (IllegalArgumentException ex) {
            testRepository.delete(test);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/assessments/create";
        }

        try {
            saveQuestions(test, questionTypes, questionContents, correctAnswers, optionAs, optionBs, optionCs, optionDs,
                    questionImages, questionAudios, List.of());
        } catch (IllegalArgumentException ex) {
            questionRepository.deleteAll(questionRepository.findByTestId(test.getId()));
            errorTestMappingRepository.findByTestId(test.getId()).forEach(errorTestMappingRepository::delete);
            testRepository.delete(test);
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/assessments/create";
        }
        auditLogService.log("ASSESSMENT_CREATED", "ASSESSMENT", test.getId(),
                "Created assessment '" + test.getTitle() + "' (" + test.getCode()
                        + ") for course '" + safe(test.getCourse() != null ? test.getCourse().getCode() : null)
                        + "' as " + test.getAssessmentType() + ".");

        ra.addFlashAttribute("success", "Assessment '" + title + "' created successfully.");
        return "redirect:/assessments";
    }

    private String getVal(List<String> list, int i) {
        return (list != null && i < list.size()) ? list.get(i) : "";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Test not found.");
            return "redirect:/assessments";
        }
        model.addAttribute("test", opt.get());
        model.addAttribute("courses", courseRepository.findAll());
        model.addAttribute("assessmentTypes", AssessmentType.values());
        model.addAttribute("questions", questionRepository.findByTestId(id));
        model.addAttribute("mappedErrorTypeId", errorTestMappingRepository.findByTestId(id).stream()
                .map(ErrorTestMapping::getErrorType)
                .filter(Objects::nonNull)
                .map(ErrorType::getId)
                .findFirst()
                .orElse(null));
        long resultCount = resultRepository.countByTest(opt.get());
        model.addAttribute("resultCount", resultCount);
        return "assessments/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam String code,
                         @RequestParam(required = false) String description,
                         @RequestParam Integer duration,
                         @RequestParam Long courseId,
                         @RequestParam String assessmentType,
                         @RequestParam(required = false) Long errorTypeId,
                         @RequestParam(required = false) Long targetLessonId,
                         @RequestParam(required = false) List<String> questionTypes,
                         @RequestParam(required = false) List<String> questionContents,
                         @RequestParam(required = false) List<String> correctAnswers,
                         @RequestParam(required = false) List<String> optionAs,
                         @RequestParam(required = false) List<String> optionBs,
                         @RequestParam(required = false) List<String> optionCs,
                         @RequestParam(required = false) List<String> optionDs,
                         @RequestParam(required = false) List<MultipartFile> questionImages,
                         @RequestParam(required = false) List<MultipartFile> questionAudios,
                         RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Test not found.");
            return "redirect:/assessments";
        }
        String normalizedCode = normalizeAssessmentCode(code);
        if (normalizedCode == null) {
            ra.addFlashAttribute("error", "Assessment code is required.");
            return "redirect:/assessments/" + id + "/edit";
        }
        if (testRepository.existsByCodeIgnoreCaseAndIdNot(normalizedCode, id)) {
            ra.addFlashAttribute("error", "Assessment code already exists.");
            return "redirect:/assessments/" + id + "/edit";
        }

        Test test = opt.get();
        String previousTitle = test.getTitle();
        String previousCode = test.getCode();
        String previousCourseCode = test.getCourse() != null ? test.getCourse().getCode() : null;
        AssessmentType previousType = test.getAssessmentType();
        courseRepository.findById(courseId).ifPresent(test::setCourse);
        test.setCode(normalizedCode);
        test.setTitle(title);
        test.setDescription(description);
        test.setDuration(duration);
        test.setAssessmentType(AssessmentType.valueOf(assessmentType));
        try {
            applyTargetLesson(test, targetLessonId);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/assessments/" + id + "/edit";
        }
        testRepository.save(test);
        try {
            syncRemedialErrorMapping(test, errorTypeId);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/assessments/" + id + "/edit";
        }

        List<Question> existingQuestions = questionRepository.findByTestId(id);
        try {
            saveQuestions(test, questionTypes, questionContents, correctAnswers, optionAs, optionBs, optionCs, optionDs,
                    questionImages, questionAudios, existingQuestions);
            deleteQuestionMedia(existingQuestions);
            questionRepository.deleteAll(existingQuestions);
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/assessments/" + id + "/edit";
        }
        auditLogService.log("ASSESSMENT_UPDATED", "ASSESSMENT", test.getId(),
                "Updated assessment from '" + safe(previousTitle) + "' (" + safe(previousCode) + ") in course '"
                        + safe(previousCourseCode) + "' as " + (previousType != null ? previousType : "UNKNOWN")
                        + " to '" + test.getTitle() + "' (" + test.getCode() + ") in course '"
                        + safe(test.getCourse() != null ? test.getCourse().getCode() : null) + "' as " + test.getAssessmentType() + ".");

        ra.addFlashAttribute("success", "Test updated successfully.");
        return "redirect:/assessments";
    }

    @Transactional
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Optional<Test> testOpt = testRepository.findById(id);
        if (testOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Test not found.");
            return "redirect:/assessments";
        }
        Test test = testOpt.get();
        String testTitle = test.getTitle();
        String testCode = test.getCode();
        String courseCode = test.getCourse() != null ? test.getCourse().getCode() : null;
        List<Question> questions = questionRepository.findByTestId(id);
        List<StudentResult> results = resultRepository.findByTestId(id);
        resultRepository.deleteAll(results);
        errorTestMappingRepository.findByTestId(id).forEach(errorTestMappingRepository::delete);
        lessonRepository.findByTestId(id).forEach(lesson -> {
            lesson.setTest(null);
            lessonRepository.save(lesson);
        });
        deleteQuestionMedia(questions);
        questionRepository.deleteAll(questions);
        testRepository.delete(test);
        auditLogService.log("ASSESSMENT_DELETED", "ASSESSMENT", id,
                "Deleted assessment '" + safe(testTitle) + "' (" + safe(testCode)
                        + ") from course '" + safe(courseCode) + "'.");
        ra.addFlashAttribute("success", "Test deleted successfully.");
        return "redirect:/assessments";
    }

    private void saveQuestions(Test test,
                               List<String> questionTypes,
                               List<String> questionContents,
                               List<String> correctAnswers,
                               List<String> optionAs,
                               List<String> optionBs,
                               List<String> optionCs,
                               List<String> optionDs,
                               List<MultipartFile> questionImages,
                               List<MultipartFile> questionAudios,
                               List<Question> existingQuestions) {
        if (questionContents == null) {
            return;
        }
        for (int i = 0; i < questionContents.size(); i++) {
            String content = questionContents.get(i);
            if (content == null || content.isBlank()) {
                continue;
            }
            Question q = new Question();
            q.setTest(test);
            q.setContent(content);

            String type = (questionTypes != null && i < questionTypes.size()) ? questionTypes.get(i) : "SHORT_ANSWER";
            q.setQuestionType(QuestionType.valueOf(type));
            String correctAnswer = correctAnswers != null && i < correctAnswers.size() ? correctAnswers.get(i) : "";
            q.setCorrectAnswer(correctAnswer);
            q.setCorrectOption(normalizeCorrectOption(correctAnswer));

            if (QuestionType.MULTIPLE_CHOICE.name().equals(type)) {
                q.setOptionA(getVal(optionAs, i));
                q.setOptionB(getVal(optionBs, i));
                q.setOptionC(getVal(optionCs, i));
                q.setOptionD(getVal(optionDs, i));
            }

            Question existingQuestion = i < existingQuestions.size() ? existingQuestions.get(i) : null;
            copyExistingMediaIfNeeded(q, existingQuestion, questionImages, questionAudios, i);
            saveQuestionImage(q, getFile(questionImages, i));
            saveQuestionAudio(q, getFile(questionAudios, i));
            questionRepository.save(q);
        }
    }

    private MultipartFile getFile(List<MultipartFile> files, int index) {
        return files != null && index < files.size() ? files.get(index) : null;
    }

    private String normalizeCorrectOption(String value) {
        if (value == null || value.isBlank()) {
            return "A";
        }
        return value.trim().substring(0, 1).toUpperCase();
    }

    private void copyExistingMediaIfNeeded(Question target,
                                           Question existing,
                                           List<MultipartFile> questionImages,
                                           List<MultipartFile> questionAudios,
                                           int index) {
        if (existing == null) {
            return;
        }
        MultipartFile image = getFile(questionImages, index);
        MultipartFile audio = getFile(questionAudios, index);
        if ((image == null || image.isEmpty()) && existing.hasImage()) {
            target.setImageOriginalName(existing.getImageOriginalName());
            target.setImageStoredName(existing.getImageStoredName());
            target.setImageContentType(existing.getImageContentType());
        }
        if ((audio == null || audio.isEmpty()) && existing.hasAudio()) {
            target.setAudioOriginalName(existing.getAudioOriginalName());
            target.setAudioStoredName(existing.getAudioStoredName());
            target.setAudioContentType(existing.getAudioContentType());
        }
    }

    private void saveQuestionImage(Question question, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return;
        }
        validateQuestionImage(image);
        try {
            Files.createDirectories(questionImageRoot);
            String originalName = sanitizeFilename(image.getOriginalFilename());
            String storedName = LocalDateTime.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + originalName;
            Files.copy(image.getInputStream(), questionImageRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            question.setImageOriginalName(originalName);
            question.setImageStoredName(storedName);
            question.setImageContentType(image.getContentType());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to store question image.");
        }
    }

    private void saveQuestionAudio(Question question, MultipartFile audio) {
        if (audio == null || audio.isEmpty()) {
            return;
        }
        validateQuestionAudio(audio);
        try {
            Files.createDirectories(questionAudioRoot);
            String originalName = sanitizeFilename(audio.getOriginalFilename());
            String storedName = LocalDateTime.now().toString().replace(":", "-") + "-" + UUID.randomUUID() + "-" + originalName;
            Files.copy(audio.getInputStream(), questionAudioRoot.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            question.setAudioOriginalName(originalName);
            question.setAudioStoredName(storedName);
            question.setAudioContentType(audio.getContentType());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Unable to store question audio.");
        }
    }

    private void deleteQuestionMedia(List<Question> questions) {
        for (Question question : questions) {
            if (question.hasImage()) {
                try {
                    Files.deleteIfExists(questionImageRoot.resolve(question.getImageStoredName()));
                } catch (IOException ignored) {
                }
            }
            if (question.hasAudio()) {
                try {
                    Files.deleteIfExists(questionAudioRoot.resolve(question.getAudioStoredName()));
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void validateQuestionImage(MultipartFile image) {
        if (image.getSize() > MAX_QUESTION_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("Question image size must be 5 MB or smaller.");
        }
        String type = image.getContentType();
        if (type == null || !ALLOWED_IMAGE_TYPES.contains(type)) {
            throw new IllegalArgumentException("Question image must be JPG, PNG, WEBP, or GIF.");
        }
    }

    private void validateQuestionAudio(MultipartFile audio) {
        if (audio.getSize() > MAX_QUESTION_AUDIO_SIZE_BYTES) {
            throw new IllegalArgumentException("Question audio size must be 10 MB or smaller.");
        }
        String type = audio.getContentType();
        if (type == null || !ALLOWED_AUDIO_TYPES.contains(type)) {
            throw new IllegalArgumentException("Question audio must be MP3, WAV, OGG, or M4A.");
        }
    }

    private String sanitizeFilename(String filename) {
        String raw = filename != null ? filename : "media-file";
        String sanitized = Paths.get(raw).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "media-file" : sanitized;
    }

    private String normalizeAssessmentCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void syncRemedialErrorMapping(Test test, Long errorTypeId) {
        if (test == null || test.getId() == null) {
            return;
        }
        List<ErrorTestMapping> existingMappings = errorTestMappingRepository.findByTestId(test.getId());
        existingMappings.forEach(errorTestMappingRepository::delete);
        if (!test.isRemedialTest()) {
            return;
        }
        if (errorTypeId == null) {
            throw new IllegalArgumentException("Please choose an error type for the remedial test.");
        }
        ErrorType errorType = errorTypeRepository.findById(errorTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Selected error type is invalid."));
        if (test.getCourse() != null && !isErrorTypeAvailableForCourse(test.getCourse().getId(), errorTypeId)) {
            throw new IllegalArgumentException("Selected error type does not belong to the selected course.");
        }
        errorTestMappingRepository.findByErrorTypeId(errorTypeId).ifPresent(errorTestMappingRepository::delete);
        ErrorTestMapping mapping = new ErrorTestMapping();
        mapping.setErrorType(errorType);
        mapping.setTest(test);
        errorTestMappingRepository.save(mapping);
    }

    private void applyTargetLesson(Test test, Long targetLessonId) {
        if (test == null || !test.isRemedialTest()) {
            if (test != null) {
                test.setTargetLesson(null);
            }
            return;
        }
        if (targetLessonId == null) {
            test.setTargetLesson(null);
            return;
        }
        Lesson lesson = lessonRepository.findById(targetLessonId)
                .orElseThrow(() -> new IllegalArgumentException("Selected target lesson is invalid."));
        if (test.getCourse() != null && lesson.getCourse() != null
                && !Objects.equals(test.getCourse().getId(), lesson.getCourse().getId())) {
            throw new IllegalArgumentException("Target lesson must belong to the selected course.");
        }
        test.setTargetLesson(lesson);
    }

    private AssessmentContextResponse buildAssessmentContext(Long courseId) {
        List<LessonOption> lessons = lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId).stream()
                .map(lesson -> new LessonOption(lesson.getId(), buildLessonLabel(lesson)))
                .toList();

        Map<Long, ErrorType> errorTypesById = new LinkedHashMap<>();

        // Include error types created directly from lesson Bloom mappings.
        for (Lesson lesson : lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId)) {
            if (lesson == null) {
                continue;
            }
            addErrorTypeIfPresent(errorTypesById, lesson.getErrorType());
            addErrorTypeIfPresent(errorTypesById, lesson.getRememberErrorType());
            addErrorTypeIfPresent(errorTypesById, lesson.getUnderstandErrorType());
            addErrorTypeIfPresent(errorTypesById, lesson.getApplyErrorType());
            addErrorTypeIfPresent(errorTypesById, lesson.getAnalyzeErrorType());
            addErrorTypeIfPresent(errorTypesById, lesson.getEvaluateErrorType());
            addErrorTypeIfPresent(errorTypesById, lesson.getCreateErrorType());
        }

        // Also include error types already mapped to remedial tests in this course.
        List<Long> testIds = testRepository.findByCourseId(courseId).stream()
                .map(Test::getId)
                .filter(Objects::nonNull)
                .toList();
        if (!testIds.isEmpty()) {
            errorTestMappingRepository.findByTestIdIn(testIds).stream()
                    .map(ErrorTestMapping::getErrorType)
                    .forEach(errorType -> addErrorTypeIfPresent(errorTypesById, errorType));
        }

        List<ErrorTypeOption> errorTypes = errorTypesById.values().stream()
                .sorted(Comparator.comparing(ErrorType::getName, String.CASE_INSENSITIVE_ORDER))
                .map(errorType -> new ErrorTypeOption(
                        errorType.getId(),
                        errorType.getName(),
                        errorType.getDescription()))
                .toList();

        return new AssessmentContextResponse(lessons, errorTypes);
    }

    private void addErrorTypeIfPresent(Map<Long, ErrorType> errorTypesById, ErrorType errorType) {
        if (errorType == null || errorType.getId() == null) {
            return;
        }
        errorTypesById.putIfAbsent(errorType.getId(), errorType);
    }

    private String buildLessonLabel(Lesson lesson) {
        String courseName = lesson.getCourse() != null && lesson.getCourse().getName() != null
                ? lesson.getCourse().getName()
                : "Course";
        String lessonOrder = lesson.getSortOrder() != null ? String.valueOf(lesson.getSortOrder()) : "-";
        String title = lesson.getTitle() != null ? lesson.getTitle() : "Untitled";
        return courseName + " - Lesson " + lessonOrder + " - " + title;
    }

    private boolean isErrorTypeAvailableForCourse(Long courseId, Long errorTypeId) {
        if (courseId == null || errorTypeId == null) {
            return false;
        }
        for (Lesson lesson : lessonRepository.findByCourseIdOrderBySortOrderAsc(courseId)) {
            if (lesson == null) {
                continue;
            }
            if (matchesErrorTypeId(lesson.getErrorType(), errorTypeId)
                    || matchesErrorTypeId(lesson.getRememberErrorType(), errorTypeId)
                    || matchesErrorTypeId(lesson.getUnderstandErrorType(), errorTypeId)
                    || matchesErrorTypeId(lesson.getApplyErrorType(), errorTypeId)
                    || matchesErrorTypeId(lesson.getAnalyzeErrorType(), errorTypeId)
                    || matchesErrorTypeId(lesson.getEvaluateErrorType(), errorTypeId)
                    || matchesErrorTypeId(lesson.getCreateErrorType(), errorTypeId)) {
                return true;
            }
        }

        List<Long> testIds = testRepository.findByCourseId(courseId).stream()
                .map(Test::getId)
                .filter(Objects::nonNull)
                .toList();
        if (testIds.isEmpty()) {
            return false;
        }
        return errorTestMappingRepository.findByTestIdIn(testIds).stream()
                .map(ErrorTestMapping::getErrorType)
                .filter(Objects::nonNull)
                .anyMatch(errorType -> errorTypeId.equals(errorType.getId()));
    }

    private boolean matchesErrorTypeId(ErrorType errorType, Long errorTypeId) {
        return errorType != null && errorType.getId() != null && errorTypeId.equals(errorType.getId());
    }

    private record AssessmentContextResponse(List<LessonOption> lessons,
                                             List<ErrorTypeOption> errorTypes) {
    }

    private record LessonOption(Long id, String label) {
    }

    private record ErrorTypeOption(Long id, String name, String description) {
    }

    @GetMapping("/{id}/results")
    public String results(@PathVariable Long id, Model model,
                          @RequestParam(required = false) String passFilter,
                          @RequestParam(required = false) String studentSearch,
                          RedirectAttributes ra) {
        Optional<Test> opt = testRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Test not found.");
            return "redirect:/assessments";
        }

        Test test = opt.get();
        List<StudentResult> allResults = resultRepository.findByTestId(id);
        List<StudentResult> results = allResults.stream()
                .filter(r -> matchesPassFilter(r, passFilter))
                .filter(r -> matchesStudentSearch(r, studentSearch))
                .sorted(Comparator
                        .comparing(StudentResult::isPassed)
                        .thenComparing(StudentResult::getScore, Comparator.nullsLast(Double::compareTo))
                        .thenComparing(r -> displayStudentName(r.getStudent()), String.CASE_INSENSITIVE_ORDER))
                .toList();

        long passCount = results.stream().filter(StudentResult::isPassed).count();
        long failCount = results.size() - passCount;
        Double avg = results.isEmpty()
                ? null
                : results.stream().map(StudentResult::getScore).filter(Objects::nonNull).mapToDouble(Double::doubleValue).average().orElse(0);

        model.addAttribute("test", test);
        model.addAttribute("results", results);
        model.addAttribute("avgScore", avg != null ? String.format("%.1f", avg) : "N/A");
        model.addAttribute("totalCount", results.size());
        model.addAttribute("passCount", passCount);
        model.addAttribute("failCount", failCount);
        model.addAttribute("allResultCount", allResults.size());
        model.addAttribute("passFilter", passFilter);
        model.addAttribute("studentSearch", studentSearch);
        return "assessments/results";
    }

    @GetMapping("/{testId}/results/{resultId}")
    public String resultDetail(@PathVariable Long testId,
                               @PathVariable Long resultId,
                               Model model,
                               RedirectAttributes ra) {
        Optional<Test> testOpt = testRepository.findById(testId);
        Optional<StudentResult> resultOpt = resultRepository.findById(resultId);
        if (testOpt.isEmpty() || resultOpt.isEmpty() || resultOpt.get().getTest() == null
                || !Objects.equals(resultOpt.get().getTest().getId(), testId)) {
            ra.addFlashAttribute("error", "The requested result could not be found.");
            return "redirect:/assessments";
        }

        StudentResult result = resultOpt.get();
        List<ResultQuestionDetail> details = result.getAnswerDetails();
        List<ResultQuestionDetail> wrongDetails = details.stream().filter(detail -> !detail.isCorrect()).toList();

        model.addAttribute("test", testOpt.get());
        model.addAttribute("result", result);
        model.addAttribute("details", details);
        model.addAttribute("wrongDetails", wrongDetails);
        model.addAttribute("correctCount", details.stream().filter(ResultQuestionDetail::isCorrect).count());
        model.addAttribute("wrongCount", wrongDetails.size());
        return "assessments/result-detail";
    }

    private AssessmentType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AssessmentType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean matchesPassFilter(StudentResult result, String passFilter) {
        if (passFilter == null || passFilter.isBlank()) {
            return true;
        }
        if ("pass".equalsIgnoreCase(passFilter)) {
            return result.isPassed();
        }
        if ("fail".equalsIgnoreCase(passFilter)) {
            return !result.isPassed();
        }
        return true;
    }

    private boolean matchesStudentSearch(StudentResult result, String studentSearch) {
        if (studentSearch == null || studentSearch.isBlank()) {
            return true;
        }
        String q = studentSearch.trim().toLowerCase();
        User student = result.getStudent();
        if (student == null) {
            return false;
        }
        return String.valueOf(student.getId()).contains(q)
                || safe(student.getUsername()).contains(q)
                || safe(student.getFullName()).contains(q);
    }

    private String displayStudentName(User student) {
        if (student == null) {
            return "";
        }
        return student.getFullName() != null && !student.getFullName().isBlank()
                ? student.getFullName()
                : safe(student.getUsername());
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}

