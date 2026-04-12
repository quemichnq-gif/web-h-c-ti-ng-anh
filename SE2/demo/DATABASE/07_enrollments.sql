-- ==========================================
-- BẢNG: enrollments
-- Mô tả: Ghi danh sinh viên vào khóa học
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `enrollments`;

CREATE TABLE `enrollments` (
    `id`                BIGINT NOT NULL AUTO_INCREMENT,
    `student_id`        BIGINT NOT NULL,
    `course_id`         BIGINT NOT NULL,
    `academic_staff_id` BIGINT DEFAULT NULL,
    `status`            ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    `enrolled_at`       DATETIME(6) DEFAULT NULL,
    `reject_reason`     VARCHAR(255) DEFAULT NULL,
    `note`              TEXT,
    `created_at`        DATETIME(6) DEFAULT NULL,
    `updated_at`        DATETIME(6) DEFAULT NULL,
    `processed_at`      DATETIME(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_enrollment_student_course` (`student_id`, `course_id`),
    CONSTRAINT `fk_enrollments_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_enrollments_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_enrollments_staff` FOREIGN KEY (`academic_staff_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

