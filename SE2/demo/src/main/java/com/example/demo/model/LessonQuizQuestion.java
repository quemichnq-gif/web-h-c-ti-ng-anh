package com.example.demo.model;

import jakarta.persistence.*;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "lesson_quiz_questions")
public class LessonQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 30)
    private QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    @Column(name = "option_a")
    private String optionA;

    @Column(name = "option_b")
    private String optionB;

    @Column(name = "option_c")
    private String optionC;

    @Column(name = "option_d")
    private String optionD;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(name = "bloom_level", nullable = false, length = 20)
    private BloomLevel bloomLevel = BloomLevel.REMEMBER;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType != null ? questionType : QuestionType.MULTIPLE_CHOICE; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public BloomLevel getBloomLevel() { return bloomLevel; }
    public void setBloomLevel(BloomLevel bloomLevel) { this.bloomLevel = bloomLevel != null ? bloomLevel : BloomLevel.REMEMBER; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isCorrect(String studentAnswer) {
        if (studentAnswer == null || correctAnswer == null) {
            return false;
        }
        if (questionType == QuestionType.SHORT_ANSWER) {
            return correctAnswer.trim().equalsIgnoreCase(studentAnswer.trim());
        }
        return correctAnswer.equalsIgnoreCase(studentAnswer.trim());
    }

    public Map<String, String> getAvailableOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        if (optionA != null && !optionA.isBlank()) options.put("A", optionA);
        if (optionB != null && !optionB.isBlank()) options.put("B", optionB);
        if (optionC != null && !optionC.isBlank()) options.put("C", optionC);
        if (optionD != null && !optionD.isBlank()) options.put("D", optionD);
        return options;
    }

    public boolean isShortAnswer() {
        return questionType == QuestionType.SHORT_ANSWER;
    }
}
