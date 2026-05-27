package org.example.java_ai.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI接口调用监控切面
 * 自动记录AI调用的响应时间、成功率等指标
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMonitorAspect {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 记录AI调用日志
     */
    public void recordAiCall(String sessionId, String userId, String question, 
                            String answer, long responseTime, boolean isError, 
                            String errorType, boolean isMissingKnowledge) {
        try {
            // 计算响应等级
            String responseLevel;
            if (responseTime < 10000) {
                responseLevel = "green";
            } else if (responseTime < 20000) {
                responseLevel = "orange";
            } else {
                responseLevel = "red";
            }

            String sql = """
                INSERT INTO ai_monitor_log 
                (session_id, user_id, question, answer, response_time, response_level, 
                 is_error, error_type, is_missing_knowledge, create_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
                """;

            jdbcTemplate.update(sql, 
                sessionId, 
                userId != null ? userId : "anonymous",
                question != null ? question.substring(0, Math.min(question.length(), 500)) : null,
                answer != null ? answer.substring(0, Math.min(answer.length(), 2000)) : null,
                responseTime,
                responseLevel,
                isError ? 1 : 0,
                errorType,
                isMissingKnowledge ? 1 : 0
            );

            log.debug("AI监控记录成功 - 用户: {}, 响应时间: {}ms, 等级: {}", userId, responseTime, responseLevel);
        } catch (Exception e) {
            log.error("记录AI监控日志失败", e);
        }
    }

    /**
     * 获取统计指标（优化版：减少数据库查询次数）
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 一次性查询所有基础统计数据
            String baseStatsSql = """
                SELECT 
                    COUNT(*) as total_count,
                    SUM(CASE WHEN is_error = 1 THEN 1 ELSE 0 END) as error_count,
                    SUM(CASE WHEN is_missing_knowledge = 1 THEN 1 ELSE 0 END) as missing_knowledge_count,
                    AVG(CASE WHEN is_error = 0 THEN response_time ELSE NULL END) as avg_response_time,
                    SUM(CASE WHEN response_level = 'green' AND is_error = 0 THEN 1 ELSE 0 END) as green_count,
                    SUM(CASE WHEN response_level = 'orange' AND is_error = 0 THEN 1 ELSE 0 END) as orange_count,
                    SUM(CASE WHEN response_level = 'red' AND is_error = 0 THEN 1 ELSE 0 END) as red_count
                FROM ai_monitor_log 
                WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                """;
            
            var baseStats = jdbcTemplate.queryForMap(baseStatsSql);
            
            Long totalCount = ((Number) baseStats.get("total_count")).longValue();
            Long errorCount = ((Number) baseStats.getOrDefault("error_count", 0)).longValue();
            Long missingKnowledgeCount = ((Number) baseStats.getOrDefault("missing_knowledge_count", 0)).longValue();
            Object avgResponseTimeObj = baseStats.get("avg_response_time");
            Double avgResponseTime = avgResponseTimeObj != null ? ((Number) avgResponseTimeObj).doubleValue() : null;
            Long greenCount = ((Number) baseStats.getOrDefault("green_count", 0)).longValue();
            Long orangeCount = ((Number) baseStats.getOrDefault("orange_count", 0)).longValue();
            Long redCount = ((Number) baseStats.getOrDefault("red_count", 0)).longValue();
            
            // 今日会话数（单独查询，因为时间范围不同）
            Long todayCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ai_monitor_log WHERE DATE(create_time) = CURDATE()", Long.class);
            stats.put("todayConversations", todayCount != null ? todayCount : 0);
            
            // 近7天平均会话数
            Double avgConversations = jdbcTemplate.queryForObject(
                "SELECT AVG(daily_count) FROM (SELECT COUNT(*) as daily_count FROM ai_monitor_log WHERE create_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) GROUP BY DATE(create_time)) as daily", 
                Double.class);
            stats.put("avgConversations7Days", avgConversations != null ? Math.round(avgConversations) : 0);
            
            // 响应时间分布
            stats.put("responseTimeGreen", greenCount);
            stats.put("responseTimeOrange", orangeCount);
            stats.put("responseTimeRed", redCount);
            
            // 平均响应时间
            stats.put("avgResponseTime", avgResponseTime != null ? Math.round(avgResponseTime) : 0);
            
            // 错误率
            double errorRate = totalCount > 0 ? errorCount * 100.0 / totalCount : 0;
            stats.put("errorRate", Math.round(errorRate * 100.0) / 100.0);
            
            // 无知识率
            double missingRate = totalCount > 0 ? missingKnowledgeCount * 100.0 / totalCount : 0;
            stats.put("missingKnowledgeRate", Math.round(missingRate * 100.0) / 100.0);
            
            // 24小时会话趋势
            var hourlyData = jdbcTemplate.queryForList("""
                SELECT HOUR(create_time) as hour, COUNT(*) as count 
                FROM ai_monitor_log 
                WHERE create_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                GROUP BY HOUR(create_time)
                ORDER BY hour
                """);
            stats.put("hourlyTrend", hourlyData);
            
            log.debug("AI监控统计查询完成 - 总记录数: {}", totalCount);
            
        } catch (Exception e) {
            log.error("获取AI监控统计数据失败", e);
            // 返回空统计数据，避免前端报错
            stats.put("todayConversations", 0);
            stats.put("avgConversations7Days", 0);
            stats.put("responseTimeGreen", 0);
            stats.put("responseTimeOrange", 0);
            stats.put("responseTimeRed", 0);
            stats.put("avgResponseTime", 0);
            stats.put("errorRate", 0.0);
            stats.put("missingKnowledgeRate", 0.0);
            stats.put("hourlyTrend", new java.util.ArrayList<>());
        }

        return stats;
    }
}
