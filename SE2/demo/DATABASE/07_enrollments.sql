SET FOREIGN_KEY_CHECKS = 0;

-- Table structure for table `enrollments`
--

DROP TABLE IF EXISTS `enrollments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;