-- ==========================================
-- AI-Native智能商城 - 数据库初始化脚本
-- 版本: v2.0
-- 日期: 2026-04-10
-- 编码: UTF-8
-- ==========================================

CREATE DATABASE IF NOT EXISTS ai_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_mall;

-- ==========================================
-- 1. 用户表
-- ==========================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ==========================================
-- 2. 商品分类表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ==========================================
-- 3. 商品表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  `stock` INT DEFAULT 0 COMMENT '库存',
  `sales` INT DEFAULT 0 COMMENT '销量',
  `image` VARCHAR(500) DEFAULT NULL COMMENT '主图URL',
  `images` TEXT COMMENT '图片列表（JSON）',
  `description` TEXT COMMENT '商品描述',
  `specs` TEXT COMMENT '规格参数（JSON）',
  `seo_title` VARCHAR(300) DEFAULT NULL COMMENT 'AI生成的SEO标题',
  `ai_description` TEXT COMMENT 'AI生成的营销文案',
  `sentiment_score` DECIMAL(3,2) DEFAULT 5.00 COMMENT '情感评分（1-5）',
  `view_count` INT DEFAULT 0 COMMENT '浏览量',
  `click_count` INT DEFAULT 0 COMMENT '点击量',
  `status` TINYINT DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
  `publish_status` TINYINT DEFAULT 0 COMMENT '发布状态: 0-草稿 1-已上架 2-已下架',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`(50))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ==========================================
-- 4. 商品评论表
-- ==========================================
CREATE TABLE IF NOT EXISTS `product_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `sentiment` VARCHAR(20) DEFAULT NULL COMMENT '情感倾向: positive/negative/neutral',
  `ai_tags` VARCHAR(500) DEFAULT NULL COMMENT 'AI提取的标签（逗号分隔）',
  `summary` TEXT COMMENT 'AI分析总结',
  `rating` INT DEFAULT 5 COMMENT '评分（1-5）',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sentiment` (`sentiment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评论表';

-- ==========================================
-- 5. 购物车表
-- ==========================================
CREATE TABLE IF NOT EXISTS `shopping_cart` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT DEFAULT 1 COMMENT '数量',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ==========================================
-- 6. 订单表
-- ==========================================
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `status` TINYINT DEFAULT 0 COMMENT '订单状态: 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
  `receiver_address` VARCHAR(500) DEFAULT NULL COMMENT '收货地址',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ==========================================
-- 7. 订单商品表
-- ==========================================
CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单商品ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(200) DEFAULT NULL COMMENT '商品名称（快照）',
  `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品图片（快照）',
  `price` DECIMAL(10,2) NOT NULL COMMENT '商品单价',
  `quantity` INT NOT NULL COMMENT '购买数量',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '商品总价',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单商品表';

-- ==========================================
-- 8. AI对话记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS `ai_chat_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `session_id` VARCHAR(100) DEFAULT NULL COMMENT '会话ID',
  `question` TEXT NOT NULL COMMENT '用户问题',
  `answer` TEXT COMMENT 'AI回复',
  `tokens_used` INT DEFAULT 0 COMMENT '消耗的Token数',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话记录表';

-- ==========================================
-- 9. 收货地址表
-- ==========================================
CREATE TABLE IF NOT EXISTS `user_address` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `receiver_name` VARCHAR(50) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) NOT NULL COMMENT '收货人手机号',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省份',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区县',
  `detail_address` VARCHAR(500) NOT NULL COMMENT '详细地址',
  `is_default` TINYINT DEFAULT 0 COMMENT '是否默认: 0-否 1-是',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ==========================================
-- 10. AI知识库文档表
-- ==========================================
CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `doc_name` VARCHAR(200) NOT NULL COMMENT '文档名称',
  `doc_type` VARCHAR(20) DEFAULT NULL COMMENT '文档类型: pdf/word/txt/markdown',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  `content` LONGTEXT COMMENT '文档内容（纯文本）',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类标签: product/policy/faq/custom',
  `status` TINYINT DEFAULT 0 COMMENT '状态: 0-未处理 1-已向量化 2-向量化失败',
  `vector_ids` TEXT COMMENT '向量ID列表（JSON数组）',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
  `upload_user_id` BIGINT DEFAULT NULL COMMENT '上传用户ID',
  `upload_user_name` VARCHAR(100) DEFAULT NULL COMMENT '上传用户名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档表';

-- ==========================================
-- 初始数据
-- ==========================================

-- 初始管理员账号（密码: admin123，BCrypt加密）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `phone`, `email`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', '13800138000', 'admin@example.com', 1),
('test_user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', '13900139000', 'test@example.com', 1);

