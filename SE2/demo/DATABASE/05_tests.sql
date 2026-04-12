-- ==========================================
-- BẢNG: tests
-- Mô tả: Thông tin các bài kiểm tra
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `tests`;

CREATE TABLE `tests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `duration` int DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `course_id` bigint NOT NULL,
  `lesson_id` bigint DEFAULT NULL,
  `assessment_type` enum('COURSE_ASSESSMENT','REMEDIAL_TEST') NOT NULL,
  `code` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `target_lesson_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnn88a30eakyhdu5nt1m5trxit` (`course_id`),
  KEY `FKa9ekvwmlio4eibo0hcwntdjxf` (`lesson_id`),
  KEY `FKme3q644jfvsrl0oagxwsrpvjp` (`target_lesson_id`),
  CONSTRAINT `FKa9ekvwmlio4eibo0hcwntdjxf` FOREIGN KEY (`lesson_id`) REFERENCES `lessons` (`id`),
  CONSTRAINT `FKme3q644jfvsrl0oagxwsrpvjp` FOREIGN KEY (`target_lesson_id`) REFERENCES `lessons` (`id`),
  CONSTRAINT `FKnn88a30eakyhdu5nt1m5trxit` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

