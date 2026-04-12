package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Configuration
public class BootstrapConfig {

    private static final LocalDateTime SEED_TIME = LocalDateTime.of(2026, 4, 1, 9, 0);
    private static final LocalDateTime SEED_TIME_2 = LocalDateTime.of(2026, 4, 1, 10, 0);
    private static final LocalDateTime SEED_TIME_3 = LocalDateTime.of(2026, 4, 2, 9, 0);

    private static final String ADMIN_USERNAME = "maitrang";
    private static final String ADMIN_EMAIL = "maitrang@university.edu";
    private static final String ADMIN_PASSWORD = "maitrang123";
    private static final String ADMIN_FULL_NAME = "Mai Trang (Admin)";
    private static final String ADMIN_PHONE = "0901234567";

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               CourseRepository courseRepository,
                               LessonRepository lessonRepository,
                               EnrollmentRepository enrollmentRepository,
                               ErrorTypeRepository errorTypeRepository,
                               ErrorTestMappingRepository errorTestMappingRepository,
                               TestRepository testRepository,
                               QuestionRepository questionRepository,
                               LessonQuizQuestionRepository lessonQuizQuestionRepository,
                               StudentResultRepository studentResultRepository,
                               StudentErrorRepository studentErrorRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = upsertUser(userRepository, passwordEncoder,
                    ADMIN_USERNAME, ADMIN_EMAIL, ADMIN_PASSWORD, Role.ADMIN,
                    "ACTIVE", ADMIN_FULL_NAME, ADMIN_PHONE, SEED_TIME);

            User staff = upsertUser(userRepository, passwordEncoder,
                    "staff", "staff@university.edu", "staff123", Role.ACADEMIC_STAFF,
                    "ACTIVE", "Nguyen Van Nhan", "0902345678", SEED_TIME);

            List<User> students = List.of(
                    upsertUser(userRepository, passwordEncoder,
                            "student1", "student1@university.edu", "student123", Role.STUDENT,
                            "ACTIVE", "Tran Anh Tuan", null, SEED_TIME),
                    upsertUser(userRepository, passwordEncoder,
                            "student2", "student2@university.edu", "student123", Role.STUDENT,
                            "ACTIVE", "Pham Thi Mai", null, SEED_TIME),
                    upsertUser(userRepository, passwordEncoder,
                            "student3", "student3@university.edu", "student123", Role.STUDENT,
                            "ACTIVE", "Le Gia Hung", null, SEED_TIME),
                    upsertUser(userRepository, passwordEncoder,
                            "student4", "student4@university.edu", "student123", Role.STUDENT,
                            "ACTIVE", "Vu Nhat Minh", null, SEED_TIME),
                    upsertUser(userRepository, passwordEncoder,
                            "student5", "student5@university.edu", "student123", Role.STUDENT,
                            "ACTIVE", "Hoang Kim Chi", null, SEED_TIME),
                    upsertUser(userRepository, passwordEncoder,
                            "student6", "student6@university.edu", "student123", Role.STUDENT,
                            "ACTIVE", "Do Bao Ngoc", null, SEED_TIME)
            );

            ErrorType grammar = upsertErrorType(errorTypeRepository,
                    "Grammar", "Cac loi ve ngu phap co ban");
            ErrorType vocabulary = upsertErrorType(errorTypeRepository,
                    "Vocabulary", "Dung tu sai ngu canh");
            ErrorType pronunciation = upsertErrorType(errorTypeRepository,
                    "Pronunciation", "Loi phat am am cuoi va am gio");

            Course writingCourse = upsertCourse(courseRepository,
                    "IELTS-WRITING-T2",
                    "IELTS Academic Writing Task 2",
                    "Hoc cach viet luan Academic IELTS dat band 7.0+",
                    CourseStatus.OPEN,
                    LocalDate.of(2026, 4, 15),
                    LocalDate.of(2026, 7, 15),
                    SEED_TIME);

