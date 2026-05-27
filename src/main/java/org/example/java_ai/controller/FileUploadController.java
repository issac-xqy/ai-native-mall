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

    private static final java.util.Set<String> ALLOWED_EXTENSIONS =
            java.util.Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 单图上传（商品主图）
     */
    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        String validationError = validateFile(file);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", validationError));
        }
        try {
            log.info("上传图片: {}", file.getOriginalFilename());
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
        if (files.length > 9) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "最多只能上传 9 张图片"
            ));
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String validationError = validateFile(file);
            if (validationError != null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", validationError));
            }
        }

        try {
            for (MultipartFile file : files) {
                urls.add(fileUploadUtil.uploadFile(file, "product"));
            }
            log.info("多图上传成功，共 {} 张", urls.size());
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "上传成功",
                "data", Map.of("urls", urls, "count", urls.size())
            ));
        } catch (Exception e) {
            log.error("多图上传失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "上传失败: " + e.getMessage()));
        }
    }

    private String validateFile(MultipartFile file) {
        if (file.isEmpty()) return "文件为空";
        if (file.getSize() > MAX_FILE_SIZE) return "文件大小不能超过10MB";
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return "文件名无效";
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return "不支持的文件类型: " + ext + "，仅支持 " + String.join(",", ALLOWED_EXTENSIONS);
        }
        return null;
    }
}
