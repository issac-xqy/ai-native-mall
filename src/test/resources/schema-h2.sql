-- ==========================================
-- H2 测试用建表脚本（MySQL 兼容模式）
-- ==========================================

-- 商品表
CREATE TABLE IF NOT EXISTS product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL,
  category_id BIGINT NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  original_price DECIMAL(10,2),
  stock INT DEFAULT 0,
  sales INT DEFAULT 0,
  image VARCHAR(500),
  images TEXT,
  description TEXT,
  specs TEXT,
  seo_title VARCHAR(300),
  ai_description TEXT,
  sentiment_score DECIMAL(3,2) DEFAULT 5.00,
  view_count INT DEFAULT 0,
  click_count INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  publish_status TINYINT DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  nickname VARCHAR(50),
  phone VARCHAR(20),
  email VARCHAR(100),
  avatar VARCHAR(255),
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 商品分类表
CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  parent_id BIGINT DEFAULT 0,
  sort_order INT DEFAULT 0,
  icon VARCHAR(255),
  status TINYINT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 商品评论表
CREATE TABLE IF NOT EXISTS product_comment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  sentiment VARCHAR(20),
  ai_tags VARCHAR(500),
  summary TEXT,
  rating INT DEFAULT 5,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户钱包表
CREATE TABLE IF NOT EXISTS user_wallet (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  balance DECIMAL(10,2) DEFAULT 0.00,
  total_recharge DECIMAL(10,2) DEFAULT 0.00,
  total_spent DECIMAL(10,2) DEFAULT 0.00,
  frozen_amount DECIMAL(10,2) DEFAULT 0.00,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 充值记录表
CREATE TABLE IF NOT EXISTS recharge_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  wallet_id BIGINT,
  amount DECIMAL(10,2) NOT NULL,
  recharge_type INT DEFAULT 1,
  status INT DEFAULT 0,
  trade_no VARCHAR(100),
  out_trade_no VARCHAR(100),
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 收货地址表
CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  receiver_name VARCHAR(50) NOT NULL,
  receiver_phone VARCHAR(20) NOT NULL,
  province VARCHAR(50),
  city VARCHAR(50),
  district VARCHAR(50),
  detail_address VARCHAR(500) NOT NULL,
  is_default TINYINT DEFAULT 0,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI知识库文档表
CREATE TABLE IF NOT EXISTS ai_knowledge_document (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doc_name VARCHAR(200) NOT NULL,
  doc_type VARCHAR(20),
  file_path VARCHAR(500) NOT NULL,
  content TEXT,
  category VARCHAR(50),
  status TINYINT DEFAULT 0,
  vector_ids TEXT,
  file_size BIGINT DEFAULT 0,
  upload_user_id BIGINT,
  upload_user_name VARCHAR(100),
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
