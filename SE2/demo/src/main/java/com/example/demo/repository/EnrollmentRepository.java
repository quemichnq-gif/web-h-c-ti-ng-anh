package com.example.demo.repository;

import com.example.demo.model.Enrollment;
import com.example.demo.model.User;
import com.example.demo.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStatus(String status);
    List<Enrollment> findByCourse(Course course);
    List<Enrollment> findByCourseId(Long courseId);
    List<Enrollment> findByStudent(User student);
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
    long countByStatus(String status);
    long countByCourse(Course course);
    long countByStudent(User student);
    
    @Query("SELECT e FROM Enrollment e WHERE e.course.id = :courseId AND e.status = :status")
    List<Enrollment> findByCourseIdAndStatus(@Param("courseId") Long courseId, @Param("status") String status);
}