-- 商品分类
INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`) VALUES
('手机数码', 0, 1),
('电脑办公', 0, 2),
('家用电器', 0, 3),
('服装鞋帽', 0, 4),
('食品饮料', 0, 5);

-- ==========================================
-- 100+ 商品数据（无重复、无乱码）
-- ==========================================

-- 分类1：手机数码（25个）
INSERT INTO `product` (`name`, `category_id`, `price`, `original_price`, `stock`, `sales`, `description`, `specs`, `status`) VALUES
('iPhone 15 Pro Max', 1, 9999.00, 10999.00, 80, 1250, 'Apple iPhone 15 Pro Max，A17 Pro芯片，钛金属边框，4800万像素三摄系统', '{"屏幕": "6.7英寸 OLED", "处理器": "A17 Pro", "内存": "8GB", "存储": "256GB", "摄像头": "4800万主摄+1200万超广角+1200万长焦"}', 1),
('iPhone 15', 1, 5999.00, 6999.00, 150, 2300, 'Apple iPhone 15，A16芯片，灵动岛设计，4800万像素主摄', '{"屏幕": "6.1英寸 OLED", "处理器": "A16", "内存": "6GB", "存储": "128GB", "摄像头": "4800万主摄+1200万超广角"}', 1),
('三星Galaxy S24 Ultra', 1, 9699.00, 10999.00, 60, 890, '三星Galaxy S24 Ultra，骁龙8 Gen 3，S Pen手写笔，2亿像素', '{"屏幕": "6.8英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "2亿主摄+5000万长焦+1000万长焦+1200万超广角"}', 1),
('三星Galaxy S24', 1, 5999.00, 6999.00, 100, 1100, '三星Galaxy S24，骁龙8 Gen 3，Galaxy AI智能功能', '{"屏幕": "6.2英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "8GB", "存储": "128GB", "摄像头": "5000万主摄+1200万超广角+1000万长焦"}', 1),
('华为Mate 60 Pro', 1, 6999.00, 7999.00, 50, 3500, '华为Mate 60 Pro，麒麟9000S芯片，卫星通话，超可靠玄武架构', '{"屏幕": "6.82英寸 OLED", "处理器": "麒麟9000S", "内存": "12GB", "存储": "256GB", "摄像头": "4800万主摄+1200万超广角+4800万长焦"}', 1),
('华为Pura 70 Pro', 1, 6499.00, 6999.00, 70, 1800, '华为Pura 70 Pro，麒麟9010芯片，XMAGE影像，超聚光伸缩摄像头', '{"屏幕": "6.7英寸 OLED", "处理器": "麒麟9010", "内存": "12GB", "存储": "256GB", "摄像头": "5000万超聚光主摄+4000万超广角+4800万长焦"}', 1),
('小米14 Pro', 1, 4999.00, 5499.00, 120, 2800, '小米14 Pro，骁龙8 Gen 3，徕卡光学镜头，120W快充', '{"屏幕": "6.73英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+5000万长焦"}', 1),
('小米14', 1, 3999.00, 4299.00, 200, 4200, '小米14，骁龙8 Gen 3，徕卡光学，小巧旗舰', '{"屏幕": "6.36英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+5000万长焦"}', 1),
('OPPO Find X7 Ultra', 1, 5999.00, 6499.00, 80, 950, 'OPPO Find X7 Ultra，天玑9300，哈苏影像，双潜望长焦', '{"屏幕": "6.82英寸 AMOLED", "处理器": "天玑9300", "内存": "16GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+5000万潜望长焦+5000万潜望长焦"}', 1),
('vivo X100 Pro', 1, 4999.00, 5499.00, 90, 1600, 'vivo X100 Pro，天玑9300，蔡司APO超级长焦，蓝海电池', '{"屏幕": "6.78英寸 AMOLED", "处理器": "天玑9300", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+5000万长焦"}', 1),
('荣耀Magic6 Pro', 1, 5699.00, 6199.00, 70, 780, '荣耀Magic6 Pro，骁龙8 Gen 3，鸿燕通信，荣耀绿洲护眼屏', '{"屏幕": "6.78英寸 OLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+1.8亿长焦"}', 1),
('一加12', 1, 4299.00, 4799.00, 100, 1300, '一加12，骁龙8 Gen 3，2K东方屏，100W快充', '{"屏幕": "6.82英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+4800万超广角+6400万长焦"}', 1),
('Redmi K70 Pro', 1, 3299.00, 3699.00, 250, 5600, 'Redmi K70 Pro，骁龙8 Gen 3，2K高光屏，120W快充', '{"屏幕": "6.67英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+800万超广角+200万微距"}', 1),
('Redmi K70', 1, 2499.00, 2799.00, 300, 7800, 'Redmi K70，骁龙8 Gen 2，2K高光屏，性能旗舰', '{"屏幕": "6.67英寸 AMOLED", "处理器": "骁龙8 Gen 2", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+800万超广角+200万微距"}', 1),
('真我GT5 Pro', 1, 3399.00, 3799.00, 120, 980, '真我GT5 Pro，骁龙8 Gen 3，潜望长焦，144Hz电竞屏', '{"屏幕": "6.78英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+800万超广角+5000万长焦"}', 1),
('魅族21 Pro', 1, 4999.00, 5499.00, 40, 320, '魅族21 Pro，骁龙8 Gen 3，无界设计，mBack交互', '{"屏幕": "6.79英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+1300万超广角+1000万长焦"}', 1),
('iQOO 12 Pro', 1, 4999.00, 5499.00, 80, 670, 'iQOO 12 Pro，骁龙8 Gen 3，自研电竞芯片Q1，144Hz直屏', '{"屏幕": "6.78英寸 AMOLED", "处理器": "骁龙8 Gen 3", "内存": "16GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+6400万长焦"}', 1),
('Redmi Note 13 Pro+', 1, 1899.00, 2199.00, 400, 12000, 'Redmi Note 13 Pro+，天玑7200 Ultra，2亿像素，IP68防水', '{"屏幕": "6.67英寸 AMOLED", "处理器": "天玑7200 Ultra", "内存": "12GB", "存储": "256GB", "摄像头": "2亿主摄+800万超广角+200万微距"}', 1),
('荣耀100 Pro', 1, 3399.00, 3799.00, 150, 2100, '荣耀100 Pro，骁龙8 Gen 2，雅顾光影人像，100W快充', '{"屏幕": "6.78英寸 AMOLED", "处理器": "骁龙8 Gen 2", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+3200万长焦"}', 1),
('vivo S18 Pro', 1, 3299.00, 3699.00, 130, 1900, 'vivo S18 Pro，天玑9200+，蔡司人像镜头，东方美学设计', '{"屏幕": "6.78英寸 AMOLED", "处理器": "天玑9200+", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+5000万超广角+5000万长焦"}', 1),
('OPPO Reno11 Pro', 1, 3499.00, 3999.00, 110, 1500, 'OPPO Reno11 Pro，天玑8200，超光影长焦人像', '{"屏幕": "6.74英寸 AMOLED", "处理器": "天玑8200", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+800万超广角+3200万长焦"}', 1),
('华为nova 12 Pro', 1, 4699.00, 4999.00, 60, 890, '华为nova 12 Pro，麒麟8000，前置6000万人像双摄', '{"屏幕": "6.7英寸 OLED", "处理器": "麒麟8000", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+800万超广角+6000万前置双摄"}', 1),
('三星Galaxy Z Fold5', 1, 12999.00, 13999.00, 30, 450, '三星Galaxy Z Fold5，骁龙8 Gen 2，7.6英寸大屏，多任务处理', '{"屏幕": "7.6英寸 AMOLED(展开)/6.2英寸(折叠)", "处理器": "骁龙8 Gen 2", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+1200万超广角+1000万长焦"}', 1),
('三星Galaxy Z Flip5', 1, 7499.00, 8999.00, 50, 680, '三星Galaxy Z Flip5，骁龙8 Gen 2，时尚小折叠，3.4英寸外屏', '{"屏幕": "6.7英寸 AMOLED(展开)/3.4英寸(折叠)", "处理器": "骁龙8 Gen 2", "内存": "8GB", "存储": "256GB", "摄像头": "1200万主摄+1200万超广角"}', 1),
('华为Mate X5', 1, 12999.00, 13999.00, 20, 320, '华为Mate X5，麒麟9000S，超可靠玄武架构，双向北斗卫星消息', '{"屏幕": "7.85英寸 OLED(展开)/6.4英寸(折叠)", "处理器": "麒麟9000S", "内存": "12GB", "存储": "256GB", "摄像头": "5000万主摄+1300万超广角+1200万长焦"}', 1);

-- 分类2：电脑办公（25个）
INSERT INTO `product` (`name`, `category_id`, `price`, `original_price`, `stock`, `sales`, `description`, `specs`, `status`) VALUES
('MacBook Pro 14英寸 M3 Pro', 2, 16999.00, 18999.00, 40, 580, 'Apple MacBook Pro 14英寸，M3 Pro芯片，18GB内存，512GB存储', '{"屏幕": "14.2英寸 Liquid Retina XDR", "芯片": "M3 Pro", "内存": "18GB", "存储": "512GB SSD", "电池": "最长17小时"}', 1),
('MacBook Pro 16英寸 M3 Max', 2, 27999.00, 29999.00, 20, 230, 'Apple MacBook Pro 16英寸，M3 Max芯片，36GB内存，1TB存储', '{"屏幕": "16.2英寸 Liquid Retina XDR", "芯片": "M3 Max", "内存": "36GB", "存储": "1TB SSD", "电池": "最长22小时"}', 1),
('MacBook Air 13英寸 M3', 2, 8999.00, 9999.00, 80, 1800, 'Apple MacBook Air 13英寸，M3芯片，8GB内存，256GB存储，轻薄便携', '{"屏幕": "13.6英寸 Liquid Retina", "芯片": "M3", "内存": "8GB", "存储": "256GB SSD", "重量": "1.24kg"}', 1),
('MacBook Air 15英寸 M3', 2, 10499.00, 11499.00, 50, 920, 'Apple MacBook Air 15英寸，M3芯片，8GB内存，256GB存储', '{"屏幕": "15.3英寸 Liquid Retina", "芯片": "M3", "内存": "8GB", "存储": "256GB SSD", "重量": "1.51kg"}', 1),
('ThinkPad X1 Carbon Gen 11', 2, 12999.00, 14999.00, 30, 450, '联想ThinkPad X1 Carbon，13代酷睿i7，14英寸2.8K OLED屏', '{"屏幕": "14英寸 2.8K OLED", "处理器": "i7-1365U", "内存": "16GB", "存储": "512GB SSD", "重量": "1.12kg"}', 1),
('ThinkPad T14s Gen 4', 2, 8999.00, 9999.00, 60, 780, '联想ThinkPad T14s，13代酷睿i5，14英寸2.2K屏，商务办公', '{"屏幕": "14英寸 2.2K IPS", "处理器": "i5-1340P", "内存": "16GB", "存储": "512GB SSD", "重量": "1.22kg"}', 1),
('戴尔XPS 13 Plus', 2, 9999.00, 11999.00, 40, 560, '戴尔XPS 13 Plus，13代酷睿i7，13.4英寸3.5K OLED触控屏', '{"屏幕": "13.4英寸 3.5K OLED", "处理器": "i7-1360P", "内存": "16GB", "存储": "512GB SSD", "重量": "1.23kg"}', 1),
('戴尔XPS 15', 2, 13999.00, 15999.00, 25, 340, '戴尔XPS 15，13代酷睿i7，15.6英寸3.5K OLED屏，RTX 4060', '{"屏幕": "15.6英寸 3.5K OLED", "处理器": "i7-13700H", "内存": "16GB", "存储": "512GB SSD", "显卡": "RTX 4060"}', 1),
('华为MateBook X Pro 2024', 2, 11999.00, 12999.00, 35, 670, '华为MateBook X Pro，13代酷睿i7，14.2英寸3.1K触控屏', '{"屏幕": "14.2英寸 3.1K LTPS", "处理器": "i7-1360P", "内存": "16GB", "存储": "1TB SSD", "重量": "1.26kg"}', 1),
('华为MateBook 14s 2024', 2, 6999.00, 7999.00, 70, 1200, '华为MateBook 14s，13代酷睿i5，14.2英寸2.5K触控屏', '{"屏幕": "14.2英寸 2.5K IPS", "处理器": "i5-13500H", "内存": "16GB", "存储": "512GB SSD", "重量": "1.43kg"}', 1),
('小米笔记本Pro 14 2024', 2, 5999.00, 6499.00, 90, 1500, '小米笔记本Pro 14，13代酷睿i5，14英寸2.8K OLED屏', '{"屏幕": "14英寸 2.8K OLED", "处理器": "i5-13500H", "内存": "16GB", "存储": "512GB SSD", "重量": "1.5kg"}', 1),
('荣耀MagicBook Pro 16', 2, 6499.00, 6999.00, 60, 890, '荣耀MagicBook Pro 16，13代酷睿i5，16英寸2.5K护眼屏', '{"屏幕": "16英寸 2.5K IPS", "处理器": "i5-13500H", "内存": "16GB", "存储": "512GB SSD", "重量": "1.79kg"}', 1),
('惠普战66 七代', 2, 4999.00, 5499.00, 100, 2100, '惠普战66七代，13代酷睿i5，14英寸2.5K高色域屏，商务本', '{"屏幕": "14英寸 2.5K IPS", "处理器": "i5-1340P", "内存": "16GB", "存储": "512GB SSD", "重量": "1.4kg"}', 1),
('惠普暗影精灵9', 2, 7999.00, 8999.00, 50, 1300, '惠普暗影精灵9，13代酷睿i7，16.1英寸2.5K 165Hz电竞屏', '{"屏幕": "16.1英寸 2.5K 165Hz", "处理器": "i7-13700HX", "内存": "16GB", "存储": "512GB SSD", "显卡": "RTX 4060"}', 1),
('联想拯救者Y9000P 2024', 2, 9999.00, 10999.00, 40, 1800, '联想拯救者Y9000P，14代酷睿i9，16英寸2.5K 240Hz电竞屏', '{"屏幕": "16英寸 2.5K 240Hz", "处理器": "i9-14900HX", "内存": "16GB", "存储": "1TB SSD", "显卡": "RTX 4060"}', 1),
('联想拯救者Y7000P 2024', 2, 7499.00, 7999.00, 60, 2200, '联想拯救者Y7000P，14代酷睿i7，15.6英寸2.5K 165Hz屏', '{"屏幕": "15.6英寸 2.5K 165Hz", "处理器": "i7-14650HX", "内存": "16GB", "存储": "512GB SSD", "显卡": "RTX 4060"}', 1),
('华硕ROG 枪神8', 2, 14999.00, 16999.00, 25, 420, '华硕ROG枪神8，14代酷睿i9，16英寸2.5K 240Hz星云屏', '{"屏幕": "16英寸 2.5K 240Hz", "处理器": "i9-14900HX", "内存": "32GB", "存储": "1TB SSD", "显卡": "RTX 4070"}', 1),
('华硕天选5 Pro', 2, 7999.00, 8999.00, 45, 1600, '华硕天选5 Pro，14代酷睿i7，16英寸2.5K 165Hz电竞屏', '{"屏幕": "16英寸 2.5K 165Hz", "处理器": "i7-14650HX", "内存": "16GB", "存储": "512GB SSD", "显卡": "RTX 4060"}', 1),
('机械革命旷世16 Pro', 2, 6999.00, 7999.00, 55, 980, '机械革命旷世16 Pro，13代酷睿i7，16英寸2.5K 165Hz屏', '{"屏幕": "16英寸 2.5K 165Hz", "处理器": "i7-13650HX", "内存": "16GB", "存储": "512GB SSD", "显卡": "RTX 4060"}', 1),
('神舟战神Z8', 2, 5499.00, 5999.00, 80, 1400, '神舟战神Z8，12代酷睿i7，15.6英寸144Hz电竞屏，性价比之选', '{"屏幕": "15.6英寸 144Hz IPS", "处理器": "i7-12700H", "内存": "16GB", "存储": "512GB SSD", "显卡": "RTX 4060"}', 1),
('iPad Pro 12.9英寸 M2', 2, 8999.00, 9999.00, 40, 720, 'Apple iPad Pro 12.9英寸，M2芯片，Liquid Retina XDR显示屏', '{"屏幕": "12.9英寸 Liquid Retina XDR", "芯片": "M2", "内存": "8GB", "存储": "128GB", "支持": "Apple Pencil 2代"}', 1),
('iPad Air 5 M1', 2, 4799.00, 5499.00, 70, 1900, 'Apple iPad Air 5，M1芯片，10.9英寸Liquid Retina屏', '{"屏幕": "10.9英寸 Liquid Retina", "芯片": "M1", "内存": "8GB", "存储": "64GB", "支持": "Apple Pencil 2代"}', 1),
('Surface Pro 9', 2, 8688.00, 9688.00, 30, 450, '微软Surface Pro 9，12代酷睿i5，13英寸PixelSense触控屏', '{"屏幕": "13英寸 PixelSense", "处理器": "i5-1235U", "内存": "8GB", "存储": "256GB SSD", "重量": "879g"}', 1),
('Surface Laptop 5', 2, 7988.00, 8988.00, 25, 380, '微软Surface Laptop 5，12代酷睿i5，13.5英寸PixelSense触控屏', '{"屏幕": "13.5英寸 PixelSense", "处理器": "i5-1235U", "内存": "8GB", "存储": "256GB SSD", "重量": "1.27kg"}', 1),
('AOC 27英寸4K显示器', 2, 1999.00, 2499.00, 100, 2300, 'AOC 27英寸4K IPS显示器，99% sRGB色域，Type-C接口', '{"屏幕": "27英寸 4K IPS", "色域": "99% sRGB", "刷新率": "60Hz", "接口": "HDMI+DP+Type-C", "亮度": "350nit"}', 1);

-- 分类3：家用电器（25个）
INSERT INTO `product` (`name`, `category_id`, `price`, `original_price`, `stock`, `sales`, `description`, `specs`, `status`) VALUES
('戴森V12 Detect Slim', 3, 3990.00, 4590.00, 150, 3200, '戴森V12 Detect Slim无绳吸尘器，激光探测技术，150AW强劲吸力', '{"吸力": "150AW", "续航": "60分钟", "尘杯容量": "0.35L", "重量": "2.2kg", "噪音": "78dB"}', 1),
('戴森V15 Detect', 3, 5290.00, 5990.00, 80, 1800, '戴森V15 Detect无绳吸尘器，激光探测+压电式传感器，240AW吸力', '{"吸力": "240AW", "续航": "60分钟", "尘杯容量": "0.76L", "重量": "3.1kg", "噪音": "82dB"}', 1),
('戴森吹风机HD15', 3, 2990.00, 3490.00, 100, 5600, '戴森Supersonic吹风机，智能温控，负离子护发，快速干发', '{"功率": "1600W", "风速": "4档", "温度": "3档", "重量": "635g", "线长": "2.7m"}', 1),
('戴森卷发棒HS05', 3, 3990.00, 4490.00, 60, 2100, '戴森Airwrap多功能卷发棒，智能温控，多种造型配件', '{"功率": "1300W", "配件": "5种造型", "温度": "3档", "重量": "600g", "线长": "2.62m"}', 1),
('小米扫地机器人2 Pro', 3, 2499.00, 2999.00, 120, 4500, '小米扫地机器人2 Pro，5200Pa大吸力，AI避障，自动集尘', '{"吸力": "5200Pa", "续航": "180分钟", "尘盒": "400ml", "水箱": "200ml", "导航": "LDS激光"}', 1),
('科沃斯T20 Omni', 3, 3999.00, 4599.00, 70, 2800, '科沃斯T20 Omni扫拖机器人，60度热水洗拖布，自动集尘', '{"吸力": "6000Pa", "续航": "260分钟", "水箱": "4L", "尘袋": "3L", "导航": "dToF激光"}', 1),
('石头G20', 3, 4299.00, 4999.00, 50, 1900, '石头G20扫拖机器人，6000Pa吸力，双螺旋胶刷，自动集尘', '{"吸力": "6000Pa", "续航": "180分钟", "尘盒": "350ml", "水箱": "200ml", "导航": "RR Mason 9.0"}', 1),
('美的空调冷静星三代', 3, 2999.00, 3499.00, 80, 6700, '美的1.5匹新一级能效空调，冷静星三代，快速冷暖', '{"匹数": "1.5匹", "能效": "新一级", "制冷量": "3500W", "制热量": "5000W", "噪音": "18dB"}', 1),
('格力空调云佳', 3, 3299.00, 3799.00, 60, 5400, '格力1.5匹新一级能效空调，云佳系列，自清洁功能', '{"匹数": "1.5匹", "能效": "新一级", "制冷量": "3500W", "制热量": "4900W", "噪音": "18dB"}', 1),
('海尔空调静悦', 3, 2799.00, 3199.00, 90, 4200, '海尔1.5匹新一级能效空调，静悦系列，PKC倍速升频', '{"匹数": "1.5匹", "能效": "新一级", "制冷量": "3500W", "制热量": "5000W", "噪音": "17dB"}', 1),
('海尔冰箱BCD-500WGHFD', 3, 4599.00, 5299.00, 40, 2100, '海尔500升十字对开门冰箱，全空间保鲜，干湿分储', '{"容量": "500L", "类型": "十字对开门", "能效": "一级", "噪音": "36dB", "制冷": "风冷无霜"}', 1),
('容声冰箱BCD-501WD16FP', 3, 3999.00, 4599.00, 50, 1800, '容声501升十字对开门冰箱，离子净味，蓝光SPA养鲜', '{"容量": "501L", "类型": "十字对开门", "能效": "一级", "噪音": "36dB", "制冷": "风冷无霜"}', 1),
('美的冰箱BCD-509WSPZM', 3, 4299.00, 4999.00, 45, 1600, '美的509升十字对开门冰箱，PST+智能净化，干湿精控', '{"容量": "509L", "类型": "十字对开门", "能效": "一级", "噪音": "36dB", "制冷": "风冷无霜"}', 1),
('西门子洗碗机SJ636X04JC', 3, 5999.00, 6999.00, 30, 1200, '西门子12套嵌入式洗碗机，智能洗，晶蕾烘干', '{"容量": "12套", "安装": "嵌入式", "烘干": "晶蕾烘干", "程序": "6种", "能耗": "一级"}', 1),
('老板油烟机27A3H', 3, 3499.00, 3999.00, 55, 3400, '老板22m³/min大吸力油烟机，挥手智控，自清洁', '{"风量": "22m³/min", "风压": "460Pa", "噪音": "54dB", "控制": "挥手智控", "清洁": "自清洁"}', 1),
('方太油烟机JCD10TA', 3, 3999.00, 4599.00, 40, 2600, '方太24m³/min大吸力油烟机，智能巡航增压，挥手智控', '{"风量": "24m³/min", "风压": "1000Pa", "噪音": "52dB", "控制": "挥手智控", "清洁": "自清洁"}', 1),
('索尼电视XR-65X90L', 3, 7999.00, 8999.00, 25, 890, '索尼65英寸4K HDR电视，XR认知芯片，120Hz高刷', '{"尺寸": "65英寸", "分辨率": "4K", "刷新率": "120Hz", "HDR": "支持", "芯片": "XR认知芯片"}', 1),
('海信电视75E8H', 3, 6999.00, 7999.00, 30, 1100, '海信75英寸4K ULED电视，144Hz高刷，2200尼特峰值亮度', '{"尺寸": "75英寸", "分辨率": "4K", "刷新率": "144Hz", "亮度": "2200nit", "背光": "ULED X"}', 1),
('TCL电视75Q10G Pro', 3, 6499.00, 7499.00, 35, 1300, 'TCL 75英寸4K Mini LED电视，1200尼特峰值亮度', '{"尺寸": "75英寸", "分辨率": "4K", "刷新率": "144Hz", "亮度": "1200nit", "背光": "Mini LED"}', 1),
('格力空气净化器KJ500G', 3, 1999.00, 2499.00, 70, 2800, '格力空气净化器，500m³/h CADR值，甲醛数显，静音运行', '{"CADR": "500m³/h", "CCM": "P4", "噪音": "33dB", "适用面积": "60㎡", "滤网": "HEPA+活性炭"}', 1),
('小米空气净化器4 Pro', 3, 1299.00, 1599.00, 100, 8900, '小米空气净化器4 Pro，500m³/h CADR值，OLED触控屏', '{"CADR": "500m³/h", "CCM": "P4", "噪音": "34dB", "适用面积": "60㎡", "滤网": "高效滤网"}', 1),
('飞利浦电动剃须刀S9000', 3, 1999.00, 2499.00, 80, 4200, '飞利浦S9000电动剃须刀，智能感应，全身水洗', '{"类型": "旋转式", "刀头": "3刀头", "充电": "1小时", "续航": "60分钟", "防水": "IPX7"}', 1),
('美的电饭煲MB-HF40E108', 3, 599.00, 699.00, 150, 6700, '美的4L智能电饭煲，IH加热，精铁釜内胆，24小时预约', '{"容量": "4L", "加热": "IH电磁", "内胆": "精铁釜", "功率": "1100W", "预约": "24小时"}', 1),
('九阳破壁机L18-Y32', 3, 899.00, 1099.00, 100, 3500, '九阳破壁机，1.75L容量，1200W大功率，自动清洗', '{"容量": "1.75L", "功率": "1200W", "转速": "35000转/分", "功能": "8种", "清洗": "自动"}', 1),
('海尔洗衣机EG100MATE81', 3, 3999.00, 4599.00, 40, 2300, '海尔10公斤滚筒洗衣机，直驱变频，蒸汽除菌', '{"容量": "10kg", "类型": "滚筒", "转速": "1400转", "能效": "一级", "功能": "蒸汽除菌"}', 1);

-- 分类4：服装鞋帽（15个）
INSERT INTO `product` (`name`, `category_id`, `price`, `original_price`, `stock`, `sales`, `description`, `specs`, `status`) VALUES
('优衣库UT印花T恤', 4, 79.00, 99.00, 500, 12000, '优衣库UT系列印花T恤，纯棉面料，舒适透气，多种图案可选', '{"材质": "100%棉", "版型": "宽松", "颜色": "多色", "尺码": "XS-XXL", "产地": "中国"}', 1),
('耐克Air Force 1', 4, 749.00, 899.00, 200, 8900, '耐克Air Force 1经典款运动鞋，皮革鞋面，Air缓震', '{"材质": "皮革+橡胶", "鞋底": "Air缓震", "颜色": "白/黑", "尺码": "36-46", "产地": "越南"}', 1),
('阿迪达斯 Ultraboost 23', 4, 1299.00, 1499.00, 120, 4500, '阿迪达斯Ultraboost 23跑鞋，Boost缓震，Primeknit鞋面', '{"材质": "Primeknit编织", "鞋底": "Boost", "颜色": "多色", "尺码": "36-47", "产地": "印尼"}', 1),
('李宁韦德之道10', 4, 1099.00, 1299.00, 80, 3200, '李宁韦德之道10篮球鞋，䨻科技中底，碳板支撑', '{"材质": "织物+TPU", "中底": "䨻科技", "鞋面": "䨻丝", "尺码": "39-46", "产地": "中国"}', 1),
('安踏C202 6代', 4, 899.00, 1099.00, 100, 2800, '安踏C202 6代跑鞋，氮科技中底，碳板推进', '{"材质": "工程网布", "中底": "氮科技", "碳板": "全掌碳板", "尺码": "39-46", "产地": "中国"}', 1),
('北面1996羽绒服', 4, 2998.00, 3298.00, 60, 5600, '北面1996经典款羽绒服，700蓬松度鹅绒，防风防水', '{"填充": "700蓬鹅绒", "面料": "尼龙", "防水": "DWR涂层", "颜色": "多色", "尺码": "XS-XXL"}', 1),
('波司登极寒羽绒服', 4, 1999.00, 2399.00, 80, 7800, '波司登极寒系列羽绒服，90%白鸭绒，蓄热保暖', '{"填充": "90%白鸭绒", "充绒量": "200g+", "面料": "防风面料", "颜色": "多色", "尺码": "S-3XL"}', 1),
('太平鸟男装休闲裤', 4, 299.00, 399.00, 150, 4200, '太平鸟男装休闲裤，修身直筒，弹力面料，商务休闲', '{"材质": "聚酯纤维+氨纶", "版型": "修身直筒", "颜色": "黑/灰/卡其", "尺码": "28-38", "产地": "中国"}', 1),
('太平鸟女装连衣裙', 4, 459.00, 599.00, 100, 3600, '太平鸟女装连衣裙，法式优雅设计，雪纺面料', '{"材质": "雪纺", "版型": "A字", "颜色": "多色", "尺码": "XS-L", "产地": "中国"}', 1),
('UR快时尚外套', 4, 399.00, 499.00, 120, 2900, 'UR快时尚外套，简约设计，百搭款，春秋必备', '{"材质": "聚酯纤维", "版型": "宽松", "颜色": "多色", "尺码": "XS-L", "产地": "中国"}', 1),
('ZARA西装外套', 4, 599.00, 799.00, 70, 2100, 'ZARA西装外套，通勤百搭款，修身剪裁', '{"材质": "聚酯纤维+粘纤", "版型": "修身", "颜色": "黑/藏青", "尺码": "XS-L", "产地": "土耳其"}', 1),
('H&M基础款卫衣', 4, 149.00, 199.00, 300, 9800, 'H&M基础款卫衣，纯棉面料，舒适百搭，多色可选', '{"材质": "80%棉+20%聚酯", "版型": "常规", "颜色": "多色", "尺码": "XS-XXL", "产地": "孟加拉"}', 1),
('New Balance 574', 4, 699.00, 899.00, 100, 6700, 'New Balance 574经典复古跑鞋，麂皮+网面鞋面', '{"材质": "麂皮+网面", "鞋底": "ENCAP缓震", "颜色": "多色", "尺码": "36-46", "产地": "越南"}', 1),
('匡威Chuck 70', 4, 599.00, 699.00, 150, 7800, '匡威Chuck 70经典帆布鞋，加厚鞋垫，复古设计', '{"材质": "帆布", "鞋底": "橡胶", "颜色": "黑/白/红", "尺码": "35-45", "产地": "印尼"}', 1),
('蕉内500E内裤套装', 4, 129.00, 159.00, 400, 15000, '蕉内500E无感标签内裤，莫代尔面料，3条装', '{"材质": "莫代尔", "款式": "平角/三角", "数量": "3条", "尺码": "M-XXL", "产地": "中国"}', 1);

-- 分类5：食品饮料（15个）
INSERT INTO `product` (`name`, `category_id`, `price`, `original_price`, `stock`, `sales`, `description`, `specs`, `status`) VALUES
('三只松鼠坚果礼盒', 5, 129.00, 169.00, 300, 18000, '三只松鼠坚果礼盒，8种坚果混合装，年货送礼首选', '{"规格": "1495g", "种类": "8种坚果", "保质期": "180天", "储存": "阴凉干燥", "产地": "安徽"}', 1),
('良品铺子猪肉脯', 5, 39.90, 49.90, 500, 25000, '良品铺子猪肉脯，靖江风味，独立小包装', '{"规格": "200g", "口味": "蜜汁/香辣", "保质期": "9个月", "储存": "常温", "产地": "江苏"}', 1),
('百草味每日坚果', 5, 89.90, 109.90, 250, 15000, '百草味每日坚果，7种坚果+3种果干，30天装', '{"规格": "750g(25g×30包)", "种类": "7坚果+3果干", "保质期": "150天", "储存": "阴凉干燥", "产地": "浙江"}', 1),
('蒙牛特仑苏纯牛奶', 5, 59.90, 69.90, 400, 32000, '蒙牛特仑苏纯牛奶，3.6g优质蛋白，12盒装', '{"规格": "250ml×12盒", "蛋白": "3.6g/100ml", "保质期": "6个月", "储存": "常温", "产地": "内蒙古"}', 1),
('伊利安慕希酸奶', 5, 69.90, 79.90, 350, 28000, '伊利安慕希希腊风味酸奶，浓郁丝滑，12盒装', '{"规格": "205g×12盒", "蛋白": "3.1g/100g", "保质期": "6个月", "储存": "常温", "产地": "河北"}', 1),
('可口可乐330ml×24罐', 5, 59.90, 69.90, 500, 45000, '可口可乐经典口味，330ml罐装，24罐整箱', '{"规格": "330ml×24罐", "口味": "经典", "保质期": "12个月", "储存": "常温", "产地": "多产地"}', 1),
('农夫山泉550ml×24瓶', 5, 29.90, 39.90, 600, 52000, '农夫山泉天然饮用水，550ml瓶装，24瓶整箱', '{"规格": "550ml×24瓶", "类型": "天然水", "保质期": "12个月", "储存": "常温", "产地": "多水源地"}', 1),
('元气森林气泡水', 5, 49.90, 59.90, 400, 22000, '元气森林0糖0脂气泡水，多种口味，12瓶装', '{"规格": "480ml×12瓶", "糖分": "0糖", "保质期": "12个月", "储存": "常温", "产地": "安徽"}', 1),
('雀巢速溶咖啡', 5, 69.90, 89.90, 200, 18000, '雀巢速溶咖啡，1+2奶香口味，100条装', '{"规格": "1500g(15g×100条)", "口味": "奶香", "保质期": "18个月", "储存": "阴凉干燥", "产地": "广东"}', 1),
('立顿红茶包', 5, 29.90, 39.90, 300, 15000, '立顿精选红茶包，100包独立茶包，下午茶必备', '{"规格": "200g(2g×100包)", "类型": "红茶", "保质期": "24个月", "储存": "阴凉干燥", "产地": "福建"}', 1),
('金龙鱼大米5kg', 5, 39.90, 49.90, 500, 35000, '金龙鱼优质东北大米，5kg装，颗粒饱满', '{"规格": "5kg", "类型": "东北大米", "保质期": "12个月", "储存": "阴凉干燥", "产地": "黑龙江"}', 1),
('鲁花花生油5L', 5, 129.90, 149.90, 200, 22000, '鲁花5S压榨花生油，5L桶装，物理压榨', '{"规格": "5L", "类型": "压榨花生油", "保质期": "18个月", "储存": "阴凉避光", "产地": "山东"}', 1),
('海天酱油1.9L', 5, 19.90, 24.90, 400, 42000, '海天金标生抽酱油，1.9L大瓶装，酿造酱油', '{"规格": "1.9L", "类型": "生抽", "保质期": "24个月", "储存": "阴凉", "产地": "广东"}', 1),
('李锦记蚝油680g', 5, 15.90, 19.90, 350, 28000, '李锦记旧庄蚝油，680g挤挤装，提鲜调味', '{"规格": "680g", "类型": "蚝油", "保质期": "18个月", "储存": "冷藏", "产地": "广东"}', 1),
('盼盼法式小面包', 5, 25.90, 29.90, 300, 38000, '盼盼法式小面包，奶香味，500g袋装，早餐零食', '{"规格": "500g", "口味": "奶香", "保质期": "6个月", "储存": "常温", "产地": "福建"}', 1);

