-- ==========================================
-- 用户钱包和充值记录表
-- ==========================================

USE ai_mall;

-- 用户钱包表
CREATE TABLE IF NOT EXISTS `user_wallet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `balance` DECIMAL(10,2) DEFAULT 0.00,
  `total_recharge` DECIMAL(10,2) DEFAULT 0.00,
  `total_spent` DECIMAL(10,2) DEFAULT 0.00,
  `frozen_amount` DECIMAL(10,2) DEFAULT 0.00,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  KEY `idx_balance` (`balance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user_wallet';

-- 充值记录表
CREATE TABLE IF NOT EXISTS `recharge_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `wallet_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `recharge_type` TINYINT DEFAULT 1,
  `status` TINYINT DEFAULT 0,
  `trade_no` VARCHAR(100) DEFAULT NULL,
  `out_trade_no` VARCHAR(100) DEFAULT NULL,
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trade_no` (`trade_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='recharge_record';

-- 为现有用户创建钱包
INSERT INTO `user_wallet` (`user_id`, `balance`, `total_recharge`, `total_spent`, `frozen_amount`)
SELECT id, 0.00, 0.00, 0.00, 0.00
FROM sys_user
WHERE id NOT IN (SELECT user_id FROM user_wallet)
  AND deleted = 0;

-- 验证数据
SELECT 'user_wallet' as table_name, COUNT(*) as count FROM user_wallet
UNION ALL
SELECT 'recharge_record', COUNT(*) FROM recharge_record;
