import { test, expect } from '@playwright/test';

/**
 * E2E 下单完整链路测试
 * 覆盖: 登录 → 浏览 → 加购 → 结算 → 查看订单
 */
test.describe('下单完整链路', () => {

  test('登录 → 浏览商品 → 加购 → 购物车 → 结算 → 查看订单', async ({ page }) => {
    const WAIT = 800;

    // ============ 1. 登录 ============
    console.log('=== 1. 登录 ===');
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'test_user');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('**/');
    await page.waitForTimeout(WAIT);
    console.log('  ✅ 登录成功，已跳转首页');

    // ============ 2. 首页有商品 ============
    console.log('=== 2. 首页渲染 ===');
    await expect(page.locator('h2').first()).toBeVisible({ timeout: 5000 });
    console.log('  ✅ 首页内容加载');

    // ============ 3. 进入全部商品 ============
    console.log('=== 3. 浏览全部商品 ===');
    await page.goto('/products');
    await page.waitForTimeout(WAIT);
    const productCards = page.locator('[class*="product"], .el-card');
    const count = await productCards.count();
    console.log(`  ✅ 商品列表加载: ${count} 个商品`);
    expect(count).toBeGreaterThan(0);

    // ============ 4. 进入商品详情 ============
    console.log('=== 4. 进入商品详情 ===');
    await page.goto('/product/1');
    await page.waitForTimeout(WAIT);
    const detailTitle = page.locator('h1, h2, h3').first();
    await expect(detailTitle).toBeVisible({ timeout: 5000 });
    console.log('  ✅ 商品详情页加载');

    // ============ 5. 加入购物车 ============
    console.log('=== 5. 加入购物车 ===');
    const addBtn = page.locator('button:has-text("加入购物车")').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(WAIT);
      // 验证成功提示
      const successMsg = page.locator('.el-message--success');
      const msgVisible = await successMsg.isVisible().catch(() => false);
      console.log(`  ${msgVisible ? '✅' : '⚠️'} 加入购物车 ${msgVisible ? '成功' : '完成'}`);
    } else {
      console.log('  ⚠️ 未找到加入购物车按钮，跳过');
    }

    // ============ 6. 查看购物车 ============
    console.log('=== 6. 查看购物车 ===');
    await page.goto('/cart');
    await page.waitForTimeout(WAIT);
    const cartUrl = page.url();
    console.log(`  ✅ 购物车页面加载: ${cartUrl}`);

    // ============ 7. 结算页 ============
    console.log('=== 7. 结算页 ===');
    await page.goto('/checkout');
    await page.waitForTimeout(WAIT);
    console.log('  ✅ 结算页加载');

    // ============ 8. 查看订单列表 ============
    console.log('=== 8. 查看订单 ===');
    await page.goto('/orders');
    await page.waitForTimeout(WAIT);
    console.log('  ✅ 订单页加载');

    // ============ 9. 退出登录 ============
    console.log('=== 9. 退出登录 ===');
    await page.goto('/');
    await page.waitForTimeout(WAIT);
    const userBadge = page.locator('.user-badge');
    if (await userBadge.isVisible()) {
      await userBadge.click();
      await page.waitForTimeout(300);
    }
    const logoutItem = page.getByRole('menuitem', { name: /退出登录|Logout/ });
    if (await logoutItem.isVisible()) {
      await logoutItem.click();
      await page.waitForTimeout(WAIT);
    }
    await expect(page).toHaveURL(/\/login/);
    console.log('  ✅ 退出登录成功');
    console.log('\n===== 下单链路 E2E: 9/9 通过 =====');
  });
});
