-- ==========================================
-- BẢNG: AUDIT_LOG_ENTRIES
-- Mô tả: Nhật ký hoạt động hệ thống
-- ==========================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `audit_log_entries`;

CREATE TABLE `audit_log_entries` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `action`      VARCHAR(100) NOT NULL,
    `details`     TEXT,
    `entity_id`   BIGINT DEFAULT NULL,
    `entity_type` VARCHAR(100) NOT NULL,
    `timestamp`   DATETIME(6) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
