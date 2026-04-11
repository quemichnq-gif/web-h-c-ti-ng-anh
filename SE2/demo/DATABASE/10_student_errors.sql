-- ==========================================
-- BẢNG: student_errors
-- Mô tả: Thống kê các lỗi sinh viên thường mắc
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `student_errors`;

CREATE TABLE `student_errors` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `error_type_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKd1obj998ct29frdkh49v00n8j` (`error_type_id`),
  KEY `FKreiie0glfnqm3wvce0pumkhl5` (`student_id`),
  CONSTRAINT `FKd1obj998ct29frdkh49v00n8j` FOREIGN KEY (`error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FKreiie0glfnqm3wvce0pumkhl5` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
