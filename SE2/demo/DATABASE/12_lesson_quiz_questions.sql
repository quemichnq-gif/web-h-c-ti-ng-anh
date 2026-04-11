-- ==========================================
-- BẢNG: lesson_quiz_questions
-- Mô tả: Câu hỏi trắc nghiệm nhanh trong bài học
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `lesson_quiz_questions`;

CREATE TABLE `lesson_quiz_questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bloom_level` enum('REMEMBER','UNDERSTAND','APPLY','ANALYZE','EVALUATE','CREATE') NOT NULL,
  `correct_answer` varchar(255) NOT NULL,
  `explanation` text,
  `option_a` varchar(255) DEFAULT NULL,
  `option_b` varchar(255) DEFAULT NULL,
  `option_c` varchar(255) DEFAULT NULL,
  `option_d` varchar(255) DEFAULT NULL,
  `question_text` text NOT NULL,
  `question_type` enum('SHORT_ANSWER','MULTIPLE_CHOICE') NOT NULL,
  `sort_order` int NOT NULL,
  `lesson_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkjt0eh6g5jt0uhvl3b0olvdfq` (`lesson_id`),
  CONSTRAINT `FKkjt0eh6g5jt0uhvl3b0olvdfq` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
