package org.example.java_ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.common.Result;
import org.example.java_ai.service.ShoppingCartService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCartService cartService;

    @GetMapping
    public Result<?> getCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        return Result.success(cartService.getCartItems(userId));
    }

    @PostMapping
    public Result<Map<String, Object>> addToCart(HttpServletRequest request,
            @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        Long productId = ((Number) body.get("productId")).longValue();
        Integer quantity = body.containsKey("quantity") ? ((Number) body.get("quantity")).intValue() : 1;
        return Result.success(cartService.addToCart(userId, productId, quantity));
    }

    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateQuantity(HttpServletRequest request,
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        Integer quantity = ((Number) body.get("quantity")).intValue();
        boolean ok = cartService.updateQuantity(userId, id, quantity);
        return ok ? Result.success(Map.of("message", "更新成功"))
                : Result.error("无权限或购物车项不存在");
    }

    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> removeFromCart(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        boolean ok = cartService.removeFromCart(userId, id);
        return ok ? Result.success(Map.of("message", "已移除"))
                : Result.error("无权限或购物车项不存在");
    }

    @DeleteMapping("/clear")
    public Result<Map<String, Object>> clearCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("未登录");
        cartService.clearCart(userId);
        return Result.success(Map.of("message", "购物车已清空"));
    }
}
