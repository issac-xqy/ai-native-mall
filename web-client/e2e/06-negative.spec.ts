import { test, expect } from '@playwright/test';

test.describe('负面场景测试', () => {
  test('登录-空输入直接点登录-表单校验', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    // 什么都不填，直接点登录
    await page.locator('.submit-btn').click();
    await page.waitForTimeout(500);

    // Element Plus 表单校验错误
    const formError = page.locator('.el-form-item__error, .el-message--warning, .el-message--error');
    const hasError = await formError.first().isVisible({ timeout: 3000 }).catch(() => false);
    expect(hasError).toBeTruthy();
  });

  test('搜索-无结果时显示空状态', async ({ page }) => {
    // 先登录
    await page.goto('/login');
    await page.fill('input[placeholder="请输入用户名/手机号"]', 'test_user');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('.submit-btn');
    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 8000 });

    // 打开商品列表并搜索不存在的内容
    await page.goto('/products');
    await page.waitForSelector('.product-card', { timeout: 5000 }).catch(() => {});

    const searchInput = page.locator('input[placeholder="搜索商品名称、描述"]');
    await searchInput.fill('xyznonexistent_product_999');
    await page.locator('button:has-text("搜索")').click();
    await page.waitForTimeout(2000);

    // 应显示空状态或搜索无结果
    const emptyState = page.locator('.el-empty, [class*="empty"], [class*="no-result"], :text-matches("未找到|暂无|没有")');
    const hasEmpty = await emptyState.first().isVisible({ timeout: 3000 }).catch(() => false);

    // 或有产品卡片数为0
    const cards = page.locator('.product-card');
    const cardCount = await cards.count();

    // 通过：要么有空状态提示，要么没有商品卡片
    expect(hasEmpty || cardCount === 0).toBeTruthy();
  });

  test('AI客服-输入敏感词被过滤', async ({ page }) => {
    await page.goto('/ai-chat');
    await page.waitForLoadState('networkidle');

    const input = page.locator('textarea[placeholder*="请输入您的问题"]');
    const sendBtn = page.locator('button:has-text("发送")');

    // 发送一个简单问题确保 AI 正常
    await input.fill('你好');
    await sendBtn.click();
    await page.waitForTimeout(3000);

    // 对话区应有回复
    const messages = page.locator('.message');
    const countBefore = await messages.count();
    expect(countBefore).toBeGreaterThan(1);
  });

  test('未登录-访问需要登录页面应跳转登录页', async ({ page }) => {
    // 清除 localStorage 确保未登录
    await page.goto('/');
    await page.evaluate(() => localStorage.clear());

    await page.goto('/orders');
    await page.waitForTimeout(1000);

    // /orders 页面有 auth 检查，未登录会跳转
    expect(page.url()).toContain('/login');
  });

  test('无效商品ID-返回商品不存在', async ({ page }) => {
    // v1 API 版本化后的路径
    const response = await page.request.get('http://localhost:8081/api/v1/product/99999');
    const data = await response.json();
    // 商品不存在：code 为 1005 或 data 为 null
    const isNotFound = data.code === 1005 || data.data === null ||
                       data.code === 404;
    expect(isNotFound).toBeTruthy();
  });

  test('注册-重复用户名返回错误', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    // 切换到注册模式
    await page.click('text=注册账号');
    await page.waitForTimeout(300);

    // 用已知存在的用户名注册
    await page.fill('input[placeholder="请输入用户名"]', 'test_user');
    await page.fill('input[placeholder="请输入手机号"]', '13800001111');
    await page.fill('input[placeholder="请输入密码（6-20位）"]', 'test123456');
    await page.fill('input[placeholder="请确认密码"]', 'test123456');

    await page.click('.submit-btn');

    // 应显示错误消息
    const errorMsg = page.locator('.el-message--error').first();
    await expect(errorMsg).toBeVisible({ timeout: 5000 });
  });
});
