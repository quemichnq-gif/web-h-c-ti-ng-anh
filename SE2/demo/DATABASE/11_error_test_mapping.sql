SET FOREIGN_KEY_CHECKS = 0;

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