            Course businessCourse = upsertCourse(courseRepository,
                    "BUSINESS-ENGLISH",
                    "Communicative Business English",
                    "Tieng Anh giao tiep chuyen sau cho moi truong van phong quoc te.",
                    CourseStatus.OPEN,
                    LocalDate.of(2026, 4, 20),
                    LocalDate.of(2026, 10, 20),
                    SEED_TIME);

            Course toeicCourse = upsertCourse(courseRepository,
                    "TOEIC-800-PLUS",
                    "TOEIC Intensive 800+",
                    "Luyen de TOEIC than toc, nam vung meo thi dat diem cao.",
                    CourseStatus.OPEN,
                    LocalDate.of(2026, 3, 20),
                    LocalDate.of(2026, 8, 20),
                    SEED_TIME);

            Lesson writingLesson1 = upsertLesson(lessonRepository, writingCourse, grammar,
                    "LES-001",
                    "Gioi thieu ve Writing Task 2",
                    "Overview of essay structure, task response, and common mistakes.",
                    "Noi dung gioi thieu ve cau truc bai Writing Task 2.",
                    1,
                    Lesson.LessonStatus.PUBLISHED,
                    45,
                    "https://example.com/videos/writing-task-2-intro");

            Lesson writingLesson2 = upsertLesson(lessonRepository, writingCourse, grammar,
                    "LES-002",
                    "Thesis Statement and Topic Sentences",
                    "How to write a clear thesis statement and supporting topic sentences.",
                    "Nang cao ky nang viet thesis statement va topic sentence.",
                    2,
                    Lesson.LessonStatus.PUBLISHED,
                    50,
                    "https://example.com/videos/thesis-statement");

            Lesson businessLesson1 = upsertLesson(lessonRepository, businessCourse, vocabulary,
                    "BUS-001",
                    "Business Greetings and Introductions",
                    "Useful language for office meetings and introductions.",
                    "Cac mau cau giao tiep trong moi truong cong so.",
                    1,
                    Lesson.LessonStatus.PUBLISHED,
                    40,
                    "https://example.com/videos/business-greetings");

            Lesson toeicLesson1 = upsertLesson(lessonRepository, toeicCourse, pronunciation,
                    "TOEIC-001",
                    "Listening Warm-up",
                    "Short listening strategies for part 1 and part 2.",
                    "Kich hoat ky nang nghe co ban cho bai thi TOEIC.",
                    1,
                    Lesson.LessonStatus.DRAFT,
                    30,
                    "https://example.com/videos/toeic-listening-warmup");

            Test writingTest = upsertTest(testRepository, writingCourse, writingLesson1,
                    "TEST-WRITING-001",
                    "Writing Skills Check",
                    "Kiem tra ky nang viet co ban.",
                    60,
                    AssessmentType.COURSE_ASSESSMENT);

            Test remedialTest = upsertTest(testRepository, writingCourse, writingLesson2,
                    "TEST-WRITING-REMEDIAL",
                    "Grammar Recovery Drill",
                    "Bai test bo sung cho hoc vien gap loi ngu phap lap lai.",
                    25,
                    AssessmentType.REMEDIAL_TEST);

            Test businessTest = upsertTest(testRepository, businessCourse, businessLesson1,
                    "TEST-BUSINESS-001",
                    "Business English Starter",
                    "Kiem tra tu vung va giao tiep cong so.",
                    30,
                    AssessmentType.COURSE_ASSESSMENT);

            List<Question> writingQuestions = List.of(
                    upsertQuestion(questionRepository, writingTest,
                            "Which sentence uses the correct verb form?",
                            QuestionType.MULTIPLE_CHOICE,
                            "B", "The team is ready.", "The team are ready.", "The team be ready.", null),
                    upsertQuestion(questionRepository, writingTest,
                            "Complete the sentence: Each of the students ___ responsible.",
                            QuestionType.SHORT_ANSWER,
                            "is", null, null, null, null),
                    upsertQuestion(questionRepository, writingTest,
                            "Choose the best topic sentence for an opinion essay.",
                            QuestionType.MULTIPLE_CHOICE,
                            "A", "This essay explains both sides.", "I strongly believe online learning is effective.", "Many people study every day.", null),
                    upsertQuestion(questionRepository, writingTest,
                            "Fill in the blank: The list of items ___ on the desk.",
                            QuestionType.SHORT_ANSWER,
                            "is", null, null, null, null)
            );

