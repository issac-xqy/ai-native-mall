package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.UserAddress;
import org.example.java_ai.service.UserAddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户地址控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class UserAddressController {

    private final UserAddressService addressService;

    /**
     * 获取用户地址列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> listAddresses(HttpServletRequest servletRequest) {
        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            List<UserAddress> addresses = addressService.listByUserId(userId);
            return ResponseEntity.ok(Map.of("success", true, "data", addresses));
        } catch (Exception e) {
            log.error("获取地址列表失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "获取失败: " + e.getMessage()));
        }
    }

    /**
     * 添加地址
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addAddress(
            HttpServletRequest servletRequest,
            @RequestBody UserAddress address) {
        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            address.setUserId(userId);
            UserAddress created = addressService.addAddress(address);
            return ResponseEntity.ok(Map.of("success", true, "data", created, "message", "添加成功"));
        } catch (Exception e) {
            log.error("添加地址失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "添加失败: " + e.getMessage()));
        }
    }

    /**
     * 更新地址
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAddress(
            HttpServletRequest servletRequest,
            @PathVariable Long id,
            @RequestBody UserAddress address) {
        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            address.setId(id);
            address.setUserId(userId);
            UserAddress updated = addressService.updateAddress(address);
            return ResponseEntity.ok(Map.of("success", true, "data", updated, "message", "更新成功"));
        } catch (Exception e) {
            log.error("更新地址失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "更新失败: " + e.getMessage()));
        }
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/{id}/default")
    public ResponseEntity<Map<String, Object>> setDefault(
            HttpServletRequest servletRequest,
            @PathVariable Long id) {
        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            boolean success = addressService.setDefault(id, userId);
            return ResponseEntity.ok(Map.of("success", success, "message", success ? "设置成功" : "设置失败"));
        } catch (Exception e) {
            log.error("设置默认地址失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "设置失败: " + e.getMessage()));
        }
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteAddress(
            HttpServletRequest servletRequest,
            @PathVariable Long id) {
        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.ok(Map.of("success", false, "message", "未登录"));
            }
            boolean success = addressService.deleteAddress(id, userId);
            return ResponseEntity.ok(Map.of("success", success, "message", success ? "删除成功" : "删除失败"));
        } catch (Exception e) {
            log.error("删除地址失败", e);
            return ResponseEntity.ok(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }
}
