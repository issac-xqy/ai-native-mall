package org.example.java_ai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DataMaskingUtil 数据脱敏单元测试")
class DataMaskingUtilTest {

    // ==================== maskPhone ====================

    @Test
    @DisplayName("maskPhone-标准手机号-中间4位脱敏")
    void maskPhone_ValidPhone_MasksMiddleFour() {
        String masked = DataMaskingUtil.maskPhone("13812345678");

        assertEquals("138****5678", masked);
    }

    @Test
    @DisplayName("maskPhone-null输入-返回null")
    void maskPhone_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskPhone(null));
    }

    @Test
    @DisplayName("maskPhone-短字符串(少于7位)-原样返回")
    void maskPhone_TooShort_ReturnsOriginal() {
        String shortPhone = "13812";

        String result = DataMaskingUtil.maskPhone(shortPhone);

        assertEquals("13812", result);
    }

    // ==================== maskIdCard ====================

    @Test
    @DisplayName("maskIdCard-标准身份证-保留前3后4")
    void maskIdCard_ValidIdCard_MasksMiddle() {
        String masked = DataMaskingUtil.maskIdCard("110101199001011234");

        assertEquals("110***********1234", masked);
    }

    @Test
    @DisplayName("maskIdCard-null输入-返回null")
    void maskIdCard_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskIdCard(null));
    }

    @Test
    @DisplayName("maskIdCard-短字符串(少于10位)-原样返回")
    void maskIdCard_TooShort_ReturnsOriginal() {
        String shortId = "123456789";

        String result = DataMaskingUtil.maskIdCard(shortId);

        assertEquals("123456789", result);
    }

    // ==================== maskName ====================

    @Test
    @DisplayName("maskName-中文名-只显示姓")
    void maskName_ChineseName_MasksGivenName() {
        String masked = DataMaskingUtil.maskName("张三");

        assertEquals("张*", masked);
    }

    @Test
    @DisplayName("maskName-null输入-返回null")
    void maskName_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskName(null));
    }

    @Test
    @DisplayName("maskName-空字符串-返回空字符串")
    void maskName_Empty_ReturnsEmpty() {
        assertEquals("", DataMaskingUtil.maskName(""));
    }

    // ==================== maskEmail ====================

    @Test
    @DisplayName("maskEmail-标准邮箱-用户名部分脱敏")
    void maskEmail_ValidEmail_MasksUsername() {
        String masked = DataMaskingUtil.maskEmail("zhangsan@gmail.com");

        assertEquals("z*******@gmail.com", masked);
    }

    @Test
    @DisplayName("maskEmail-null输入-返回null")
    void maskEmail_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskEmail(null));
    }

    @Test
    @DisplayName("maskEmail-无@符号-原样返回")
    void maskEmail_NoAtSign_ReturnsOriginal() {
        String notEmail = "notAnEmail";

        String result = DataMaskingUtil.maskEmail(notEmail);

        assertEquals("notAnEmail", result);
    }

    // ==================== maskAddress ====================

    @Test
    @DisplayName("maskAddress-标准地址-保留前6字符其余脱敏")
    void maskAddress_ValidAddress_MasksDetail() {
        String masked = DataMaskingUtil.maskAddress("北京市朝阳区建国路123号");

        assertEquals("北京市朝阳区****", masked);
    }

    @Test
    @DisplayName("maskAddress-null输入-返回null")
    void maskAddress_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskAddress(null));
    }

    @Test
    @DisplayName("maskAddress-短地址(少于6位)-原样返回")
    void maskAddress_TooShort_ReturnsOriginal() {
        String shortAddr = "北京";

        String result = DataMaskingUtil.maskAddress(shortAddr);

        assertEquals("北京", result);
    }

    // ==================== maskBankCard ====================

    @Test
    @DisplayName("maskBankCard-标准银行卡号-只保留前后4位")
    void maskBankCard_ValidBankCard_MasksMiddle() {
        String masked = DataMaskingUtil.maskBankCard("6222021234567890123");

        assertEquals("6222 **** **** **** 123", masked);
    }

    @Test
    @DisplayName("maskBankCard-null输入-返回null")
    void maskBankCard_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskBankCard(null));
    }

    @Test
    @DisplayName("maskBankCard-短卡号(少于8位)-原样返回")
    void maskBankCard_TooShort_ReturnsOriginal() {
        String shortCard = "622202";

        String result = DataMaskingUtil.maskBankCard(shortCard);

        assertEquals("622202", result);
    }

    // ==================== maskPassword ====================

    @Test
    @DisplayName("maskPassword-任意密码-返回固定星号")
    void maskPassword_AnyPassword_ReturnsStars() {
        assertEquals("******", DataMaskingUtil.maskPassword("admin123"));
        assertEquals("******", DataMaskingUtil.maskPassword("P@ssw0rd!"));
        assertEquals("******", DataMaskingUtil.maskPassword(""));
    }

    // ==================== maskIp ====================

    @Test
    @DisplayName("maskIp-标准IPv4-最后一段脱敏")
    void maskIp_ValidIp_MasksLastSegment() {
        String masked = DataMaskingUtil.maskIp("192.168.1.100");

        assertEquals("192.168.1.*", masked);
    }

    @Test
    @DisplayName("maskIp-null输入-返回null")
    void maskIp_Null_ReturnsNull() {
        assertNull(DataMaskingUtil.maskIp(null));
    }

    @Test
    @DisplayName("maskIp-无点号-原样返回")
    void maskIp_NoDot_ReturnsOriginal() {
        String notIp = "localhost";

        String result = DataMaskingUtil.maskIp(notIp);

        assertEquals("localhost", result);
    }

    // ==================== maskUserInfo ====================

    @Test
    @DisplayName("maskUserInfo-综合脱敏-正确格式化")
    void maskUserInfo_ValidInputs_FormatsCorrectly() {
        String result = DataMaskingUtil.maskUserInfo("张三", "13812345678", "北京市朝阳区建国路");

        assertEquals("用户[张*, 138****5678, 北京市朝阳区****]", result);
    }

    // ==================== maskOrderInfo ====================

    @Test
    @DisplayName("maskOrderInfo-订单脱敏-金额不脱敏便于对账")
    void maskOrderInfo_ValidInputs_MasksUserButNotAmount() {
        String result = DataMaskingUtil.maskOrderInfo("ORD2026001", "李四", "13900001111", "9999.00");

        assertTrue(result.contains("ORD2026001"));
        assertTrue(result.contains("李*"));
        assertTrue(result.contains("139****1111"));
        assertTrue(result.contains("9999.00")); // 金额不脱敏
    }
}
