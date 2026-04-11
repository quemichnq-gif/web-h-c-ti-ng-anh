-- ==========================================
-- BẢNG: users
-- Mô tả: Lưu trữ thông tin tài khoản người dùng
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
    `id`                     BIGINT NOT NULL AUTO_INCREMENT,
    `username`               VARCHAR(255) NOT NULL,
    `password`               VARCHAR(255) NOT NULL,
    `full_name`              VARCHAR(255) DEFAULT NULL,
    `email`                  VARCHAR(255) NOT NULL,
    `phone`                  VARCHAR(255) DEFAULT NULL,
    `role`                   ENUM('ADMIN', 'ACADEMIC_STAFF', 'STUDENT') NOT NULL,
    `status`                 VARCHAR(255) NOT NULL DEFAULT 'ACTIVE',
    `created_at`             DATETIME(6) DEFAULT NULL,
    `reset_token`            VARCHAR(255) DEFAULT NULL,
    `reset_token_expires_at` DATETIME(6) DEFAULT NULL,
    `reset_verification_code` VARCHAR(255) DEFAULT NULL,
    `reset_code_expires_at`  DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`),
    UNIQUE KEY `uk_users_email` (`email`),
    UNIQUE KEY `uk_users_reset_token` (`reset_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------
-- Dữ liệu mẫu cho users
-- ------------------------------------------
-- Mật khẩu mặc định là mã hóa của "maitrang123", "staff123", "student123"
INSERT INTO `users` (`username`, `password`, `full_name`, `email`, `role`, `status`, `created_at`) VALUES
('maitrang', '$2a$10$8.UnVuG9HHgffUDAlk8Kn.2NvEn50XhH6fUu3Kz0lH3p6a.8y8v6.', 'Mai Trang (Admin)', 'maitrang@university.edu', 'ADMIN', 'ACTIVE', NOW()),
('staff', '$2a$10$8.UnVuG9HHgffUDAlk8Kn.2NvEn50XhH6fUu3Kz0lH3p6a.8y8v6.', 'Nguyễn Văn Nhân', 'staff@university.edu', 'ACADEMIC_STAFF', 'ACTIVE', NOW()),
('student1', '$2a$10$8.UnVuG9HHgffUDAlk8Kn.2NvEn50XhH6fUu3Kz0lH3p6a.8y8v6.', 'Trần Anh Tuấn', 'student1@university.edu', 'STUDENT', 'ACTIVE', NOW());