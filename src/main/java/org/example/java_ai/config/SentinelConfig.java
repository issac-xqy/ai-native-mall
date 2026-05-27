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

import javax.annotation.PostConstruct;
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
        
        // 商品接口限流：QPS = 100
        FlowRule productFlowRule = new FlowRule();
        productFlowRule.setResource("product-api");
        productFlowRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        productFlowRule.setCount(100);
        productFlowRule.setLimitApp("default");
        rules.add(productFlowRule);
        
        FlowRuleManager.loadRules(rules);
        log.info("Sentinel限流规则初始化完成");
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
        
        // 商品接口熔断：RT超过1000ms熔断5秒
        DegradeRule productDegradeRule = new DegradeRule();
        productDegradeRule.setResource("product-api");
        productDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        productDegradeRule.setCount(1000);
        productDegradeRule.setTimeWindow(5);
        productDegradeRule.setMinRequestAmount(5);
        productDegradeRule.setStatIntervalMs(1000);
        rules.add(productDegradeRule);
        
        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel熔断规则初始化完成");
    }
}
