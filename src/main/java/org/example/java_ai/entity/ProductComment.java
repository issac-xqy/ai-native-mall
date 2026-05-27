package org.example.java_ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 商品评论实体类
 * 
 * @author xqy
 * @since 2026-04-09
 */
@Data
public class ProductComment {
    private Long id;
    private Long productId;
    private Long userId;
    private String content;
    private String sentiment;
    private String aiTags;
    private String summary;
    private Integer rating;
    private Integer deleted;
    
    /**
     * 创建时间（自动填充 + JSON格式化）
     */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
    
    /**
     * 更新时间（自动填充 + JSON格式化）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
