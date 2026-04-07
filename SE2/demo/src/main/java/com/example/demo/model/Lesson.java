package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons", uniqueConstraints = {
        @UniqueConstraint(name = "uk_lessons_code", columnNames = "code")
})
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_type_id")
    private ErrorType errorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remember_error_type_id")
    private ErrorType rememberErrorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "understand_error_type_id")
    private ErrorType understandErrorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apply_error_type_id")
    private ErrorType applyErrorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyze_error_type_id")
    private ErrorType analyzeErrorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluate_error_type_id")
    private ErrorType evaluateErrorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "create_error_type_id")
    private ErrorType createErrorType;

    @Column(nullable = false)
    private String title;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 500)
    private String summary;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    @Column(name = "attachment_original_name")
    private String attachmentOriginalName;

    @Column(name = "attachment_stored_name")
    private String attachmentStoredName;

    @Column(name = "attachment_content_type")
    private String attachmentContentType;

    @Column(name = "image_original_name")
    private String imageOriginalName;

    @Column(name = "image_stored_name")
    private String imageStoredName;

    @Column(name = "image_content_type")
    private String imageContentType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id")
    private Test test;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "video_url")
    private String videoUrl;

    public enum LessonStatus {
        DRAFT, PUBLISHED, ARCHIVED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private LessonStatus status = LessonStatus.DRAFT;

    // ✅ THÊM RELATIONSHIP NÀY - QUAN TRỌNG
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<LessonQuizQuestion> quizQuestions = new ArrayList<>();

    // ========== GETTERS & SETTERS ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public ErrorType getErrorType() { return errorType; }
    public void setErrorType(ErrorType errorType) { this.errorType = errorType; }
    public ErrorType getRememberErrorType() { return rememberErrorType; }
    public void setRememberErrorType(ErrorType rememberErrorType) { this.rememberErrorType = rememberErrorType; }
    public ErrorType getUnderstandErrorType() { return understandErrorType; }
    public void setUnderstandErrorType(ErrorType understandErrorType) { this.understandErrorType = understandErrorType; }
    public ErrorType getApplyErrorType() { return applyErrorType; }
    public void setApplyErrorType(ErrorType applyErrorType) { this.applyErrorType = applyErrorType; }
    public ErrorType getAnalyzeErrorType() { return analyzeErrorType; }
    public void setAnalyzeErrorType(ErrorType analyzeErrorType) { this.analyzeErrorType = analyzeErrorType; }
    public ErrorType getEvaluateErrorType() { return evaluateErrorType; }
    public void setEvaluateErrorType(ErrorType evaluateErrorType) { this.evaluateErrorType = evaluateErrorType; }
    public ErrorType getCreateErrorType() { return createErrorType; }
    public void setCreateErrorType(ErrorType createErrorType) { this.createErrorType = createErrorType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getAttachmentOriginalName() { return attachmentOriginalName; }
    public void setAttachmentOriginalName(String attachmentOriginalName) { this.attachmentOriginalName = attachmentOriginalName; }
    public String getAttachmentStoredName() { return attachmentStoredName; }
    public void setAttachmentStoredName(String attachmentStoredName) { this.attachmentStoredName = attachmentStoredName; }
    public String getAttachmentContentType() { return attachmentContentType; }
    public void setAttachmentContentType(String attachmentContentType) { this.attachmentContentType = attachmentContentType; }
    public String getImageOriginalName() { return imageOriginalName; }
    public void setImageOriginalName(String imageOriginalName) { this.imageOriginalName = imageOriginalName; }
    public String getImageStoredName() { return imageStoredName; }
    public void setImageStoredName(String imageStoredName) { this.imageStoredName = imageStoredName; }
    public String getImageContentType() { return imageContentType; }
    public void setImageContentType(String imageContentType) { this.imageContentType = imageContentType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public LessonStatus getStatus() { return status; }
    public void setStatus(LessonStatus status) { this.status = status; }

    // ✅ GETTER & SETTER CHO quizQuestions
    public List<LessonQuizQuestion> getQuizQuestions() { return quizQuestions; }
    public void setQuizQuestions(List<LessonQuizQuestion> quizQuestions) { this.quizQuestions = quizQuestions; }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = LessonStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean hasAttachment() {
        return attachmentStoredName != null && !attachmentStoredName.isBlank();
    }

    public boolean hasImage() {
        return imageStoredName != null && !imageStoredName.isBlank();
    }

    public ErrorType getErrorTypeForBloomLevel(BloomLevel bloomLevel) {
        if (bloomLevel == null) {
            return null;
        }
        return switch (bloomLevel) {
            case REMEMBER -> rememberErrorType;
            case UNDERSTAND -> understandErrorType;
            case APPLY -> applyErrorType;
            case ANALYZE -> analyzeErrorType;
            case EVALUATE -> evaluateErrorType;
            case CREATE -> createErrorType;
        };
    }
}
