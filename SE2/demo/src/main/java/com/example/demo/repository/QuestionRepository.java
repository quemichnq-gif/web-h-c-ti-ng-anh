package com.example.demo.repository;

import com.example.demo.model.Question;
import com.example.demo.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTest(Test test);
    List<Question> findByTestId(Long testId);
    long countByTest(Test test);
}
