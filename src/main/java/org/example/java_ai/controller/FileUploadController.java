package org.example.java_ai.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.util.FileUploadUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadUtil fileUploadUtil;

    /**
     * 单图上传（商品主图）
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("📤 开始上传图片: {}", file.getOriginalFilename());
            
            String url = fileUploadUtil.uploadFile(file, "product");
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "上传成功",
                "data", Map.of(
                    "url", url,
                    "filename", file.getOriginalFilename(),
                    "size", file.getSize()
                )
            ));
        } catch (Exception e) {
            log.error("图片上传失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "上传失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 多图上传（商品详情图）
     */
    @PostMapping("/images")
    public ResponseEntity<Map<String, Object>> uploadImages(@RequestParam("files") MultipartFile[] files) {
        try {
            log.info("📤 开始上传多图，数量: {}", files.length);
            
            if (files.length > 9) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "最多只能上传 9 张图片"
                ));
            }

            List<String> urls = new ArrayList<>();
            for (MultipartFile file : files) {
                String url = fileUploadUtil.uploadFile(file, "product");
                urls.add(url);
            }

            log.info("✅ 多图上传成功，共 {} 张", urls.size());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "上传成功",
                "data", Map.of(
                    "urls", urls,
                    "count", urls.size()
                )
            ));
        } catch (Exception e) {
            log.error("多图上传失败", e);
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "上传失败: " + e.getMessage()
            ));
        }
    }
}
