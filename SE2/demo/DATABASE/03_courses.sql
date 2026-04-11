SET FOREIGN_KEY_CHECKS = 0;

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