-- ==========================================
-- BẢNG: STUDENT_RESULTS
-- Mô tả: Lưu kết quả bài thi của sinh viên
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `student_results`;

CREATE TABLE `student_results` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT,
    `student_id`          BIGINT NOT NULL,
    `test_id`             BIGINT NOT NULL,
    `score`               DOUBLE NOT NULL,
    `submitted_at`        DATETIME(6) DEFAULT NULL,
    `answer_details_json` LONGTEXT,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_results_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_results_test` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
