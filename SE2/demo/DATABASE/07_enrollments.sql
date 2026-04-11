-- ==========================================
-- BẢNG: enrollments
-- Mô tả: Ghi danh sinh viên vào khóa học
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `enrollments`;

CREATE TABLE `enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `course_id` bigint NOT NULL,
  `enrolled_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('PENDING','APPROVED','REJECTED') NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `note` text,
  `processed_at` datetime(6) DEFAULT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `academic_staff_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_course` (`user_id`,`course_id`),
  UNIQUE KEY `uk_enrollment_student_course` (`student_id`,`course_id`),
  KEY `fk_enrollments_courses` (`course_id`),
  KEY `FKh4vhe0oan3i5dbhg6b777mstl` (`academic_staff_id`),
  CONSTRAINT `FK2lha5vwilci2yi3vu5akusx4a` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_enrollments_courses` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_enrollments_users` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FKh4vhe0oan3i5dbhg6b777mstl` FOREIGN KEY (`academic_staff_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ------------------------------------------
-- Dữ liệu mẫu cho enrollments
-- ------------------------------------------
INSERT INTO `enrollments` (`user_id`, `student_id`, `course_id`, `status`) VALUES (3, 3, 1, 'APPROVED');
