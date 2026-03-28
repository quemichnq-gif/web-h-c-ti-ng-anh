package com.example.demo.repository;

import com.example.demo.model.Test;
import com.example.demo.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByCourse(Course course);
    List<Test> findByCourseId(Long courseId);
    long countByCourse(Course course);
}
