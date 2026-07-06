package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.KnowledgeDocument;
import org.example.java_ai.service.ai.KnowledgeBaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理API
 * 
 * @author xqy
 * @since 2026-04-15
 */
@Slf4j
@RestController
@RequestMapping("/admin/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 上传知识库文档
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", defaultValue = "custom") String category,
            @RequestParam(value = "userId", defaultValue = "1") Long userId,
            @RequestParam(value = "userName", defaultValue = "admin") String userName) {
        
        log.info("接收到知识库文档上传请求: {}, 分类: {}", file.getOriginalFilename(), category);

        try {
            KnowledgeDocument kb = knowledgeBaseService.uploadDocument(file, title, category, userId, userName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", kb);
            result.put("message", "文档上传并向量化成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("文档上传失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 查询知识库列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listDocuments(
            @RequestParam(value = "category", required = false) String category) {
        
        List<KnowledgeDocument> documents = knowledgeBaseService.listDocuments(category);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", documents);
        result.put("total", documents.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentDetail(@PathVariable Long id) {
        KnowledgeDocument kb = knowledgeBaseService.getDocumentDetail(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", kb);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDocument(@PathVariable Long id) {
        try {
            knowledgeBaseService.deleteDocument(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文档删除成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("删除文档失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * 重新向量化文档
     */
    @PostMapping("/{id}/revectorize")
    public ResponseEntity<Map<String, Object>> reVectorize(@PathVariable Long id) {
        try {
            boolean success = knowledgeBaseService.reVectorize(id);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", success ? "重新向量化成功" : "重新向量化失败");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("重新向量化失败", e);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(result);
        }
    }
}
