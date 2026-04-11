-- ==========================================
-- BẢNG: student_results
-- Mô tả: Kết quả làm bài của sinh viên
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `student_results`;

CREATE TABLE `student_results` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `test_id` bigint NOT NULL,
  `score` double NOT NULL,
  `completed_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `answer_details_json` longtext,
  `submitted_at` datetime(6) DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_results_users` (`user_id`),
  KEY `fk_results_tests` (`test_id`),
  KEY `FKabvjlmo3ke91lnenkg4r0908h` (`student_id`),
  CONSTRAINT `fk_results_tests` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_results_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKabvjlmo3ke91lnenkg4r0908h` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
