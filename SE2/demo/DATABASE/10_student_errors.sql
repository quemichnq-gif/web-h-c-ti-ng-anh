-- ==========================================
-- BẢNG: STUDENT_ERRORS
-- Mô tả: Thống kê các loại lỗi sinh viên mắc phải
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `student_errors`;

CREATE TABLE `student_errors` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `student_id`    BIGINT NOT NULL,
    `error_type_id` BIGINT NOT NULL,
    `created_at`    DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_errors_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_errors_type` FOREIGN KEY (`error_type_id`) REFERENCES `error_types` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
