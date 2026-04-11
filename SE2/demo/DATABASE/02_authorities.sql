-- ==========================================
-- BẢNG: authorities
-- Mô tả: Phân quyền cho người dùng (Spring Security)
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `authorities`;

CREATE TABLE `authorities` (
    `username`  VARCHAR(100) NOT NULL,
    `authority` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`username`, `authority`),
    CONSTRAINT `fk_authorities_users` 
        FOREIGN KEY (`username`) REFERENCES `users` (`username`) 
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------
-- Dữ liệu mẫu cho authorities
-- ------------------------------------------
INSERT INTO `authorities` (`username`, `authority`) VALUES
('maitrang', 'ROLE_ADMIN'),
('staff',    'ROLE_ACADEMIC_STAFF'),
('student1', 'ROLE_STUDENT');