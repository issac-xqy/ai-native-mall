package org.example.java_ai.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel限流熔断配置
 * 
 * @author xqy
 * @since 2026-04-10
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /**
     * 配置Sentinel注解切面
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * 初始化限流规则
     */
    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();
        
        // AI接口限流：QPS = 20
        FlowRule aiFlowRule = new FlowRule();
        aiFlowRule.setResource("ai-api");
        aiFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        aiFlowRule.setCount(20);
        aiFlowRule.setLimitApp("default");
        rules.add(aiFlowRule);

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel限流规则初始化完成 (ai-api QPS=20)");
    }

    /**
     * 初始化熔断降级规则
     */
    @PostConstruct
    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();
        
        // AI接口熔断：RT超过3000ms熔断10秒
        DegradeRule aiDegradeRule = new DegradeRule();
        aiDegradeRule.setResource("ai-api");
        aiDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        aiDegradeRule.setCount(3000); // RT阈值：3000ms
        aiDegradeRule.setTimeWindow(10); // 熔断时长：10秒
        aiDegradeRule.setMinRequestAmount(5); // 最小请求数
        aiDegradeRule.setStatIntervalMs(1000); // 统计时长：1秒
        rules.add(aiDegradeRule);

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel熔断规则初始化完成 (ai-api RT>3000ms → 熔断10s)");
    }
}
