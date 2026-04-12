-- ==========================================
-- BẢNG: LESSON_QUIZ_QUESTIONS
-- Mô tả: Câu hỏi trắc nghiệm nhanh đính kèm bài học
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `lesson_quiz_questions`;

CREATE TABLE `lesson_quiz_questions` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT,
    `lesson_id`      BIGINT NOT NULL,
    `question_text`  TEXT NOT NULL,
    `question_type`  ENUM('SHORT_ANSWER', 'MULTIPLE_CHOICE') NOT NULL,
    `option_a`       VARCHAR(255) DEFAULT NULL,
    `option_b`       VARCHAR(255) DEFAULT NULL,
    `option_c`       VARCHAR(255) DEFAULT NULL,
    `option_d`       VARCHAR(255) DEFAULT NULL,
    `correct_answer` VARCHAR(255) NOT NULL,
    `explanation`    TEXT,
    `bloom_level`    ENUM('REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE') NOT NULL,
    `sort_order`     INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_quiz_lesson` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
