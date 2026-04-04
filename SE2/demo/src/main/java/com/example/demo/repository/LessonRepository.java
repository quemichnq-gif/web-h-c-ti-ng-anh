package com.example.demo.repository;

import com.example.demo.model.Course;
import com.example.demo.model.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdOrderBySortOrderAsc(Long courseId);

    List<Lesson> findByCourseOrderBySortOrderAsc(Course course);

    long countByCourseId(Long courseId);

    boolean existsByCourseIdAndSortOrder(Long courseId, Integer sortOrder);

    Optional<Lesson> findByCourseIdAndSortOrder(Long courseId, Integer sortOrder);

    Optional<Lesson> findFirstByCourseIdOrderBySortOrderAsc(Long courseId);

    Optional<Lesson> findFirstByCourseIdOrderBySortOrderDesc(Long courseId);

    void deleteByCourseId(Long courseId);

    List<Lesson> findByTitleContainingIgnoreCase(String keyword);

    List<Lesson> findByCourseIdAndTitleContainingIgnoreCase(Long courseId, String keyword);

    List<Lesson> findByCourseIdAndSortOrderGreaterThanOrderBySortOrderAsc(Long courseId, Integer sortOrder);

    @Query("SELECT DISTINCT l FROM Lesson l LEFT JOIN FETCH l.quizQuestions WHERE l.course.id = :courseId ORDER BY l.sortOrder ASC")
    List<Lesson> findByCourseIdWithQuizQuestions(@Param("courseId") Long courseId);
}