-- ==========================================
-- BẢNG: ERROR_TEST_MAPPING
-- Mô tả: Ánh xạ lỗi với các bài thi bổ trợ tương ứng
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `error_test_mapping`;

CREATE TABLE `error_test_mapping` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `error_type_id` BIGINT NOT NULL,
    `test_id`       BIGINT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_mapping_type` FOREIGN KEY (`error_type_id`) REFERENCES `error_types` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mapping_test` FOREIGN KEY (`test_id`) REFERENCES `tests` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
