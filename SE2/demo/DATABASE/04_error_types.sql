-- ==========================================
-- BẢNG: error_types
-- Mô tả: Loại lỗi học tập (Grammar, Vocab...)
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `error_types`;

CREATE TABLE `error_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

