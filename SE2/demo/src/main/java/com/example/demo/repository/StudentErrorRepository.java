package com.example.demo.repository;

import com.example.demo.model.StudentError;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface StudentErrorRepository extends JpaRepository<StudentError, Long> {
    List<StudentError> findByStudent(User student);
    List<StudentError> findByStudentId(Long studentId);
    List<StudentError> findByErrorTypeId(Long errorTypeId);
    boolean existsByStudentIdAndErrorTypeId(Long studentId, Long errorTypeId);
    List<StudentError> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
    
    @Query("SELECT COUNT(DISTINCT se.student) FROM StudentError se WHERE se.errorType.id NOT IN " +
           "(SELECT etm.errorType.id FROM ErrorTestMapping etm)")
    long countStudentsWithUnassignedErrors();
    
    @Query("SELECT se FROM StudentError se ORDER BY se.createdAt DESC")
    List<StudentError> findAllOrderByCreatedAtDesc();
}
