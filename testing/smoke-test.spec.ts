import { test, expect } from '@playwright/test';

/**
 * 冒烟测试 — 覆盖核心用户流程
 * 运行: npx playwright test smoke-test.spec.ts
 */

test.describe('用户端冒烟测试', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3001');
    await page.evaluate(() => localStorage.clear());
  });

  test('1. 未登录 → 首页 → 自动跳转登录页', async ({ page }) => {
    await page.goto('http://localhost:3001/');
    await expect(page).toHaveURL(/\/login\?redirect=\//);
  });

  test('2. 错误密码 → 显示错误提示', async ({ page }) => {
    await page.goto('http://localhost:3001/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'wrongpass');
    await page.click('button:has-text("登录")');
    await page.waitForTimeout(1000);
    const msg = page.locator('.el-message--error');
    await expect(msg).toContainText('用户名或密码错误');
  });

  test('3. 正确密码 → 登录成功 → 首页 + 用户名显示', async ({ page }) => {
    await page.goto('http://localhost:3001/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('http://localhost:3001/');
    await expect(page.locator('.user-name')).toHaveText('admin');
  });

  test('4. 首页渲染商品内容', async ({ page }) => {
    await page.goto('http://localhost:3001/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('http://localhost:3001/');
    await page.waitForTimeout(1000);
    // 检查热门商品区域有内容
    const items = page.locator('[class*="product"]');
    await expect(items.first()).toBeVisible();
  });

  test('5. 搜索功能 → 跳转商品列表 + 带 keyword', async ({ page }) => {
    await page.goto('http://localhost:3001/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('http://localhost:3001/');
    await page.fill('input[placeholder="搜索商品..."]', '农夫');
    await page.keyboard.press('Enter');
    await page.waitForURL(/\/products\?keyword=/);
  });

  test('6. 各页面导航正常', async ({ page }) => {
    await page.goto('http://localhost:3001/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('http://localhost:3001/');

    const pages = ['/cart', '/orders', '/wallet', '/profile', '/ai-chat'];
    for (const path of pages) {
      await page.goto('http://localhost:3001' + path);
      await page.waitForTimeout(500);
      await expect(page).toHaveURL(new RegExp(path));
    }
  });

  test('7. 退出登录 → 回到登录页 + 显示登录按钮', async ({ page }) => {
    // 先登录
    await page.goto('http://localhost:3001/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'admin');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('button:has-text("登录")');
    await page.waitForURL('http://localhost:3001/');
    await page.waitForTimeout(1000);

    // 点击用户名打开下拉
    await page.locator('.user-badge').click();
    await page.waitForTimeout(300); // 等下拉动画

    // 点击退出登录
    await page.getByRole('menuitem', { name: '退出登录' }).click();
    await page.waitForTimeout(1000);

    // 验证：已跳转登录页
    await expect(page).toHaveURL(/\/login/);
  });
});
