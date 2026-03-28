package com.example.demo.repository;

import com.example.demo.model.ErrorType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorTypeRepository extends JpaRepository<ErrorType, Long> {
}
