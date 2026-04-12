package com.example.demo.repository;

import com.example.demo.model.Question;
import com.example.demo.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTest(Test test);
    List<Question> findByTestId(Long testId);
    Optional<Question> findFirstByTestIdAndContentIgnoreCase(Long testId, String content);
    long countByTest(Test test);
}
