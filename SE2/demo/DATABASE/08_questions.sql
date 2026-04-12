-- ==========================================
-- BẢNG: questions
-- Mô tả: Ngân hàng câu hỏi cho bài test
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `questions`;

CREATE TABLE `questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `test_id` bigint NOT NULL,
  `content` text NOT NULL,
  `option_a` varchar(255) NOT NULL,
  `option_b` varchar(255) NOT NULL,
  `option_c` varchar(255) NOT NULL,
  `option_d` varchar(255) NOT NULL,
  `correct_option` char(1) NOT NULL COMMENT 'A, B, C, or D',
  `audio_content_type` varchar(255) DEFAULT NULL,
  `audio_original_name` varchar(255) DEFAULT NULL,
  `audio_stored_name` varchar(255) DEFAULT NULL,
  `correct_answer` varchar(255) NOT NULL,
  `image_content_type` varchar(255) DEFAULT NULL,
  `image_original_name` varchar(255) DEFAULT NULL,
  `image_stored_name` varchar(255) DEFAULT NULL,
  `question_type` enum('SHORT_ANSWER','MULTIPLE_CHOICE') NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_questions_tests` (`test_id`),
  CONSTRAINT `fk_questions_tests` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------
-- Dữ liệu mẫu cho questions
-- ------------------------------------------
INSERT INTO `questions` (`test_id`, `content`, `option_a`, `option_b`, `option_c`, `option_d`, `correct_answer`, `question_type`) VALUES (1, 'Which is a noun?', 'Go', 'Eat', 'Apple', 'Fast', 'Apple', 'MULTIPLE_CHOICE');
