-- ==========================================
-- Flyway V2: RBAC 角色权限系统
-- ==========================================

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    resource_type VARCHAR(50) COMMENT '资源类型: MENU/BUTTON/API',
    parent_id BIGINT DEFAULT 0 COMMENT '父权限ID',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_role_id (role_id),
    KEY idx_perm_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ==================== 种子数据 ====================

-- 角色
INSERT IGNORE INTO sys_role (role_code, role_name, description) VALUES
('admin', '超级管理员', '拥有所有权限'),
('product_manager', '商品管理员', '管理商品、分类、评论'),
('order_manager', '订单管理员', '管理订单、退款'),
('viewer', '只读用户', '查看数据统计和列表');

-- 权限
INSERT IGNORE INTO sys_permission (perm_code, perm_name, resource_type) VALUES
('product:read', '查看商品', 'API'),
('product:write', '新建/编辑商品', 'API'),
('product:delete', '删除商品', 'API'),
('category:read', '查看分类', 'API'),
('category:write', '管理分类', 'API'),
('order:read', '查看订单', 'API'),
('order:write', '编辑订单状态', 'API'),
('order:refund', '处理退款', 'API'),
('dashboard:view', '查看数据报表', 'API'),
('comment:view', '查看评论分析', 'API'),
('ai:operate', 'AI智能运营', 'API'),
('knowledge:manage', '知识库管理', 'API'),
('user:manage', '用户管理', 'API'),
('system:config', '系统配置', 'API');

-- 分配 admin 角色给用户 ID 1 (现有 admin 用户)
INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- admin 角色拥有所有权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- product_manager 角色权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission WHERE perm_code IN ('product:read','product:write','category:read','category:write','comment:view','dashboard:view');

-- order_manager 角色权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission WHERE perm_code IN ('order:read','order:write','order:refund','dashboard:view');

-- viewer 角色权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 4, id FROM sys_permission WHERE perm_code IN ('product:read','category:read','order:read','dashboard:view','comment:view');
