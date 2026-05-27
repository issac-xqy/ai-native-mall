import { test, expect } from '@playwright/test';

test.describe('用户认证', () => {
  test('注册新用户', async ({ page }) => {
    await page.goto('/login');

    // 切换到注册模式
    await page.click('text=注册账号');

    // 用时间戳保证用户名唯一
    const ts = Date.now().toString().slice(-8);
    await page.fill('input[placeholder="请输入用户名"]', `e2e_${ts}`);
    await page.fill('input[placeholder="请输入手机号"]', `138${ts}`);
    await page.fill('input[placeholder="请输入邮箱（选填）"]', `e2e_${ts}@test.com`);
    await page.fill('input[placeholder="请输入密码（6-20位）"]', 'test123456');
    await page.fill('input[placeholder="请确认密码"]', 'test123456');

    await page.click('.submit-btn');

    // 注册成功会显示消息并切换到登录模式
    await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5000 });
  });

  test('登录已有用户', async ({ page }) => {
    await page.goto('/login');

    // 正确 placeholder: "请输入用户名/手机号"
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'test_user');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');

    await page.click('.submit-btn');

    // 登录成功后跳转首页
    await expect(page).not.toHaveURL(/\/login$/, { timeout: 8000 });
  });

  test('登录-密码错误', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[placeholder="请输入用户名/手机号"]', 'test_user');
    await page.fill('input[placeholder="请输入密码"]', 'wrong_password');
    await page.click('.submit-btn');

    // Element Plus 错误消息
    await expect(page.locator('.el-message--error').first()).toBeVisible({ timeout: 5000 });
  });

  test('登录-用户不存在', async ({ page }) => {
    await page.goto('/login');

    await page.fill('input[placeholder="请输入用户名/手机号"]', 'no_such_user_xyz');
    await page.fill('input[placeholder="请输入密码"]', 'anything');
    await page.click('.submit-btn');

    await expect(page.locator('.el-message--error').first()).toBeVisible({ timeout: 5000 });
  });

  test('未登录访问需要登录的页面-跳转登录页', async ({ page }) => {
    await page.goto('/orders');

    await expect(page).toHaveURL(/\/login/, { timeout: 5000 });
  });
});
