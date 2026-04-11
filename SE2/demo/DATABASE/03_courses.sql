-- ==========================================
-- BẢNG: courses
-- Mô tả: Thông tin về các khóa học tiếng Anh
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `courses`;

CREATE TABLE `courses` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(50) NOT NULL,
    `name`        VARCHAR(255) NOT NULL,
    `description` TEXT,
    `status`      ENUM('DRAFT', 'OPEN', 'CLOSED', 'PENDING', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'DRAFT',
    `start_date`  DATE DEFAULT NULL,
    `end_date`    DATE DEFAULT NULL,
    `created_at`  DATETIME(6) DEFAULT NULL,
    `updated_at`  DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_courses_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------
-- Dữ liệu mẫu cho courses
-- ------------------------------------------
INSERT INTO `courses` (`code`, `name`, `description`, `status`, `start_date`, `end_date`, `created_at`) VALUES
('IELTS-W-T2', 'IELTS Academic Writing Task 2', 'Học cách viết luận Academic IELTS đạt band 7.0+', 'OPEN', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 3 MONTH), NOW()),
('BIZ-ENG',    'Communicative Business English', 'Tiếng Anh giao tiếp chuyên sâu cho môi trường văn phòng.', 'OPEN', CURDATE(), DATE_ADD(CURDATE(), INTERVAL 6 MONTH), NOW());