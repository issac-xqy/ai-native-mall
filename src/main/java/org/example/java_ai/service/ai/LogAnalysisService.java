   package org.example.java_ai.service.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogAnalysisService {

    private final ChatLanguageModel chatModel;

    private static final String LOG_DIR = "./logs";
    private static final Pattern ERROR_LINE = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[^ ]*)\\s+(ERROR|WARN|FATAL)\\s+(\\d+)\\s+---\\s+\\[.+?\\]\\s+\\[.+?\\]\\s+(.+)");
    private static final Pattern EXCEPTION_LINE = Pattern.compile(
            "([a-z]+(?:\\.[a-z]+){2,}): (.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern STACK_LINE = Pattern.compile(
            "\\s+at\\s+([\\w.$]+)\\(([\\w.]+):(\\d+)\\)");

    private static final int MAX_SAMPLES_PER_TYPE = 3;
    private static final int MAX_CONTEXT_LINES = 20;

    public record LogError(String timestamp, String level, String message,
                           String exceptionType, String stackSample, String codeLocation, int count) {}

    public record AnalysisResult(String rootCause, String affectedComponent,
                                 String severity, String fixSuggestion, String codeReference) {}

    /** Extract recent errors from log files (read-only) */
    public List<LogError> extractErrors(int maxErrors) {
        List<LogError> errors = new ArrayList<>();
        Path logPath = Paths.get(LOG_DIR);

        if (!Files.exists(logPath) || !Files.isDirectory(logPath)) {
            log.warn("Log dir {} not found", LOG_DIR);
            return errors;
        }

        try {
            // Only scan the 2 most recent .log files
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            File[] logFiles = logPath.toFile().listFiles((dir, name) ->
                    name.endsWith(".log") && !name.endsWith(".gz"));

            if (logFiles != null) {
                Arrays.sort(logFiles, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                for (int i = 0; i < Math.min(2, logFiles.length) && errors.size() < maxErrors; i++) {
                    try {
                        scanLogFile(logFiles[i].toPath(), errors, maxErrors);
                    } catch (IOException e) {
                        log.warn("Failed to scan {}: {}", logFiles[i].getName(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Log scan failed", e);
        }

        return errors;
    }

    private void scanLogFile(Path file, List<LogError> errors, int maxErrors) throws IOException {
        List<String> lines = Files.readAllLines(file);
        for (int i = 0; i < lines.size() && errors.size() < maxErrors; i++) {
            String line = lines.get(i);
            Matcher m = ERROR_LINE.matcher(line);
            if (m.find()) {
                String timestamp = m.group(1);
                String level = m.group(2);
                String message = m.group(4);

                // Collect next few lines for stack sample
                StringBuilder stack = new StringBuilder(message);
                String exceptionType = "Unknown";
                String codeLocation = "";

                for (int j = i + 1; j < Math.min(i + MAX_CONTEXT_LINES, lines.size()); j++) {
                    String next = lines.get(j).trim();
                    if (next.isEmpty()) break;

                    // Skip INFO/DEBUG interleaving
                    if (next.startsWith("202") && next.contains(" INFO ")) break;

                    stack.append("\n").append(next);

                    // Extract exception type from caused-by or first exception class
                    Matcher em = EXCEPTION_LINE.matcher(next);
                    if (em.find() && exceptionType.equals("Unknown")) {
                        exceptionType = em.group(1).substring(em.group(1).lastIndexOf('.') + 1);
                    }

                    // Extract code location (our project classes only)
                    Matcher sl = STACK_LINE.matcher(next);
                    if (sl.find() && sl.group(1).startsWith("org.example.java_ai")) {
                        if (codeLocation.isEmpty()) {
                            codeLocation = sl.group(1) + "(" + sl.group(2) + ":" + sl.group(3) + ")";
                        }
                    }
                }

                errors.add(new LogError(timestamp, level, message, exceptionType,
                        stack.toString().substring(0, Math.min(1000, stack.length())),
                        codeLocation, 1));
            }
        }
    }

    /** Deduplicate errors by exception type summary */
    public Map<String, List<LogError>> deduplicateErrors(List<LogError> errors) {
        Map<String, List<LogError>> groups = new LinkedHashMap<>();
        for (LogError e : errors) {
            String key = e.exceptionType() + ":" + e.message().substring(0, Math.min(80, e.message().length()));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }
        return groups;
    }

    /** AI root cause analysis (with rule-based fallback) */
    public AnalysisResult analyzeRootCause(LogError error) {
        // First try rule-based analysis for known patterns
        AnalysisResult ruleResult = ruleBasedAnalysis(error);
        if (ruleResult != null) return ruleResult;

        // Fall through to AI
        String prompt = buildAnalysisPrompt(error);
        try {
            String response = chatModel.generate(prompt);
            return parseAnalysisResponse(response);
        } catch (Exception e) {
            log.warn("AI analysis unavailable (API key not configured), using generic fallback");
            return new AnalysisResult(
                    error.message().length() > 150 ? error.message().substring(0, 150) : error.message(),
                    error.codeLocation().isEmpty() ? "See stack trace" : error.codeLocation(),
                    error.level().equals("ERROR") ? "MEDIUM" : "LOW",
                    "Check the component logs for more details. Enable AI API key for automated root cause analysis.",
                    error.codeLocation()
            );
        }
    }

    private AnalysisResult ruleBasedAnalysis(LogError error) {
        String msg = error.message();
        String stack = error.stackSample();

        // Nacos connection failure
        String lowerMsg = msg.toLowerCase();
        String lowerStack = stack.toLowerCase();
        if (lowerMsg.contains("nacos") || lowerStack.contains("nacos")
                || lowerStack.contains("grpcclient") && stack.contains("9848")) {
            return new AnalysisResult(
                "Nacos service discovery client cannot reach Nacos server at 127.0.0.1:8848",
                "Nacos client — service registration/discovery module",
                "LOW",
                "Disable Nacos auto-config in application.yml: spring.cloud.nacos.discovery.enabled=false, or start a Nacos server",
                error.codeLocation()
            );
        }

        // Database connection failure
        if (msg.contains("CommunicationsException") || msg.contains("CannotGetJdbcConnection")
                || msg.contains("Connection refused") && (msg.contains("mysql") || msg.contains("3306"))) {
            return new AnalysisResult(
                "Database connection refused — MySQL is not running or unreachable at configured host:port",
                "JDBC connection pool (HikariCP → MySQL)",
                "CRITICAL",
                "Start MySQL server and verify connection settings in application.yml: spring.datasource.url, DB_HOST, DB_PORT",
                error.codeLocation()
            );
        }

        // HikariCP pool issues
        if (msg.contains("HikariPool") || msg.contains("hikari")) {
            return new AnalysisResult(
                "HikariCP connection pool issue — thread starvation or clock leap detected, usually caused by long GC pause or system sleep",
                "HikariCP connection pool",
                "LOW",
                "Monitor JVM GC pauses, increase maximum-pool-size if needed, ensure system time is synced",
                error.codeLocation()
            );
        }

        // Timeout
        if (msg.contains("TimeoutException") || msg.contains("timeout") || msg.contains("timed out")) {
            return new AnalysisResult(
                "Operation timed out — the target service did not respond within the configured timeout",
                "External service call or DB query",
                "MEDIUM",
                "Increase timeout settings, check network latency, verify the target service is responsive",
                error.codeLocation()
            );
        }

        // OutOfMemoryError
        if (msg.contains("OutOfMemoryError") || stack.contains("OutOfMemoryError")) {
            return new AnalysisResult(
                "JVM out of memory — heap space exhausted",
                "JVM / Application memory",
                "CRITICAL",
                "Analyze heap dump, increase -Xmx, check for memory leaks in recent changes",
                error.codeLocation()
            );
        }

        // NullPointerException
        if (stack.contains("NullPointerException") || msg.contains("NullPointerException")) {
            return new AnalysisResult(
                "Null pointer dereference — code attempted to use a null reference",
                error.codeLocation().isEmpty() ? "See trace" : error.codeLocation(),
                "HIGH",
                "Add null check at the code location shown in the stack trace",
                error.codeLocation()
            );
        }

        // AI API key
        if (msg.contains("invalid_api_key") || msg.contains("Incorrect API key")) {
            return new AnalysisResult(
                "Invalid AI API key — the configured API key is rejected by the AI provider",
                "AI integration — LangChain4j chat/embedding model",
                "MEDIUM",
                "Set a valid AI_API_KEY environment variable or update spring.app.ai.api-key in application.yml",
                error.codeLocation()
            );
        }

        return null; // No rule matched
    }

    private String buildAnalysisPrompt(LogError error) {
        return """
            You are an expert Java error analyst. Analyze this error log and respond in the EXACT format below.

            Error timestamp: %s
            Error level: %s
            Error message: %s
            Exception type: %s
            Stack trace (abbreviated):
            %s

            Respond EXACTLY in this format with 4 lines:
            ROOT_CAUSE: <one sentence describing what happened and why>
            AFFECTED: <which component/module is affected>
            SEVERITY: <CRITICAL|HIGH|MEDIUM|LOW>
            FIX: <one sentence with the specific fix action>

            Example:
            ROOT_CAUSE: Nacos service discovery is configured but no Nacos server running on localhost:8848
            AFFECTED: Service registration / discovery (Nacos client)
            SEVERITY: LOW
            FIX: Either start Nacos server or disable Nacos auto-config in application.yml
            """.formatted(error.timestamp(), error.level(), error.message(),
                    error.exceptionType(), error.stackSample());
    }

    private AnalysisResult parseAnalysisResponse(String response) {
        String rootCause = extractField(response, "ROOT_CAUSE");
        String affected = extractField(response, "AFFECTED");
        String severity = extractField(response, "SEVERITY");
        String fix = extractField(response, "FIX");

        return new AnalysisResult(
                rootCause.isEmpty() ? "Unable to parse AI response" : rootCause,
                affected.isEmpty() ? "Unknown" : affected,
                severity.isEmpty() ? "MEDIUM" : severity,
                fix.isEmpty() ? "Review error manually" : fix,
                ""
        );
    }

    private String extractField(String text, String fieldName) {
        Pattern p = Pattern.compile("^" + fieldName + ":\\s*(.+)$", Pattern.MULTILINE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : "";
    }
}
