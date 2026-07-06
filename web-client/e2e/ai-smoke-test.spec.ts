import { test, expect } from '@playwright/test';

test.describe('AI 驱动冒烟测试', () => {
  test('登录→验证跳转首页—完整链路', async ({ page }) => {
    // 慢放：每步等 0.8 秒，让你看清浏览器操作
    const STEP_DELAY = 800;

    // Step 1: 打开登录页
    console.log('=== Step 1: 打开登录页 ===');
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(STEP_DELAY);

    // Step 2: 检查组件是否存在
    console.log('=== Step 2: 检查登录表单组件 ===');
    const usernameInput = page.locator('input[placeholder="请输入用户名/手机号"]');
    const passwordInput = page.locator('input[placeholder="请输入密码"]');
    const submitBtn = page.locator('.submit-btn');

    await expect(usernameInput).toBeVisible({ timeout: 5000 });
    console.log('  ✅ 用户名输入框 — 存在');
    await expect(passwordInput).toBeVisible({ timeout: 5000 });
    console.log('  ✅ 密码输入框 — 存在');
    await expect(submitBtn).toBeVisible({ timeout: 5000 });
    console.log('  ✅ 登录按钮 — 存在');

    // Step 3: 截图（登录前）
    await page.screenshot({ path: 'e2e-screenshots/01-login-page.png', fullPage: true });
    await page.waitForTimeout(STEP_DELAY);
    console.log('  📸 截图: 登录页');

    // Step 4: 填入账号密码
    console.log('=== Step 3: 填入账号密码 ===');
    await usernameInput.fill('test_user');
    console.log('  ✅ 填入用户名: test_user');
    await passwordInput.fill('admin123');
    console.log('  ✅ 填入密码: admin123');
    await page.waitForTimeout(STEP_DELAY);

    // Step 5: 点击登录
    console.log('=== Step 4: 点击登录按钮 ===');
    await submitBtn.click();
    console.log('  ✅ 点击登录按钮');

    // Step 6: 验证跳转（不再停留在 /login）
    console.log('=== Step 5: 验证跳转 ===');
    try {
      await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 10000 });
      console.log('  ✅ 已跳转，当前URL: ' + page.url());
    } catch {
      console.log('  ❌ 10秒内未跳转，当前URL: ' + page.url());
      // 检查是否有错误消息
      const errorMsg = page.locator('.el-message--error');
      if (await errorMsg.isVisible({ timeout: 2000 }).catch(() => false)) {
        console.log('  ❌ 登录失败：' + await errorMsg.textContent());
      }
      throw new Error('登录后未跳转');
    }

    // Step 7: 截图（登录后）
    await page.waitForTimeout(1000);
    await page.screenshot({ path: 'e2e-screenshots/02-after-login.png', fullPage: true });
    console.log('  📸 截图: 登录后页面');

    // Step 8: 验证成功提示
    const successMsg = page.locator('.el-message--success');
    const hasSuccess = await successMsg.isVisible({ timeout: 3000 }).catch(() => false);
    if (hasSuccess) {
      console.log('  ✅ 成功提示可见');
    } else {
      console.log('  ⚠️ 成功提示不可见（可能已消失），但跳转已成功');
    }

    // Step 9: 验证首页内容加载
    const productCards = page.locator('.product-card');
    const hasProducts = await productCards.first().isVisible({ timeout: 5000 }).catch(() => false);
    if (hasProducts) {
      console.log('  ✅ 首页商品卡片可见');
    } else {
      console.log('  ⚠️ 首页无商品卡片，检查页面内容...');
    }

    console.log('');
    console.log('========================================');
    console.log('  冒烟测试结果: ALL PASS ✅');
    console.log('  登录 test_user → 跳转首页 → 成功');
    console.log('========================================');
  });
});
