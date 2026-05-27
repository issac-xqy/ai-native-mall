package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.java_ai.entity.KnowledgeDocument;

/**
 * AI知识库文档Mapper
 * 
 * @author xqy
 * @since 2026-04-15
 */
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
}
