-- ==========================================
-- 商品分类初始化数据
-- 包含一级和二级分类
-- ==========================================

USE ai_mall;

-- 清空现有分类数据（可选）
-- TRUNCATE TABLE product_category;

-- 一级分类（已存在，这里仅作为参考）
-- INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`, `status`) VALUES
-- ('手机数码', 0, 1, 1),
-- ('电脑办公', 0, 2, 1),
-- ('家用电器', 0, 3, 1),
-- ('服装鞋帽', 0, 4, 1),
-- ('食品饮料', 0, 5, 1);

-- 手机数码 - 二级分类
INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`, `status`) VALUES
('智能手机', 1, 1, 1),
('平板电脑', 1, 2, 1),
('智能穿戴', 1, 3, 1),
('手机配件', 1, 4, 1),
('摄影摄像', 1, 5, 1);

-- 电脑办公 - 二级分类
INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`, `status`) VALUES
('笔记本电脑', 2, 1, 1),
('台式机', 2, 2, 1),
('显示器', 2, 3, 1),
('办公设备', 2, 4, 1),
('电脑配件', 2, 5, 1);

-- 家用电器 - 二级分类
INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`, `status`) VALUES
('空调', 3, 1, 1),
('冰箱', 3, 2, 1),
('洗衣机', 3, 3, 1),
('厨房电器', 3, 4, 1),
('生活电器', 3, 5, 1);

-- 服装鞋帽 - 二级分类
INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`, `status`) VALUES
('男装', 4, 1, 1),
('女装', 4, 2, 1),
('运动鞋', 4, 3, 1),
('休闲鞋', 4, 4, 1),
('箱包配饰', 4, 5, 1);

-- 食品饮料 - 二级分类
INSERT INTO `product_category` (`name`, `parent_id`, `sort_order`, `status`) VALUES
('休闲零食', 5, 1, 1),
('饮料冲调', 5, 2, 1),
('粮油调味', 5, 3, 1),
('生鲜水果', 5, 4, 1),
('营养保健', 5, 5, 1);

-- 验证数据
SELECT id, name, parent_id, sort_order, status FROM product_category ORDER BY parent_id, sort_order;
