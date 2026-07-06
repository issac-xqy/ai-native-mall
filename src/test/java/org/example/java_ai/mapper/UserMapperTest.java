package org.example.java_ai.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.example.java_ai.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.*;

@MybatisPlusTest(excludeAutoConfiguration = org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DisplayName("UserMapper 单元测试")
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    private User createAndSave(String username, String nickname, String phone) {
        User user = new User();
        user.setUsername(username);
        user.setPassword("pwd_encrypted");
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(username + "@test.com");
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    // ==================== 基础 CRUD ====================

    @Test
    @DisplayName("insert+selectById-插入后能查到-字段正确")
    void insertAndSelectById_RoundTrip() {
        User saved = createAndSave("testuser", "测试用户", "13800138888");

        User found = userMapper.selectById(saved.getId());

        assertNotNull(found);
        assertEquals("testuser", found.getUsername());
        assertEquals("测试用户", found.getNickname());
        assertEquals("13800138888", found.getPhone());
    }

    @Test
    @DisplayName("selectById-不存在的ID-返回null")
    void selectById_NonExistentId_ReturnsNull() {
        assertNull(userMapper.selectById(99999L));
    }

    @Test
    @DisplayName("selectOne-按用户名查询-返回唯一用户")
    void selectOne_ByUsername_ReturnsCorrectUser() {
        createAndSave("findme", "查找目标", "13800000001");

        User found = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "findme"));

        assertNotNull(found);
        assertEquals("查找目标", found.getNickname());
    }

    @Test
    @DisplayName("selectOne-不存在的用户名-返回null")
    void selectOne_NonExistentUsername_ReturnsNull() {
        assertNull(userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "no_such_user")));
    }

    @Test
    @DisplayName("updateById-修改昵称-更新成功")
    void updateById_ChangeNickname_UpdatesSuccessfully() {
        User saved = createAndSave("updateme", "旧昵称", "13800000002");

        saved.setNickname("新昵称");
        userMapper.updateById(saved);

        User updated = userMapper.selectById(saved.getId());
        assertEquals("新昵称", updated.getNickname());
    }

    // ==================== 逻辑删除 ====================

    @Test
    @DisplayName("deleteById-逻辑删除-删后selectById返回null")
    void deleteById_LogicDelete_SelectReturnsNull() {
        User saved = createAndSave("delete_me", "待删除", "13800000003");

        int rows = userMapper.deleteById(saved.getId());
        assertEquals(1, rows);

        assertNull(userMapper.selectById(saved.getId()));
    }

    @Test
    @DisplayName("deleteById-删除不存在的记录-返回0")
    void deleteById_NonExistentId_ReturnsZero() {
        assertEquals(0, userMapper.deleteById(99999L));
    }

    // ==================== count ====================

    @Test
    @DisplayName("selectCount-统计未删除用户-数量>=1")
    void selectCount_TotalUndeleted_GteOne() {
        createAndSave("count_user", "统计用", "13800000004");

        long count = userMapper.selectCount(null);

        assertTrue(count >= 1);
    }

    @Test
    @DisplayName("selectCount-phone is null 条件-计入无电话用户")
    void selectCount_PhoneIsNull_CountsCorrectly() {
        User noPhone = new User();
        noPhone.setUsername("nophone");
        noPhone.setPassword("pwd");
        noPhone.setNickname("无电话");
        noPhone.setStatus(1);
        userMapper.insert(noPhone);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNull(User::getPhone);

        long count = userMapper.selectCount(wrapper);
        assertTrue(count >= 1);
    }
}
