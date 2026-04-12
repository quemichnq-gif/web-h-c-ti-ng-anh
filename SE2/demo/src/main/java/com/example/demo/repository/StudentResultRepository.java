package com.example.demo.repository;

import com.example.demo.model.StudentResult;
import com.example.demo.model.Test;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.List;

public interface StudentResultRepository extends JpaRepository<StudentResult, Long> {
    List<StudentResult> findByTest(Test test);
    List<StudentResult> findByTestId(Long testId);
    List<StudentResult> findByStudent(User student);
    Optional<StudentResult> findByStudentIdAndTestId(Long studentId, Long testId);
    long countByTest(Test test);
    
    @Query("SELECT AVG(r.score) FROM StudentResult r WHERE r.test.id = :testId")
    Double findAverageScoreByTestId(Long testId);

    @Query("SELECT AVG(r.score) FROM StudentResult r WHERE r.student = :student")
    Double findAverageScoreByStudent(User student);

    List<StudentResult> findTop5ByStudentOrderBySubmittedAtDesc(User student);
}
