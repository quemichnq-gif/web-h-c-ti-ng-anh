package com.example.demo.repository;

import com.example.demo.model.User;
import com.example.demo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);
    List<User> findByRole(Role role);
    long countByRole(Role role);
    long countByStatus(String status);
}
