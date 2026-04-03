package com.example.demo.repository;

import com.example.demo.model.LessonQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonQuizQuestionRepository extends JpaRepository<LessonQuizQuestion, Long> {
    List<LessonQuizQuestion> findByLessonIdOrderBySortOrderAscIdAsc(Long lessonId);
    void deleteByLessonId(Long lessonId);
}
