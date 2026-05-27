package org.example.java_ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
@Data
@TableName("product")
public class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long categoryId;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private Integer sales;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 点击量
     */
    private Integer clickCount;

    private String image;

    private String images;

    private String description;

    private String specs;

    /**
     * AI生成的SEO标题
     */
    private String seoTitle;

    /**
     * AI生成的营销文案
     */
    private String aiDescription;

    /**
     * 情感评分（1-5）
     */
    private BigDecimal sentimentScore;

    private Integer status;

    /**
     * 发布状态: 0-草稿 1-已上架 2-已下架
     */
    private Integer publishStatus;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