            List<Question> remedialQuestions = List.of(
                    upsertQuestion(questionRepository, remedialTest,
                            "Pick the correct sentence.",
                            QuestionType.MULTIPLE_CHOICE,
                            "C", "She go to class every day.", "The team are winning.", "Each student is ready.", null),
                    upsertQuestion(questionRepository, remedialTest,
                            "Complete: The data ___ accurate.",
                            QuestionType.SHORT_ANSWER,
                            "are", null, null, null, null)
            );

            upsertQuestion(questionRepository, businessTest,
                    "Choose the best greeting for a morning meeting.",
                    QuestionType.MULTIPLE_CHOICE,
                    "D", "Good night everyone.", "See you tomorrow.", "Thanks for your help.", "Good morning, everyone.");
            upsertQuestion(questionRepository, businessTest,
                    "Complete the sentence: Our manager ___ the report yesterday.",
                    QuestionType.SHORT_ANSWER,
                    "reviewed", null, null, null, null);

            upsertLessonQuizQuestion(lessonQuizQuestionRepository, writingLesson1,
                    1,
                    "What is the main purpose of a thesis statement?",
                    QuestionType.MULTIPLE_CHOICE,
                    "A", "To present the essay position", "To list every example", "To copy the introduction", "To finish the conclusion",
                    BloomLevel.UNDERSTAND,
                    "A thesis statement tells the reader the essay position.");

            upsertLessonQuizQuestion(lessonQuizQuestionRepository, writingLesson1,
                    2,
                    "Write one sentence that can be used as a topic sentence.",
                    QuestionType.SHORT_ANSWER,
                    "Any clear topic sentence", null, null, null, null,
                    BloomLevel.APPLY,
                    "The answer should state one clear main idea.");

            upsertLessonQuizQuestion(lessonQuizQuestionRepository, writingLesson2,
                    1,
                    "Which part of an essay comes after the introduction?",
                    QuestionType.MULTIPLE_CHOICE,
                    "B", "Reference list", "Body paragraph", "Title page", "Appendix",
                    BloomLevel.REMEMBER,
                    "The body paragraph follows the introduction.");

            upsertLessonQuizQuestion(lessonQuizQuestionRepository, businessLesson1,
                    1,
                    "What is a polite way to start a meeting?",
                    QuestionType.MULTIPLE_CHOICE,
                    "C", "Let's finish immediately.", "Close the door.", "Good morning, everyone.", "I disagree with all of you.",
                    BloomLevel.REMEMBER,
                    "A polite greeting is suitable for a meeting.");

            upsertEnrollment(enrollmentRepository, students.get(0), writingCourse, staff,
                    EnrollmentStatus.APPROVED, SEED_TIME_2, SEED_TIME_2, null, null);
            upsertEnrollment(enrollmentRepository, students.get(1), writingCourse, staff,
                    EnrollmentStatus.APPROVED, SEED_TIME_2.plusMinutes(10), SEED_TIME_2.plusMinutes(10), null, null);
            upsertEnrollment(enrollmentRepository, students.get(2), writingCourse, staff,
                    EnrollmentStatus.APPROVED, SEED_TIME_2.plusMinutes(20), SEED_TIME_2.plusMinutes(20), null, null);
            upsertEnrollment(enrollmentRepository, students.get(3), writingCourse, staff,
                    EnrollmentStatus.APPROVED, SEED_TIME_2.plusMinutes(30), SEED_TIME_2.plusMinutes(30), null, null);
            upsertEnrollment(enrollmentRepository, students.get(4), businessCourse, staff,
                    EnrollmentStatus.PENDING, SEED_TIME_2.plusMinutes(40), SEED_TIME_2.plusMinutes(40), "Waiting for review", "Requested by student");
            upsertEnrollment(enrollmentRepository, students.get(5), toeicCourse, staff,
                    EnrollmentStatus.REJECTED, SEED_TIME_2.plusMinutes(50), SEED_TIME_2.plusMinutes(50), "Missing prerequisites", "Incomplete placement test");

