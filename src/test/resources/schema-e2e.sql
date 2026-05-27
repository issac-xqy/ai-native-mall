-- ==========================================
-- E2E 测试数据库初始化 (H2 + 种子数据)
-- ==========================================

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

-- 购物车表
CREATE TABLE IF NOT EXISTS shopping_cart (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT DEFAULT 1,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(50) NOT NULL,
  user_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status TINYINT DEFAULT 0,
  receiver_name VARCHAR(50),
  receiver_phone VARCHAR(20),
  receiver_address VARCHAR(500),
  remark VARCHAR(500),
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单商品表
CREATE TABLE IF NOT EXISTS order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(200),
  product_image VARCHAR(500),
  price DECIMAL(10,2) NOT NULL,
  quantity INT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  deleted TINYINT DEFAULT 0,
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 钱包表
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

-- ==========================================
-- E2E 种子数据
-- ==========================================

-- 管理员 (admin / admin123)
INSERT INTO sys_user (username, password, nickname, phone, email, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'Admin', '13800000000', 'admin@e2e.com', 1),
('e2e_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'E2E测试', '13900000000', 'e2e@test.com', 1);

-- 商品分类
INSERT INTO product_category (name, parent_id, sort_order) VALUES
('手机数码', 0, 1),
('电脑办公', 0, 2);

-- 测试商品
INSERT INTO product (name, category_id, price, original_price, stock, sales, description, status, publish_status) VALUES
('iPhone 15 Pro', 1, 7999.00, 8999.00, 100, 500, 'Apple iPhone 15 Pro，A17 Pro芯片', 1, 1),
('MacBook Pro 14', 2, 14999.00, 16999.00, 50, 200, 'Apple MacBook Pro 14 M3 Pro', 1, 1),
('华为Mate 60 Pro', 1, 6999.00, 7999.00, 80, 800, '华为旗舰手机，卫星通话', 1, 1),
('小米14 Pro', 1, 4999.00, 5499.00, 120, 1200, '小米旗舰，徕卡光学', 1, 1),
('三星Galaxy S24', 1, 5999.00, 6999.00, 60, 600, '三星旗舰，Galaxy AI', 1, 1),
('联想ThinkPad X1', 2, 8999.00, 10999.00, 30, 150, '商务旗舰笔记本', 1, 1),
('戴森V12吸尘器', 1, 3990.00, 4590.00, 90, 900, '无绳吸尘器，激光探测', 1, 1),
('索尼PS5游戏机', 1, 3999.00, 4499.00, 40, 1500, '次世代游戏主机', 1, 1);

-- E2E 用户钱包 (初始余额 5000)
INSERT INTO user_wallet (user_id, balance, total_recharge, total_spent, frozen_amount) VALUES
(2, 5000.00, 5000.00, 0.00, 0.00);
