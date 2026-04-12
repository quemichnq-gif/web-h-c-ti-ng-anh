-- ==========================================
-- BẢNG: lessons
-- Mô tả: Thông tin các bài học trong khóa
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `lessons`;

CREATE TABLE `lessons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `_order` int DEFAULT NULL,
  `content` text,
  `title` varchar(255) NOT NULL,
  `course_id` bigint NOT NULL,
  `attachment_content_type` varchar(255) DEFAULT NULL,
  `attachment_original_name` varchar(255) DEFAULT NULL,
  `attachment_stored_name` varchar(255) DEFAULT NULL,
  `code` varchar(50) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `image_content_type` varchar(255) DEFAULT NULL,
  `image_original_name` varchar(255) DEFAULT NULL,
  `image_stored_name` varchar(255) DEFAULT NULL,
  `sort_order` int NOT NULL,
  `status` enum('DRAFT','PUBLISHED','ARCHIVED') DEFAULT NULL,
  `summary` varchar(500) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `video_url` varchar(255) DEFAULT NULL,
  `analyze_error_type_id` bigint DEFAULT NULL,
  `apply_error_type_id` bigint DEFAULT NULL,
  `create_error_type_id` bigint DEFAULT NULL,
  `error_type_id` bigint DEFAULT NULL,
  `evaluate_error_type_id` bigint DEFAULT NULL,
  `remember_error_type_id` bigint DEFAULT NULL,
  `test_id` bigint DEFAULT NULL,
  `understand_error_type_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lessons_code` (`code`),
  UNIQUE KEY `UK_frc6jfossj0n55kk0qfdceixf` (`test_id`),
  KEY `FK17ucc7gjfjddsyi0gvstkqeat` (`course_id`),
  KEY `FKguy4x28sjr4p9b9jjvls0ih29` (`analyze_error_type_id`),
  KEY `FK43oabme2o3paw4ybj0up8ft05` (`apply_error_type_id`),
  KEY `FKhilsd00rot6l7nt19dbtn6p81` (`create_error_type_id`),
  KEY `FK7m3bokvqacshfwksy723aluc7` (`error_type_id`),
  KEY `FKmbjqdqbkc08wy7i1x4mypghv` (`evaluate_error_type_id`),
  KEY `FKt059foamaa8c8g0mknc9n65xu` (`remember_error_type_id`),
  KEY `FK1ptvp1j3ankd9xt4oxe0l77ms` (`understand_error_type_id`),
  CONSTRAINT `FK17ucc7gjfjddsyi0gvstkqeat` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `FK1ptvp1j3ankd9xt4oxe0l77ms` FOREIGN KEY (`understand_error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FK43oabme2o3paw4ybj0up8ft05` FOREIGN KEY (`apply_error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FK7m3bokvqacshfwksy723aluc7` FOREIGN KEY (`error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FK9g5qq7x9tw8we7hgvl17xlowq` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`),
  CONSTRAINT `FKguy4x28sjr4p9b9jjvls0ih29` FOREIGN KEY (`analyze_error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FKhilsd00rot6l7nt19dbtn6p81` FOREIGN KEY (`create_error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FKmbjqdqbkc08wy7i1x4mypghv` FOREIGN KEY (`evaluate_error_type_id`) REFERENCES `error_types` (`id`),
  CONSTRAINT `FKt059foamaa8c8g0mknc9n65xu` FOREIGN KEY (`remember_error_type_id`) REFERENCES `error_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

