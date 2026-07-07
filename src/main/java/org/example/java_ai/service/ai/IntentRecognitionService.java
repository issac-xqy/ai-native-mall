package org.example.java_ai.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 意图识别与实体抽取服务
 * 
 * 使用大模型进行语义理解，识别用户意图并提取关键实体
 * 
 * @author xqy
 * @since 2026-04-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentRecognitionService {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * 意图枚举
     */
    public enum Intent {
        PRODUCT_INQUIRY,      // 商品咨询
        PRICE_INQUIRY,        // 询价
        BARGAINING,           // 议价
        ORDER_STATUS,         // 订单查询
        RETURN_POLICY,        // 退换货政策
        LOGISTICS_TRACKING,   // 物流追踪
        INVOICE_REQUEST,      // 发票申请
        COUPON_INQUIRY,       // 优惠券咨询
        CHITCHAT,             // 闲聊
        UNKNOWN               // 未知意图
    }

    /**
     * 识别用户意图
     * 
     * @param message 用户消息
     * @return 意图类型
     */
    public Intent classifyIntent(String message) {
        try {
            String prompt = buildIntentClassificationPrompt(message);
            String result = chatModel.generate(prompt).trim().toUpperCase();
            
            log.info("意图识别 - 消息: {}, 结果: {}", message, result);
            
            // 解析意图
            for (Intent intent : Intent.values()) {
                if (result.contains(intent.name())) {
                    return intent;
                }
            }
            
            return Intent.UNKNOWN;
        } catch (Exception e) {
            log.error("意图识别失败", e);
            return Intent.UNKNOWN;
        }
    }

    /**
     * 提取实体信息
     * 
     * @param message 用户消息
     * @param intent  已识别的意图
     * @return 实体Map
     */
    public Map<String, String> extractEntities(String message, Intent intent) {
        try {
            String prompt = buildEntityExtractionPrompt(message, intent);
            String result = chatModel.generate(prompt);
            
            log.info("实体抽取 - 意图: {}, 结果: {}", intent, result);
            
            // 解析JSON格式的实体
            return parseEntitiesFromJson(result);
        } catch (Exception e) {
            log.error("实体抽取失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 综合识别：同时返回意图和实体
     * 
     * @param message 用户消息
     * @return 识别结果 {intent, entities}
     */
    public Map<String, Object> recognize(String message) {
        Intent intent = classifyIntent(message);
        Map<String, String> entities = extractEntities(message, intent);
        
        Map<String, Object> result = new HashMap<>();
        result.put("intent", intent.name());
        result.put("entities", entities);
        
        return result;
    }

    /**
     * 构建意图分类提示词
     */
    private String buildIntentClassificationPrompt(String message) {
        return """
            你是一个电商客服意图识别助手。请分析以下用户消息，将其分类为以下意图之一：
            
            意图列表：
            - PRODUCT_INQUIRY: 询问商品信息（如"这个有什么特点"、"有红色吗"）
            - PRICE_INQUIRY: 询问价格（如"多少钱"、"什么价位"）
            - BARGAINING: 讨价还价（如"能便宜点吗"、"有没有优惠"）
            - ORDER_STATUS: 查询订单状态（如"我的订单到哪了"、"订单12345"）
            - RETURN_POLICY: 退换货政策（如"怎么退货"、"能换货吗"）
            - LOGISTICS_TRACKING: 物流追踪（如"快递到哪了"、"什么时候到"）
            - INVOICE_REQUEST: 发票申请（如"开发票"、"要发票"）
            - COUPON_INQUIRY: 优惠券咨询（如"有优惠券吗"、"怎么用券"）
            - CHITCHAT: 闲聊问候（如"你好"、"谢谢"）
            - UNKNOWN: 无法识别
            
            用户消息: "%s"
            
            只返回意图名称（大写），不要有其他内容。
            意图:
            """.formatted(message);
    }

    /**
     * 构建实体抽取提示词
     */
    private String buildEntityExtractionPrompt(String message, Intent intent) {
        String entityDescription = switch (intent) {
            case PRODUCT_INQUIRY -> "商品名称(product)、颜色(color)、尺码(size)、品牌(brand)";
            case PRICE_INQUIRY -> "商品名称(product)";
            case ORDER_STATUS, LOGISTICS_TRACKING -> "订单号(orderId)";
            case RETURN_POLICY -> "商品名称(product)、原因(reason)";
            case INVOICE_REQUEST -> "订单号(orderId)、发票类型(invoiceType: 电子/纸质)";
            default -> "无特定实体";
        };

        return """
            你是一个电商客服实体抽取助手。请从用户消息中提取以下实体信息：
            
            需要提取的实体: %s
            
            用户消息: "%s"
            
            请以JSON格式返回提取的实体，如果某个实体不存在则设为null。
            示例输出: {"product": "红色衬衫", "color": "红色", "size": null}
            
            输出:
            """.formatted(entityDescription, message);
    }

    /**
     * 从JSON字符串解析实体
     */
    private Map<String, String> parseEntitiesFromJson(String json) {
        try {
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                Map<String, Object> raw = objectMapper.readValue(json,
                        new TypeReference<Map<String, Object>>() {});
                Map<String, String> entities = new HashMap<>();
                for (var entry : raw.entrySet()) {
                    Object val = entry.getValue();
                    if (val != null && !"null".equals(String.valueOf(val))
                            && !String.valueOf(val).isEmpty()) {
                        entities.put(entry.getKey(), String.valueOf(val));
                    }
                }
                return entities;
            }
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", json, e);
        }
        return new HashMap<>();
    }
}
