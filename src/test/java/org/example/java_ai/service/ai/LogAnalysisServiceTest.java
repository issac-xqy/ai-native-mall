package org.example.java_ai.service.ai;

import org.example.java_ai.service.ai.LogAnalysisService.LogError;
import org.example.java_ai.service.ai.LogAnalysisService.AnalysisResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Disabled("Requires running backend + DB — run manually or via IDE")
@DisplayName("LogAnalysisService 日志分析测试")
class LogAnalysisServiceTest {

    @Autowired
    private LogAnalysisService logAnalysisService;

    @Test
    @DisplayName("提取日志错误列表")
    void extractErrors_ReturnsList() {
        List<LogError> errors = logAnalysisService.extractErrors(20);
        assertNotNull(errors);
        System.out.println("Extracted " + errors.size() + " errors");
        errors.forEach(e -> System.out.printf("  [%s] %s: %s (at %s)%n",
                e.level(), e.exceptionType(), e.message().substring(0, Math.min(80, e.message().length())), e.codeLocation()));
    }

    @Test
    @DisplayName("去重错误模式")
    void deduplicateErrors_GroupsByType() {
        List<LogError> errors = logAnalysisService.extractErrors(30);
        Map<String, List<LogError>> groups = logAnalysisService.deduplicateErrors(errors);
        assertNotNull(groups);
        System.out.println(groups.size() + " unique patterns from " + errors.size() + " errors");
        groups.forEach((key, list) ->
                System.out.printf("  Count=%d: %s%n", list.size(), key.substring(0, Math.min(100, key.length()))));
    }

    @Test
    @DisplayName("AI 分析单个错误根因")
    void analyzeRootCause_SingleError() {
        List<LogError> errors = logAnalysisService.extractErrors(10);
        if (!errors.isEmpty()) {
            // Pick first non-trivial error
            LogError sample = errors.stream()
                    .filter(e -> !e.exceptionType().equals("Unknown"))
                    .findFirst().orElse(errors.get(0));

            AnalysisResult result = logAnalysisService.analyzeRootCause(sample);
            assertNotNull(result);
            System.out.println("--- AI Analysis ---");
            System.out.println("Error: " + sample.message().substring(0, Math.min(100, sample.message().length())));
            System.out.println("Root Cause: " + result.rootCause());
            System.out.println("Affected: " + result.affectedComponent());
            System.out.println("Severity: " + result.severity());
            System.out.println("Fix: " + result.fixSuggestion());
        }
    }
}
