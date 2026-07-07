package org.example.java_ai.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.KnowledgeDocument;
import org.example.java_ai.mapper.KnowledgeDocumentMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 知识库管理服务
 * 
 * @author xqy
 * @since 2026-04-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentParserService documentParserService;
    private final EmbeddingModel embeddingModel;
    
    @Qualifier("faqEmbeddingStore")
    private final EmbeddingStore<TextSegment> faqEmbeddingStore;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传文档并建立知识库
     */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeDocument uploadDocument(MultipartFile file, String title, String category, Long userId, String userName) {
        log.info("开始上传知识库文档: {}, 分类: {}", file.getOriginalFilename(), category);

        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new RuntimeException("文件名不能为空");
            }

            // 获取文件类型
            String fileType = getFileExtension(originalFilename);
            validateFileType(fileType);

            // 解析文档内容
            String content = documentParserService.parseDocument(file, fileType);
            if (content.isEmpty()) {
                throw new RuntimeException("文档内容为空");
            }

            // 保存文件
            String fileName = saveFile(file, fileType);

            // 创建知识库记录
            KnowledgeDocument kb = new KnowledgeDocument();
            kb.setDocName(title != null ? title : originalFilename);
            kb.setDocType(fileType);
            kb.setFilePath(fileName);
            kb.setCategory(category != null ? category : "custom");
            kb.setStatus(0); // 未处理
            kb.setFileSize(file.getSize());
            kb.setUploadUserId(userId);
            kb.setUploadUserName(userName);

            knowledgeDocumentMapper.insert(kb);
            log.info("文档基本信息保存成功，ID: {}", kb.getId());

            // 向量化文档
            boolean success = vectorizeDocument(kb.getId(), content, category);
            
            if (success) {
                kb.setStatus(1); // 已向量化
                knowledgeDocumentMapper.updateById(kb);
                log.info("✅ 知识库文档上传并向量化成功: {}", kb.getDocName());
            } else {
                kb.setStatus(2); // 向量化失败
                knowledgeDocumentMapper.updateById(kb);
                log.warn("⚠️ 文档向量化失败: {}", kb.getDocName());
            }

            return kb;

        } catch (Exception e) {
            log.error("上传知识库文档失败", e);
            throw new RuntimeException("上传失败: " + e.getMessage());
        }
    }

    /**
     * 向量化文档内容
     */
    private boolean vectorizeDocument(Long docId, String content, String category) {
        try {
            // 将文档分割成多个片段
            List<String> chunks = documentParserService.splitIntoChunks(content, 1000);
            log.info("开始向量化，共 {} 个片段", chunks.size());

            List<String> vectorIds = new ArrayList<>();

            // 对每个片段进行向量化
            for (int i = 0; i < chunks.size(); i++) {
                String chunk = chunks.get(i);
                
                // 创建元数据
                Metadata metadata = Metadata.metadata("docId", String.valueOf(docId))
                        .put("category", category)
                        .put("chunkIndex", String.valueOf(i))
                        .put("type", "knowledge_base");

                TextSegment segment = TextSegment.from(chunk, metadata);
                var embedding = embeddingModel.embed(segment).content();
                String vectorId = faqEmbeddingStore.add(embedding, segment);
                
                vectorIds.add(vectorId);
            }

            // 保存向量ID列表
            String vectorIdsJson = objectMapper.writeValueAsString(vectorIds);
            
            KnowledgeDocument kb = knowledgeDocumentMapper.selectById(docId);
            if (kb != null) {
                kb.setVectorIds(vectorIdsJson);
                knowledgeDocumentMapper.updateById(kb);
            }

            log.info("✅ 文档向量化完成，生成 {} 个向量", vectorIds.size());
            return true;

        } catch (Exception e) {
            log.error("文档向量化失败", e);
            return false;
        }
    }

    /**
     * 删除知识库文档
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        KnowledgeDocument kb = knowledgeDocumentMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("文档不存在");
        }

        // 删除向量库中的向量
        if (kb.getVectorIds() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> vectorIds = objectMapper.readValue(kb.getVectorIds(), List.class);
                
                for (String vectorId : vectorIds) {
                    faqEmbeddingStore.remove(vectorId);
                }
                
                log.info("已删除 {} 个向量", vectorIds.size());
            } catch (Exception e) {
                log.error("删除向量失败", e);
            }
        }

        // 删除文件
        if (kb.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(kb.getFilePath()));
                log.info("已删除文件: {}", kb.getFilePath());
            } catch (Exception e) {
                log.warn("删除文件失败: {}", kb.getFilePath(), e);
            }
        }

        // 删除数据库记录
        knowledgeDocumentMapper.deleteById(id);
        log.info("✅ 知识库文档删除成功: {}", kb.getDocName());
    }

    /**
     * 查询知识库列表
     */
    public List<KnowledgeDocument> listDocuments(String category) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeDocument> wrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        
        if (category != null && !category.isEmpty()) {
            wrapper.eq(KnowledgeDocument::getCategory, category);
        }
        
        wrapper.orderByDesc(KnowledgeDocument::getCreateTime);
        
        return knowledgeDocumentMapper.selectList(wrapper);
    }

    /**
     * 获取文档详情（不包含内容）
     */
    public KnowledgeDocument getDocumentDetail(Long id) {
        KnowledgeDocument kb = knowledgeDocumentMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("文档不存在");
        }
        kb.setContent(null); // 不返回完整内容
        return kb;
    }

    /**
     * 重新向量化文档
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean reVectorize(Long id) {
        KnowledgeDocument kb = knowledgeDocumentMapper.selectById(id);
        if (kb == null) {
            throw new RuntimeException("文档不存在");
        }

        // 先删除旧的向量
        if (kb.getVectorIds() != null) {
            try {
                @SuppressWarnings("unchecked")
                List<String> oldVectorIds = objectMapper.readValue(kb.getVectorIds(), List.class);
                for (String vectorId : oldVectorIds) {
                    faqEmbeddingStore.remove(vectorId);
                }
            } catch (Exception e) {
                log.error("删除旧向量失败", e);
            }
        }

        // 读取文件内容
        try {
            String content = Files.readString(Paths.get(kb.getFilePath()));
            boolean success = vectorizeDocument(id, content, kb.getCategory());
            
            if (success) {
                kb.setStatus(1);
            } else {
                kb.setStatus(2);
            }
            knowledgeDocumentMapper.updateById(kb);
            
            return success;
        } catch (Exception e) {
            log.error("重新向量化失败", e);
            kb.setStatus(2);
            knowledgeDocumentMapper.updateById(kb);
            return false;
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private void validateFileType(String fileType) {
        List<String> supportedTypes = List.of("pdf", "docx", "doc", "txt", "md");
        if (!supportedTypes.contains(fileType.toLowerCase())) {
            throw new RuntimeException("不支持的文件类型: " + fileType + "，仅支持: " + supportedTypes);
        }
    }

    private String saveFile(MultipartFile file, String fileType) throws Exception {
        // 使用项目根目录下的 uploads 路径
        String basePath = System.getProperty("user.dir");
        String uploadPath = basePath + "/uploads/knowledge";
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + fileType;
        
        Path targetPath = Paths.get(uploadPath, dateDir, fileName);
        Files.createDirectories(targetPath.getParent());
        
        file.transferTo(targetPath.toFile());
        log.info("文件保存成功: {}", targetPath.toAbsolutePath());
        
        return targetPath.toAbsolutePath().toString();
    }
}
