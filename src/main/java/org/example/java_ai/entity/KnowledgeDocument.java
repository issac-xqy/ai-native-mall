package org.example.java_ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI知识库文档实体
 * 
 * @author xqy
 * @since 2026-04-15
 */
@Data
@TableName("ai_knowledge_document")
public class KnowledgeDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文档名称
     */
    private String docName;

    /**
     * 文档类型: pdf/word/txt/markdown
     */
    private String docType;

    /**
     * 文件存储路径
     */
    private String filePath;

    /**
     * 文档内容（纯文本）
     */
    @TableField(select = false)
    private String content;

    /**
     * 分类标签: product/policy/faq/custom
     */
    private String category;

    /**
     * 状态: 0-未处理 1-已向量化 2-向量化失败
     */
    private Integer status;

    /**
     * 向量ID列表（JSON数组）
     */
    private String vectorIds;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 上传用户ID
     */
    private Long uploadUserId;

    /**
     * 上传用户名
     */
    private String uploadUserName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
