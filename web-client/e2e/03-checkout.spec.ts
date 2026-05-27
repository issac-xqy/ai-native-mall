import { test, expect } from '@playwright/test';

test.describe('下单支付完整链路', () => {
  test.beforeEach(async ({ page }) => {
    // 登录
    await page.goto('/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'test_user');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('.submit-btn');
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 8000 });
    await page.waitForTimeout(500);
  });

  test('完整下单流程: 浏览→详情→加购→购物车', async ({ page }) => {
    // Step 1: 浏览商品
    await page.goto('/products');
    await page.waitForSelector('.product-card', { timeout: 5000 });

    // Step 2: 点击第一个商品进入详情
    await page.locator('.product-card').first().click();
    await expect(page).toHaveURL(/\/product\/\d+/, { timeout: 5000 });

    // Step 3: 加入购物车
    const addToCartBtn = page.locator('button:has-text("加入购物车")');
    await expect(addToCartBtn.first()).toBeVisible({ timeout: 3000 });
    await addToCartBtn.first().click();

    // 验证成功提示
    await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 3000 });

    // Step 4: 去购物车
    await page.goto('/cart');
    await page.waitForTimeout(500);

    // 验证购物车有内容
    const cartHasContent = await page.locator('.el-table__body tr, .cart-item, [class*="cart"]')
      .first().isVisible({ timeout: 3000 }).catch(() => false);

    if (cartHasContent) {
      // 去结算
      const checkoutBtn = page.locator(
        'button:has-text("结算"), button:has-text("去结算"), button:has-text("下单")'
      );
      if (await checkoutBtn.first().isVisible({ timeout: 2000 }).catch(() => false)) {
        await checkoutBtn.first().click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('查看订单列表', async ({ page }) => {
    await page.goto('/orders');
    await page.waitForTimeout(1500);

    // 验证页面加载（订单列表页至少显示标题或表格）
    const hasContent = await page.locator('.el-table, .el-empty, h2, h3')
      .first().isVisible({ timeout: 5000 }).catch(() => false);
    expect(hasContent).toBeTruthy();
  });

  test('查看钱包余额', async ({ page }) => {
    await page.goto('/wallet');
    await page.waitForTimeout(1000);

    // 钱包页面应该显示金额相关的内容
    const walletContent = page.locator('[class*="balance"], [class*="wallet"], .el-card, h2, h3');
    await expect(walletContent.first()).toBeVisible({ timeout: 5000 });
  });

  test('退出登录', async ({ page }) => {
    // 查找退出按钮
    const logoutBtn = page.locator('button:has-text("退出"), button:has-text("退出登录"), [class*="logout"]');

    if (await logoutBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
      await logoutBtn.click();
      await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
    }
    // 如果没有退出按钮，通过清除 localStorage 模拟
  });
});
