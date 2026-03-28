package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "error_test_mapping")
public class ErrorTestMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "error_type_id", nullable = false)
    private ErrorType errorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ErrorType getErrorType() { return errorType; }
    public void setErrorType(ErrorType errorType) { this.errorType = errorType; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
}
