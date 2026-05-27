package org.example.java_ai.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应封装
 * 
 * @param <T> 数据类型
 * @author xqy
 * @since 2026-04-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Long pageNum;
    
    /**
     * 每页大小
     */
    private Long pageSize;
    
    /**
     * 总页数
     */
    private Long pages;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
    
    public PageResult(List<T> records, Long total, Long pageNum, Long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = (total + pageSize - 1) / pageSize;
        this.hasNext = pageNum < this.pages;
        this.hasPrevious = pageNum > 1;
    }
    
    /**
     * 创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNum, Long pageSize) {
        return new PageResult<>(records, total, pageNum, pageSize);
    }
}
