package org.example.java_ai.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件上传工具类
 */
@Slf4j
@Component
public class FileUploadUtil {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    // 存储绝对路径
    private String absoluteUploadPath;

    @Value("${file.access.url:http://localhost:8080/uploads}")
    private String accessUrl;

    /**
     * 初始化上传目录
     */
    @PostConstruct
    public void init() {
        try {
            Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
            absoluteUploadPath = path.toString();
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("✅ 创建上传目录: {}", path);
            } else {
                log.info("✅ 上传目录已存在: {}", path);
            }
        } catch (IOException e) {
            log.error("创建上传目录失败", e);
            throw new RuntimeException("初始化上传目录失败: " + e.getMessage());
        }
    }

    /**
     * 上传单个文件
     * 
     * @param file 上传的文件
     * @param directory 子目录（按日期分类）
     * @return 文件访问URL
     */
    public String uploadFile(MultipartFile file, String directory) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }

        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            throw new RuntimeException("仅支持上传图片文件（jpg、png、gif、webp）");
        }

        // 验证文件大小（限制 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("图片大小不能超过 5MB");
        }

        try {
            // 生成唯一文件名
            String extension = getFileExtension(originalFilename);
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

            // 按日期创建子目录
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            
            // 使用绝对路径构建完整路径
            Path targetPath = Paths.get(absoluteUploadPath, directory, dateDir, fileName);

            // 确保目录存在（关键修复）
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("✅ 创建子目录: {}", parentDir);
            }

            // 保存文件
            file.transferTo(targetPath.toFile());

            log.info("✅ 文件上传成功: {}", targetPath);

            // 返回访问URL（保持相对路径格式）
            return accessUrl + "/" + directory + "/" + dateDir + "/" + fileName;

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || 
               lower.endsWith(".png") || lower.endsWith(".gif") || 
               lower.endsWith(".webp") || lower.endsWith(".bmp");
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".jpg";
    }
}
