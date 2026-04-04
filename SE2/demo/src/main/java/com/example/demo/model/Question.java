package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType = QuestionType.SHORT_ANSWER;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    // Optional fields for Multiple Choice
    @Column(name = "option_a") private String optionA;
    @Column(name = "option_b") private String optionB;
    @Column(name = "option_c") private String optionC;
    @Column(name = "option_d") private String optionD;

    @Column(name = "image_original_name")
    private String imageOriginalName;

    @Column(name = "image_stored_name")
    private String imageStoredName;

    @Column(name = "image_content_type")
    private String imageContentType;

    @Column(name = "audio_original_name")
    private String audioOriginalName;

    @Column(name = "audio_stored_name")
    private String audioStoredName;

    @Column(name = "audio_content_type")
    private String audioContentType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public QuestionType getQuestionType() { return questionType; }
    public void setQuestionType(QuestionType questionType) { this.questionType = questionType; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getImageOriginalName() { return imageOriginalName; }
    public void setImageOriginalName(String imageOriginalName) { this.imageOriginalName = imageOriginalName; }
    public String getImageStoredName() { return imageStoredName; }
    public void setImageStoredName(String imageStoredName) { this.imageStoredName = imageStoredName; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public String getAudioOriginalName() { return audioOriginalName; }
    public void setAudioOriginalName(String audioOriginalName) { this.audioOriginalName = audioOriginalName; }
    public String getAudioStoredName() { return audioStoredName; }
    public void setAudioStoredName(String audioStoredName) { this.audioStoredName = audioStoredName; }
    public String getAudioContentType() { return audioContentType; }
    public void setAudioContentType(String audioContentType) { this.audioContentType = audioContentType; }

    public boolean hasImage() {
        return imageStoredName != null && !imageStoredName.isBlank();
    }

    public boolean hasAudio() {
        return audioStoredName != null && !audioStoredName.isBlank();
    }
}
