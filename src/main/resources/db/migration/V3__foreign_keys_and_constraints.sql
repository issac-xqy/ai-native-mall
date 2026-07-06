-- ==========================================
-- Flyway V3: 外键约束 + 字段完整性约束
-- ==========================================

-- product 表: 先放宽 category_id 为可空（用于 ON DELETE SET NULL）
ALTER TABLE product MODIFY COLUMN category_id BIGINT NULL;
ALTER TABLE product
    ADD CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_product_price CHECK (price >= 0),
    ADD CONSTRAINT chk_product_stock CHECK (stock >= 0),
    ADD CONSTRAINT chk_product_status CHECK (status IN (0, 1));

-- product_comment 表: 外键 + 评分约束
ALTER TABLE product_comment
    ADD CONSTRAINT fk_comment_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_comment_rating CHECK (rating >= 1 AND rating <= 5);

-- shopping_cart 表: 外键 + 数量约束
ALTER TABLE shopping_cart
    ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_cart_quantity CHECK (quantity > 0 AND quantity <= 999);

-- orders 表: 外键 + 状态约束
ALTER TABLE orders
    ADD CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_order_status CHECK (status IN (0, 1, 2, 3, 4)),
    ADD CONSTRAINT chk_order_amount CHECK (total_amount >= 0);

-- order_item 表: 外键 + 约束（先放宽 product_id 为可空）
ALTER TABLE order_item MODIFY COLUMN product_id BIGINT NULL;
ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_order_item_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE SET NULL,
    ADD CONSTRAINT chk_item_quantity CHECK (quantity > 0),
    ADD CONSTRAINT chk_item_price CHECK (price >= 0);

-- user_address 表: 外键
ALTER TABLE user_address
    ADD CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- user_wallet 表: 外键 + 余额约束
ALTER TABLE user_wallet
    ADD CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_wallet_balance CHECK (balance >= 0);

-- recharge_record 表: 外键 + 金额约束
ALTER TABLE recharge_record
    ADD CONSTRAINT fk_recharge_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT chk_recharge_amount CHECK (amount > 0);

-- product_view_log 表: 外键
ALTER TABLE product_view_log
    ADD CONSTRAINT fk_view_log_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_view_log_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;

-- ai_chat_record 表: 外键
ALTER TABLE ai_chat_record
    ADD CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE;

-- sys_user 表: 字段约束
ALTER TABLE sys_user
    ADD CONSTRAINT chk_user_status CHECK (status IN (0, 1));

-- sys_role_permission 表: FK
ALTER TABLE sys_role_permission
    ADD CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id) ON DELETE CASCADE;

-- sys_user_role 表: FK
ALTER TABLE sys_user_role
    ADD CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE;
