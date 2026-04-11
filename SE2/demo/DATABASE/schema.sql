-- MySQL dump 10.13  Distrib 9.4.0, for macos15 (arm64)
--
-- Host: localhost    Database: student_management
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_log_entries`
--

DROP TABLE IF EXISTS `audit_log_entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(100) NOT NULL,
  `details` text,
  `entity_id` bigint DEFAULT NULL,
  `entity_type` varchar(100) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `authorities`
--

DROP TABLE IF EXISTS `authorities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `authorities` (
  `username` varchar(100) NOT NULL,
  `authority` varchar(50) NOT NULL,
  PRIMARY KEY (`username`,`authority`),
  CONSTRAINT `fk_authorities_users` FOREIGN KEY (`username`) REFERENCES `users` (`username`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `courses`
--

DROP TABLE IF EXISTS `courses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `end_date` date DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `start_date` date DEFAULT NULL,
  `code` varchar(50) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `status` enum('DRAFT','OPEN','CLOSED','PENDING','IN_PROGRESS','COMPLETED') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_61og8rbqdd2y28rx2et5fdnxd` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
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

--
-- Table structure for table `error_test_mapping`
--

DROP TABLE IF EXISTS `error_test_mapping`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `error_test_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `error_type_id` bigint NOT NULL,
  `test_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKryhytdbabvd6jvsr55uc7u3aw` (`error_type_id`),
  KEY `FKmk3m9sxjajytmmyni3y6wyr37` (`test_id`),
  CONSTRAINT `FKmk3m9sxjajytmmyni3y6wyr37` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`),
  CONSTRAINT `FKryhytdbabvd6jvsr55uc7u3aw` FOREIGN KEY (`error_type_id`) REFERENCES `error_types` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `error_types`
--

DROP TABLE IF EXISTS `error_types`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `error_types` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lesson_quiz_questions`
--

DROP TABLE IF EXISTS `lesson_quiz_questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `lessons`
--

DROP TABLE IF EXISTS `lessons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_errors`
--

DROP TABLE IF EXISTS `student_errors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student_results`
--

DROP TABLE IF EXISTS `student_results`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tests`
--

DROP TABLE IF EXISTS `tests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `role` enum('ADMIN','ACADEMIC_STAFF','STUDENT') NOT NULL,
  `status` varchar(255) NOT NULL,
  `username` varchar(255) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `reset_code_expires_at` datetime(6) DEFAULT NULL,
  `reset_token` varchar(255) DEFAULT NULL,
  `reset_token_expires_at` datetime(6) DEFAULT NULL,
  `reset_verification_code` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_6dotkott2kjsp8vw4d0m25fb7` (`email`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`),
  UNIQUE KEY `UK_kpeyao30ym7l5vf8wsterwase` (`reset_token`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-11 22:09:42
