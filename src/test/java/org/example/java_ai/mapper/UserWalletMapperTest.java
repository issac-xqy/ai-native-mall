package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.example.java_ai.entity.UserWallet;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@MybatisPlusTest(excludeAutoConfiguration = org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("UserWalletMapper 单元测试")
class UserWalletMapperTest {

    @Autowired
    private UserWalletMapper walletMapper;

    @Test
    @DisplayName("insert+selectById-创建钱包后能查到")
    void insertAndSelectById() {
        UserWallet w = new UserWallet();
        w.setUserId(1L);
        w.setBalance(new BigDecimal("1000.00"));
        w.setTotalRecharge(new BigDecimal("5000.00"));
        w.setTotalSpent(new BigDecimal("4000.00"));
        walletMapper.insert(w);

        UserWallet found = walletMapper.selectById(w.getId());
        assertNotNull(found);
        assertEquals(0, new BigDecimal("1000.00").compareTo(found.getBalance()));
    }

    @Test
    @DisplayName("updateById-修改余额-更新成功")
    void updateBalance() {
        UserWallet w = new UserWallet();
        w.setUserId(2L);
        w.setBalance(new BigDecimal("500.00"));
        walletMapper.insert(w);

        w.setBalance(new BigDecimal("800.00"));
        walletMapper.updateById(w);

        UserWallet updated = walletMapper.selectById(w.getId());
        assertEquals(0, new BigDecimal("800.00").compareTo(updated.getBalance()));
    }

    @Test
    @DisplayName("deleteById-逻辑删除-查不到")
    void logicDelete() {
        UserWallet w = new UserWallet();
        w.setUserId(3L);
        w.setBalance(BigDecimal.ZERO);
        walletMapper.insert(w);

        walletMapper.deleteById(w.getId());
        assertNull(walletMapper.selectById(w.getId()));
    }
}
