package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/errors")
public class ErrorManagementController {

    private final ErrorTypeRepository errorTypeRepository;
    private final ErrorTestMappingRepository mappingRepository;
    private final StudentErrorRepository studentErrorRepository;
    private final TestRepository testRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public ErrorManagementController(ErrorTypeRepository errorTypeRepository,
                                     ErrorTestMappingRepository mappingRepository,
                                     StudentErrorRepository studentErrorRepository,
                                     TestRepository testRepository,
                                     UserRepository userRepository,
                                     LessonRepository lessonRepository) {
        this.errorTypeRepository = errorTypeRepository;
        this.mappingRepository = mappingRepository;
        this.studentErrorRepository = studentErrorRepository;
        this.testRepository = testRepository;
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public String list(Model model,
                       @RequestParam(required = false) String search,
                       @RequestParam(required = false) String studentKeyword) {
        List<ErrorType> types = errorTypeRepository.findAll();
        if (search != null && !search.isBlank()) {
            final String q = search.toLowerCase();
            types = types.stream()
                    .filter(e -> safe(e.getName()).contains(q) || safe(e.getDescription()).contains(q))
                    .sorted(Comparator.comparing(ErrorType::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } else {
            types = types.stream()
                    .sorted(Comparator.comparing(ErrorType::getName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        Map<Long, ErrorTestMapping> mappingByErrorId = mappingRepository.findAll().stream()
                .filter(mapping -> mapping.getErrorType() != null && mapping.getErrorType().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        mapping -> mapping.getErrorType().getId(),
                        mapping -> mapping,
                        (left, right) -> left,
                        LinkedHashMap::new));

        List<StudentError> allStudentErrors = studentErrorRepository.findAllOrderByCreatedAtDesc();
        List<StudentError> recentStudentErrors = allStudentErrors.stream().limit(20).toList();
        List<ErrorSummaryItem> summaries = buildErrorSummaries(types, allStudentErrors, studentKeyword);

        model.addAttribute("errorTypes", types);
        model.addAttribute("tests", testRepository.findAll());
        model.addAttribute("remedialTests", testRepository.findByAssessmentType(AssessmentType.REMEDIAL_TEST));
        model.addAttribute("mappingByErrorId", mappingByErrorId);
        model.addAttribute("errorContextById", buildErrorContextById(types));
        model.addAttribute("studentErrors", recentStudentErrors);
        model.addAttribute("errorSummaries", summaries);
        model.addAttribute("search", search);
        model.addAttribute("studentKeyword", studentKeyword);
        return "errors/list";
    }

    @GetMapping("/create")
    public String createForm() {
        return "errors/create";
    }

    @PostMapping("/create")
    public String createType(@RequestParam String name,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        ErrorType et = new ErrorType();
        et.setName(name);
        et.setDescription(description);
        errorTypeRepository.save(et);
        ra.addFlashAttribute("success", "Error type '" + name + "' created successfully.");
        return "redirect:/errors";
    }

    @GetMapping("/types/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Optional<ErrorType> errorTypeOpt = errorTypeRepository.findById(id);
        if (errorTypeOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Error type not found.");
            return "redirect:/errors";
        }
        model.addAttribute("errorType", errorTypeOpt.get());
        return "errors/edit";
    }

    @PostMapping("/types/{id}/edit")
    public String updateType(@PathVariable Long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String description,
                             RedirectAttributes ra) {
        Optional<ErrorType> errorTypeOpt = errorTypeRepository.findById(id);
        if (errorTypeOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Error type not found.");
            return "redirect:/errors";
        }
        String normalizedName = name != null ? name.trim() : "";
        if (normalizedName.isBlank()) {
            ra.addFlashAttribute("error", "Error type name is required.");
            return "redirect:/errors/types/" + id + "/edit";
        }
        if (errorTypeRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            ra.addFlashAttribute("error", "An error type with this name already exists.");
            return "redirect:/errors/types/" + id + "/edit";
        }

        ErrorType errorType = errorTypeOpt.get();
        errorType.setName(normalizedName);
        errorType.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        errorTypeRepository.save(errorType);
        ra.addFlashAttribute("success", "Error type updated successfully.");
        return "redirect:/errors";
    }

    @PostMapping("/types/{id}/delete")
    public String deleteType(@PathVariable Long id, RedirectAttributes ra) {
        mappingRepository.findByErrorTypeId(id).ifPresent(mappingRepository::delete);
        List<StudentError> related = studentErrorRepository.findByErrorTypeId(id);
        studentErrorRepository.deleteAll(related);
        errorTypeRepository.deleteById(id);
        ra.addFlashAttribute("success", "Error type deleted successfully.");
        return "redirect:/errors";
    }

    @PostMapping("/mappings/create")
    public String createMapping(@RequestParam(required = false) Long errorTypeId,
                                @RequestParam(required = false) Long testId,
                                RedirectAttributes ra) {
        if (errorTypeId == null || testId == null) {
            ra.addFlashAttribute("error", "Please choose both an error type and a remedial test.");
            return "redirect:/errors";
        }
        Optional<ErrorType> etOpt = errorTypeRepository.findById(errorTypeId);
        Optional<Test> testOpt = testRepository.findById(testId);
        if (etOpt.isEmpty() || testOpt.isEmpty()) {
            ra.addFlashAttribute("error", "Invalid data.");
            return "redirect:/errors";
        }
        if (!testOpt.get().isRemedialTest()) {
            ra.addFlashAttribute("error", "Error types can only be linked to remedial tests.");
            return "redirect:/errors";
        }
        mappingRepository.findByErrorTypeId(errorTypeId).ifPresent(mappingRepository::delete);

        ErrorTestMapping mapping = new ErrorTestMapping();
        mapping.setErrorType(etOpt.get());
        mapping.setTest(testOpt.get());
        mappingRepository.save(mapping);
        ra.addFlashAttribute("success", "Test linked to error type successfully.");
        return "redirect:/errors";
    }

    @GetMapping("/students")
    public String studentErrors(Model model,
                                @RequestParam(required = false) Long studentId,
                                @RequestParam(required = false) String search) {
        List<User> students = userRepository.findByRole(Role.STUDENT).stream()
                .sorted(Comparator.comparing(this::studentSortLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<StudentError> filteredErrors = studentErrorRepository.findAllOrderByCreatedAtDesc().stream()
                .filter(studentError -> studentId == null || matchesStudentId(studentError, studentId))
                .filter(studentError -> matchesStudentKeyword(studentError.getStudent(), search))
                .toList();

        Optional<User> selectedStudent = studentId == null
                ? Optional.empty()
                : userRepository.findById(studentId).filter(user -> user.getRole() == Role.STUDENT);

        model.addAttribute("students", students);
        model.addAttribute("studentErrors", filteredErrors);
        model.addAttribute("selectedStudent", selectedStudent.orElse(null));
        model.addAttribute("selectedStudentId", studentId);
        model.addAttribute("search", search);
        model.addAttribute("errorSummaries", buildStudentErrorSummaries(filteredErrors));
        return "errors/students";
    }

    private boolean matchesStudentId(StudentError studentError, Long studentId) {
        return studentError.getStudent() != null && Objects.equals(studentError.getStudent().getId(), studentId);
    }

    private List<ErrorSummaryItem> buildErrorSummaries(List<ErrorType> types,
                                                       List<StudentError> allStudentErrors,
                                                       String studentKeyword) {
        Set<Long> visibleErrorTypeIds = types.stream().map(ErrorType::getId).collect(java.util.stream.Collectors.toSet());
        List<StudentError> filteredErrors = allStudentErrors.stream()
                .filter(error -> error.getErrorType() != null && visibleErrorTypeIds.contains(error.getErrorType().getId()))
                .filter(error -> matchesStudentKeyword(error.getStudent(), studentKeyword))
                .toList();
        return buildStudentErrorSummaries(filteredErrors);
    }

    private List<ErrorSummaryItem> buildStudentErrorSummaries(List<StudentError> studentErrors) {
        Map<Long, List<StudentError>> grouped = new LinkedHashMap<>();
        for (StudentError studentError : studentErrors) {
            if (studentError.getErrorType() == null || studentError.getErrorType().getId() == null) {
                continue;
            }
            grouped.computeIfAbsent(studentError.getErrorType().getId(), key -> new ArrayList<>()).add(studentError);
        }

        return grouped.values().stream()
                .map(this::toSummary)
                .sorted(Comparator
                        .comparing(ErrorSummaryItem::totalOccurrences, Comparator.reverseOrder())
                        .thenComparing(ErrorSummaryItem::affectedStudentCount, Comparator.reverseOrder())
                        .thenComparing(ErrorSummaryItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private ErrorSummaryItem toSummary(List<StudentError> items) {
        StudentError sample = items.get(0);
        ErrorType errorType = sample.getErrorType();
        Map<Long, StudentChip> uniqueStudents = new LinkedHashMap<>();
        for (StudentError item : items) {
            User student = item.getStudent();
            if (student == null || student.getId() == null) {
                continue;
            }
            uniqueStudents.putIfAbsent(student.getId(), new StudentChip(student.getId(), studentDisplayName(student), safe(student.getUsername())));
        }
        List<StudentChip> students = uniqueStudents.values().stream()
                .sorted(Comparator.comparing(StudentChip::sortKey, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new ErrorSummaryItem(
                errorType.getId(),
                errorType.getName(),
                errorType.getDescription(),
                items.size(),
                students.size(),
                students);
    }

    private boolean matchesStudentKeyword(User student, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        if (student == null) {
            return false;
        }
        String q = keyword.trim().toLowerCase();
        return String.valueOf(student.getId()).contains(q)
                || safe(student.getUsername()).contains(q)
                || safe(student.getFullName()).contains(q);
    }

    private String studentDisplayName(User student) {
        if (student == null) {
            return "Unknown";
        }
        if (student.getFullName() != null && !student.getFullName().isBlank()) {
            return student.getFullName();
        }
        return student.getUsername();
    }

    private String studentSortLabel(User student) {
        return studentDisplayName(student) + " " + safe(student.getUsername()) + " " + student.getId();
    }

    private String safe(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private Map<Long, String> buildErrorContextById(List<ErrorType> types) {
        Map<Long, String> contextById = new LinkedHashMap<>();
        Set<Long> visibleIds = types.stream()
                .map(ErrorType::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        for (Lesson lesson : lessonRepository.findAll()) {
            if (lesson == null) {
                continue;
            }
            addLessonErrorContext(contextById, visibleIds, lesson, BloomLevel.REMEMBER, lesson.getRememberErrorType());
            addLessonErrorContext(contextById, visibleIds, lesson, BloomLevel.UNDERSTAND, lesson.getUnderstandErrorType());
            addLessonErrorContext(contextById, visibleIds, lesson, BloomLevel.APPLY, lesson.getApplyErrorType());
            addLessonErrorContext(contextById, visibleIds, lesson, BloomLevel.ANALYZE, lesson.getAnalyzeErrorType());
            addLessonErrorContext(contextById, visibleIds, lesson, BloomLevel.EVALUATE, lesson.getEvaluateErrorType());
            addLessonErrorContext(contextById, visibleIds, lesson, BloomLevel.CREATE, lesson.getCreateErrorType());
        }

        for (ErrorType type : types) {
            if (type != null && type.getId() != null) {
                contextById.putIfAbsent(type.getId(), type.getName());
            }
        }
        return contextById;
    }

    private void addLessonErrorContext(Map<Long, String> contextById,
                                       Set<Long> visibleIds,
                                       Lesson lesson,
                                       BloomLevel bloomLevel,
                                       ErrorType errorType) {
        if (lesson == null || bloomLevel == null || errorType == null || errorType.getId() == null || !visibleIds.contains(errorType.getId())) {
            return;
        }
        String lessonTitle = lesson.getTitle() != null && !lesson.getTitle().isBlank() ? lesson.getTitle() : "Untitled Lesson";
        contextById.putIfAbsent(errorType.getId(), lessonTitle + " - " + bloomLevel.getLabel() + " - " + errorType.getName());
    }

    public record ErrorSummaryItem(Long id,
                                   String name,
                                   String description,
                                   int totalOccurrences,
                                   int affectedStudentCount,
                                   List<StudentChip> students) {
    }

    public record StudentChip(Long id, String label, String username) {
        public String sortKey() {
            return label + " " + username + " " + id;
        }
    }
}

