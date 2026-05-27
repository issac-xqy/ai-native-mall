-- 修复 product_comment 表中 create_time 为 NULL 的记录
-- 将这些记录的时间设置为当前时间（或根据实际业务逻辑调整）

-- 方案1：设置为当前时间
UPDATE product_comment 
SET create_time = NOW(), update_time = NOW() 
WHERE create_time IS NULL OR create_time = '';

-- 方案2：如果有 update_time，则复制 update_time 作为 create_time
-- UPDATE product_comment 
-- SET create_time = update_time 
-- WHERE create_time IS NULL AND update_time IS NOT NULL;

-- 验证修复结果
SELECT id, product_id, user_id, content, create_time, update_time 
FROM product_comment 
WHERE deleted = 0 
ORDER BY create_time DESC;
