package com.example.demo.repository;

import com.example.demo.model.LessonQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LessonQuizQuestionRepository extends JpaRepository<LessonQuizQuestion, Long> {

    List<LessonQuizQuestion> findByLessonIdOrderBySortOrderAsc(Long lessonId);

    long countByLessonId(Long lessonId);

    Optional<LessonQuizQuestion> findByLessonIdAndSortOrder(Long lessonId, Integer sortOrder);

    Optional<LessonQuizQuestion> findFirstByLessonIdOrderBySortOrderAsc(Long lessonId);

    Optional<LessonQuizQuestion> findFirstByLessonIdOrderBySortOrderDesc(Long lessonId);

    @Transactional
    @Modifying
    void deleteByLessonId(Long lessonId);

    @Transactional
    @Modifying
    @Query("DELETE FROM LessonQuizQuestion q WHERE q.lesson.id = :lessonId")
    void deleteAllByLessonId(@Param("lessonId") Long lessonId);

    List<LessonQuizQuestion> findByLessonIdAndSortOrderGreaterThanOrderBySortOrderAsc(Long lessonId, Integer sortOrder);
}