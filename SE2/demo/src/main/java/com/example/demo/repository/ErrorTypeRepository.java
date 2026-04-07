package com.example.demo.repository;

import com.example.demo.model.ErrorType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ErrorTypeRepository extends JpaRepository<ErrorType, Long> {
    Optional<ErrorType> findByNameIgnoreCase(String name);
}
