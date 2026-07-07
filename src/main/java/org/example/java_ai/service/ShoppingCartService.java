package org.example.java_ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import lombok.extern.slf4j.Slf4j;
import org.example.java_ai.entity.ShoppingCart;
import org.example.java_ai.mapper.ShoppingCartMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingCartService extends ServiceImpl<ShoppingCartMapper, ShoppingCart> {

    private final ShoppingCartMapper cartMapper;

    @Cacheable(value = "cart", key = "#userId")
    public List<Map<String, Object>> getCartItems(Long userId) {
        return cartMapper.selectCartWithProduct(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "cart", key = "#userId")
    public Map<String, Object> addToCart(Long userId, Long productId, Integer quantity) {
        ShoppingCart existing = getOne(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId)
                .eq(ShoppingCart::getProductId, productId));

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + (quantity != null ? quantity : 1));
            updateById(existing);
            log.info("购物车更新: userId={}, productId={}, qty={}", userId, productId, existing.getQuantity());
            return Map.of("message", "已更新数量", "id", existing.getId(), "quantity", existing.getQuantity());
        }

        ShoppingCart cart = new ShoppingCart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(quantity != null ? quantity : 1);
        save(cart);
        log.info("购物车添加: userId={}, productId={}", userId, productId);
        return Map.of("message", "已加入购物车", "id", cart.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "cart", key = "#userId")
    public boolean updateQuantity(Long userId, Long cartId, Integer quantity) {
        ShoppingCart cart = getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) return false;
        if (quantity <= 0) {
            removeById(cartId);
            return true;
        }
        cart.setQuantity(quantity);
        return updateById(cart);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "cart", key = "#userId")
    public boolean removeFromCart(Long userId, Long cartId) {
        ShoppingCart cart = getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) return false;
        return removeById(cartId);
    }

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "cart", key = "#userId")
    public boolean clearCart(Long userId) {
        return remove(new LambdaQueryWrapper<ShoppingCart>()
                .eq(ShoppingCart::getUserId, userId));
    }
}
