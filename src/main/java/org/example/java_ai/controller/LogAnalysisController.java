package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.service.ai.LogAnalysisService;
import org.example.java_ai.service.ai.LogAnalysisService.LogError;
import org.example.java_ai.service.ai.LogAnalysisService.AnalysisResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    /** POST /api/admin/logs/analyze — full batch analysis, returns markdown report */
    @PostMapping("/analyze")
    public Result<Map<String, Object>> analyzeLogs() {
        log.info("Starting log analysis...");
        List<LogError> errors = logAnalysisService.extractErrors(50);
        log.info("Extracted {} errors from logs", errors.size());

        Map<String, List<LogError>> groups = logAnalysisService.deduplicateErrors(errors);
        log.info("{} unique error patterns found", groups.size());

        List<Map<String, Object>> findings = new ArrayList<>();

        // Analyze top 5 most frequent error patterns
        groups.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(5)
                .forEach(entry -> {
                    LogError sample = entry.getValue().get(0);
                    int count = entry.getValue().size();
                    AnalysisResult result = logAnalysisService.analyzeRootCause(sample);

                    findings.add(Map.of(
                            "errorType", sample.exceptionType(),
                            "message", sample.message(),
                            "count", count,
                            "codeLocation", sample.codeLocation(),
                            "rootCause", result.rootCause(),
                            "affectedComponent", result.affectedComponent(),
                            "severity", result.severity(),
                            "fixSuggestion", result.fixSuggestion()
                    ));
                });

        // Build markdown report
        StringBuilder report = new StringBuilder();
        report.append("# AI Log Analysis Report\n");
        report.append("> Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        report.append("**Summary:** ").append(errors.size()).append(" errors scanned, ")
                .append(groups.size()).append(" unique patterns, top ").append(findings.size()).append(" analyzed\n\n");

        findings.forEach(f -> {
            report.append("## ").append(f.get("errorType")).append(" (Count: ").append(f.get("count")).append(")\n\n");
            report.append("| Field | Value |\n|---|---|\n");
            report.append("| **Error** | ").append(((String) f.get("message")).length() > 100
                    ? ((String) f.get("message")).substring(0, 100) + "..." : f.get("message")).append(" |\n");
            report.append("| **Root Cause** | ").append(f.get("rootCause")).append(" |\n");
            report.append("| **Affected** | ").append(f.get("affectedComponent")).append(" |\n");
            report.append("| **Severity** | `").append(f.get("severity")).append("` |\n");
            report.append("| **Fix** | ").append(f.get("fixSuggestion")).append(" |\n");
            Object loc = f.get("codeLocation");
            if (loc != null && !loc.toString().isEmpty()) {
                report.append("| **Code** | `").append(loc).append("` |\n");
            }
            report.append("\n");
        });

        return Result.success(Map.of(
                "totalErrors", errors.size(),
                "uniquePatterns", groups.size(),
                "analyzedPatterns", findings.size(),
                "report", report.toString(),
                "findings", findings
        ));
    }
}
