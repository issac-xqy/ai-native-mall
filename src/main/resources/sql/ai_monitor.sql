-- AI监控日志表
CREATE TABLE IF NOT EXISTS ai_monitor_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id VARCHAR(100) COMMENT '会话ID',
    user_id VARCHAR(100) COMMENT '用户ID',
    question TEXT COMMENT '用户问题',
    answer TEXT COMMENT 'AI回答',
    response_time INT COMMENT '响应时间（毫秒）',
    response_level VARCHAR(20) COMMENT '响应等级：green/orange/red',
    error_type VARCHAR(100) COMMENT '错误类型',
    is_error TINYINT DEFAULT 0 COMMENT '是否错误：0-否，1-是',
    is_missing_knowledge TINYINT DEFAULT 0 COMMENT '是否无知识：0-否，1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_create_time (create_time),
    INDEX idx_user_id (user_id),
    INDEX idx_response_level (response_level),
    INDEX idx_is_error (is_error)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI监控日志表';

-- AI关键词映射表
CREATE TABLE IF NOT EXISTS ai_keyword_mapping (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    keyword VARCHAR(200) NOT NULL COMMENT '原始关键词',
    synonyms JSON COMMENT '同义词列表',
    expand_words JSON COMMENT '扩展词列表',
    weight DECIMAL(3,2) DEFAULT 1.00 COMMENT '权重',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_keyword (keyword),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI关键词映射表';

-- AI知识库文档管理表
CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    doc_name VARCHAR(200) NOT NULL COMMENT '文档名称',
    doc_type VARCHAR(50) COMMENT '文档类型: pdf/word/txt/markdown',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    content LONGTEXT COMMENT '文档内容（纯文本）',
    category VARCHAR(50) DEFAULT 'custom' COMMENT '分类标签: product/policy/faq/custom',
    status TINYINT DEFAULT 0 COMMENT '状态: 0-未处理 1-已向量化 2-向量化失败',
    vector_ids JSON COMMENT '向量ID列表（JSON数组）',
    file_size BIGINT COMMENT '文件大小（字节）',
    upload_user_id BIGINT COMMENT '上传用户ID',
    upload_user_name VARCHAR(100) COMMENT '上传用户名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_doc_type (doc_type),
    INDEX idx_category (category),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档表';
