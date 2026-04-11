SET FOREIGN_KEY_CHECKS = 0;

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