            upsertErrorTestMapping(errorTestMappingRepository, grammar, remedialTest);
            upsertErrorTestMapping(errorTestMappingRepository, vocabulary, businessTest);

            upsertStudentError(studentErrorRepository, students.get(0), grammar, SEED_TIME_3);
            upsertStudentError(studentErrorRepository, students.get(1), grammar, SEED_TIME_3.plusHours(1));
            upsertStudentError(studentErrorRepository, students.get(2), vocabulary, SEED_TIME_3.plusHours(2));

            upsertStudentResult(studentResultRepository, students.get(0), writingTest,
                    8.5, SEED_TIME_3.plusDays(1), buildSampleDetails(writingQuestions, 8.5));
            upsertStudentResult(studentResultRepository, students.get(1), writingTest,
                    7.0, SEED_TIME_3.plusDays(1).plusHours(2), buildSampleDetails(writingQuestions, 7.0));
            upsertStudentResult(studentResultRepository, students.get(2), remedialTest,
                    9.0, SEED_TIME_3.plusDays(2), buildSampleDetails(remedialQuestions, 9.0));

            backfillLegacyResultDetails(studentResultRepository, questionRepository);
        };
    }

    private User upsertUser(UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            String username,
                            String email,
                            String rawPassword,
                            Role role,
                            String status,
                            String fullName,
                            String phone,
                            LocalDateTime createdAt) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(email))
                .orElseGet(User::new);
        user.setUsername(username);
        user.setEmail(email);
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        user.setRole(role);
        user.setStatus(status);
        user.setFullName(fullName);
        user.setPhone(phone);
        if (createdAt != null) {
            user.setCreatedAt(createdAt);
        }
        return userRepository.save(user);
    }

    private Course upsertCourse(CourseRepository courseRepository,
                                String code,
                                String name,
                                String description,
                                CourseStatus status,
                                LocalDate startDate,
                                LocalDate endDate,
                                LocalDateTime seedTime) {
        Course course = courseRepository.findByCodeIgnoreCase(code).orElseGet(Course::new);
        course.setCode(code);
        course.setName(name);
        course.setDescription(description);
        course.setStatus(status);
        course.setStartDate(startDate);
        course.setEndDate(endDate);
        course.setCreatedAt(seedTime);
        course.setUpdatedAt(seedTime);
        return courseRepository.save(course);
    }

    private ErrorType upsertErrorType(ErrorTypeRepository errorTypeRepository,
                                      String name,
                                      String description) {
        ErrorType errorType = errorTypeRepository.findByNameIgnoreCase(name).orElseGet(ErrorType::new);
        errorType.setName(name);
        errorType.setDescription(description);
        return errorTypeRepository.save(errorType);
    }

    private Lesson upsertLesson(LessonRepository lessonRepository,
                                Course course,
                                ErrorType errorType,
                                String code,
                                String title,
                                String summary,
                                String content,
                                int sortOrder,
                                Lesson.LessonStatus status,
                                Integer duration,
                                String videoUrl) {
        Lesson lesson = lessonRepository.findByCodeIgnoreCase(code).orElseGet(Lesson::new);
        lesson.setCourse(course);
        lesson.setErrorType(errorType);
        lesson.setCode(code);
        lesson.setTitle(title);
        lesson.setSummary(summary);
        lesson.setContent(content);
        lesson.setSortOrder(sortOrder);
        lesson.setStatus(status);
        lesson.setDuration(duration);
        lesson.setVideoUrl(videoUrl);
        lesson.setCreatedAt(SEED_TIME);
        lesson.setUpdatedAt(SEED_TIME);
        return lessonRepository.save(lesson);
    }

    private Test upsertTest(TestRepository testRepository,
                            Course course,
                            Lesson targetLesson,
                            String code,
                            String title,
                            String description,
                            Integer duration,
                            AssessmentType assessmentType) {
        Test test = testRepository.findByCodeIgnoreCase(code).orElseGet(Test::new);
        test.setCourse(course);
        test.setTargetLesson(targetLesson);
        test.setCode(code);
        test.setTitle(title);
        test.setDescription(description);
        test.setDuration(duration);
        test.setAssessmentType(assessmentType);
        test.setCreatedAt(SEED_TIME);
        test.setUpdatedAt(SEED_TIME);
        return testRepository.save(test);
    }

    private Question upsertQuestion(QuestionRepository questionRepository,
                                    Test test,
                                    String content,
                                    QuestionType questionType,
                                    String correctAnswer,
                                    String optionA,
                                    String optionB,
                                    String optionC,
                                    String optionD) {
        Question question = questionRepository.findFirstByTestIdAndContentIgnoreCase(test.getId(), content)
                .orElseGet(Question::new);
        question.setTest(test);
        question.setQuestionType(questionType);
        question.setContent(content);
        question.setCorrectAnswer(correctAnswer);
        question.setCorrectOption(normalizeCorrectOption(correctAnswer));
        question.setOptionA(optionA);
        question.setOptionB(optionB);
        question.setOptionC(optionC);
        question.setOptionD(optionD);
        return questionRepository.save(question);
    }

    private LessonQuizQuestion upsertLessonQuizQuestion(LessonQuizQuestionRepository lessonQuizQuestionRepository,
                                                        Lesson lesson,
                                                        int sortOrder,
                                                        String questionText,
                                                        QuestionType questionType,
                                                        String correctAnswer,
                                                        String optionA,
                                                        String optionB,
                                                        String optionC,
                                                        String optionD,
                                                        BloomLevel bloomLevel,
                                                        String explanation) {
        LessonQuizQuestion question = lessonQuizQuestionRepository
                .findByLessonIdAndSortOrder(lesson.getId(), sortOrder)
                .orElseGet(LessonQuizQuestion::new);
        question.setLesson(lesson);
        question.setSortOrder(sortOrder);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setCorrectAnswer(correctAnswer);
        question.setOptionA(optionA);
        question.setOptionB(optionB);
        question.setOptionC(optionC);
        question.setOptionD(optionD);
        question.setBloomLevel(bloomLevel);
        question.setExplanation(explanation);
        return lessonQuizQuestionRepository.save(question);
    }

    private Enrollment upsertEnrollment(EnrollmentRepository enrollmentRepository,
                                        User student,
                                        Course course,
                                        User academicStaff,
                                        EnrollmentStatus status,
                                        LocalDateTime enrolledAt,
                                        LocalDateTime processedAt,
                                        String rejectReason,
                                        String note) {
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseGet(Enrollment::new);
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setAcademicStaff(academicStaff);
        enrollment.setStatus(status);
        enrollment.setEnrolledAt(enrolledAt);
        enrollment.setProcessedAt(processedAt);
        enrollment.setRejectReason(rejectReason);
        enrollment.setNote(note);
        enrollment.setCreatedAt(enrolledAt);
        enrollment.setUpdatedAt(processedAt);
        return enrollmentRepository.save(enrollment);
    }

    private ErrorTestMapping upsertErrorTestMapping(ErrorTestMappingRepository errorTestMappingRepository,
                                                    ErrorType errorType,
                                                    Test test) {
        ErrorTestMapping mapping = errorTestMappingRepository.findByErrorTypeId(errorType.getId())
                .orElseGet(ErrorTestMapping::new);
        mapping.setErrorType(errorType);
        mapping.setTest(test);
        return errorTestMappingRepository.save(mapping);
    }

    private StudentError upsertStudentError(StudentErrorRepository studentErrorRepository,
                                            User student,
                                            ErrorType errorType,
                                            LocalDateTime createdAt) {
        Optional<StudentError> existing = studentErrorRepository.findByStudentId(student.getId()).stream()
                .filter(entry -> entry.getErrorType() != null && errorType.getId().equals(entry.getErrorType().getId()))
                .findFirst();
        StudentError studentError = existing.orElseGet(StudentError::new);
        studentError.setStudent(student);
        studentError.setErrorType(errorType);
        studentError.setCreatedAt(createdAt);
        return studentErrorRepository.save(studentError);
    }

    private StudentResult upsertStudentResult(StudentResultRepository studentResultRepository,
                                              User student,
                                              Test test,
                                              double score,
                                              LocalDateTime submittedAt,
                                              List<ResultQuestionDetail> details) {
        StudentResult result = studentResultRepository.findByStudentIdAndTestId(student.getId(), test.getId())
                .orElseGet(StudentResult::new);
        result.setStudent(student);
        result.setTest(test);
        result.setScore(score);
        result.setSubmittedAt(submittedAt);
        result.setAnswerDetails(details);
        return studentResultRepository.save(result);
    }

    private void backfillLegacyResultDetails(StudentResultRepository studentResultRepository,
                                             QuestionRepository questionRepository) {
        for (StudentResult result : studentResultRepository.findAll()) {
            if (result.hasAnswerDetails()) {
                continue;
            }
            Long testId = result.getTest() != null ? result.getTest().getId() : null;
            if (testId == null) {
                continue;
            }
            List<Question> questions = questionRepository.findByTestId(testId);
            if (questions.isEmpty()) {
                continue;
            }
            result.setAnswerDetails(buildSampleDetails(questions, result.getScore()));
            studentResultRepository.save(result);
        }
    }

    private List<ResultQuestionDetail> buildSampleDetails(List<Question> questions, Double score) {
        List<ResultQuestionDetail> details = new ArrayList<>();
        int totalQuestions = questions.size();
        int expectedCorrect = totalQuestions == 0 || score == null
                ? 0
                : Math.max(0, Math.min(totalQuestions, (int) Math.round((score / 10.0) * totalQuestions)));

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            boolean isCorrect = i < expectedCorrect;
            ResultQuestionDetail detail = new ResultQuestionDetail();
            detail.setQuestionId(question.getId());
            detail.setQuestionNumber(i + 1);
            detail.setQuestionType(question.getQuestionType().name());
            detail.setQuestionContent(question.getContent());
            detail.setCorrectAnswer(question.getCorrectAnswer());
            detail.setStudentAnswer(generateSampleAnswer(question, isCorrect));
            detail.setCorrect(isCorrect);
            detail.setOptions(Stream.of(
                            optionLabel("A", question.getOptionA()),
                            optionLabel("B", question.getOptionB()),
                            optionLabel("C", question.getOptionC()),
                            optionLabel("D", question.getOptionD()))
                    .filter(java.util.Objects::nonNull)
                    .toList());
            details.add(detail);
        }
        return details;
    }

    private String generateSampleAnswer(Question question, boolean correct) {
        if (correct) {
            return question.getCorrectAnswer();
        }
        if (question.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
            for (String choice : List.of("A", "B", "C", "D")) {
                if (!choice.equalsIgnoreCase(question.getCorrectAnswer())) {
                    return choice;
                }
            }
        }
        return "Sample incorrect answer";
    }

    private String optionLabel(String key, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return key + ". " + value;
    }

    private String normalizeCorrectOption(String value) {
        if (value == null || value.isBlank()) {
            return "A";
        }
        return value.trim().substring(0, 1).toUpperCase();
    }
}
