package org.example.java_ai.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析服务 - 支持PDF、Word、TXT格式
 * 
 * @author xqy
 * @since 2026-04-15
 */
@Slf4j
@Service
public class DocumentParserService {

    /**
     * 解析上传的文档，提取纯文本内容
     */
    public String parseDocument(MultipartFile file, String fileType) {
        try (InputStream inputStream = file.getInputStream()) {
            return switch (fileType.toLowerCase()) {
                case "pdf" -> parsePdf(inputStream);
                case "docx", "doc" -> parseDocx(inputStream);
                case "txt", "md" -> parseTxt(inputStream);
                default -> throw new IllegalArgumentException("不支持的文件类型: " + fileType);
            };
        } catch (Exception e) {
            log.error("文档解析失败: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析PDF文件
     */
    private String parsePdf(InputStream inputStream) throws Exception {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("PDF解析成功，提取文本长度: {}", text.length());
            return cleanText(text);
        }
    }

    /**
     * 解析Word文档（.docx）
     */
    private String parseDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder text = new StringBuilder();
            document.getParagraphs().forEach(para -> {
                String paraText = para.getText();
                if (paraText != null && !paraText.isEmpty()) {
                    text.append(paraText).append("\n");
                }
            });
            log.info("Word解析成功，提取文本长度: {}", text.length());
            return cleanText(text.toString());
        }
    }

    /**
     * 解析TXT/Markdown文件
     */
    private String parseTxt(InputStream inputStream) throws Exception {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("TXT解析成功，提取文本长度: {}", text.length());
        return cleanText(text);
    }

    /**
     * 清理文本：去除多余空白行和特殊字符
     */
    private String cleanText(String text) {
        if (text == null) return "";
        
        return text
            .replaceAll("\\r\\n", "\n")
            .replaceAll("\\r", "\n")
            .replaceAll("\n{3,}", "\n\n")
            .trim();
    }

    /**
     * 将长文档分割成多个段落（用于向量化）
     * @param content 文档内容
     * @param maxChunkSize 每个片段的最大字符数
     */
    public List<String> splitIntoChunks(String content, int maxChunkSize) {
        List<String> chunks = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return chunks;
        }

        // 按段落分割
        String[] paragraphs = content.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmedPara = paragraph.trim();
            if (trimmedPara.isEmpty()) continue;

            // 如果当前片段加上新段落超过限制
            if (currentChunk.length() + trimmedPara.length() > maxChunkSize) {
                // 保存当前片段
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk.setLength(0);
                }
                
                // 如果单个段落就超过限制，需要进一步拆分
                if (trimmedPara.length() > maxChunkSize) {
                    // 按句子拆分
                    String[] sentences = trimmedPara.split("[。！？.!?]");
                    for (String sentence : sentences) {
                        if (sentence.trim().isEmpty()) continue;
                        
                        if (currentChunk.length() + sentence.length() > maxChunkSize) {
                            chunks.add(currentChunk.toString());
                            currentChunk.setLength(0);
                        }
                        currentChunk.append(sentence.trim()).append("。");
                    }
                } else {
                    currentChunk.append(trimmedPara);
                }
            } else {
                currentChunk.append(trimmedPara).append("\n");
            }
        }

        // 添加最后一个片段
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        log.info("文档分块完成，共 {} 个片段", chunks.size());
        return chunks;
    }
}
