import { test, expect } from '@playwright/test';

const ADMIN_URL = 'http://localhost:3000';

test.describe('管理后台', () => {
  // Admin login required before all tests
  test.beforeEach(async ({ page }) => {
    await page.goto(ADMIN_URL + '/');
    await page.waitForLoadState('networkidle');

    // If login page shown, auth
    const loginInput = page.locator('input[placeholder="管理员用户名"]');
    if (await loginInput.isVisible({ timeout: 2000 }).catch(() => false)) {
      await loginInput.fill('test_user');
      await page.locator('input[placeholder="密码"]').fill('admin123');
      await page.locator('button:has-text("登录")').click();
      await page.waitForTimeout(800);
    }
  });

  test('数据统计-仪表盘显示统计卡片', async ({ page }) => {
    await expect(page.locator('.stat-card').first()).toBeVisible({ timeout: 5000 });
    const count = await page.locator('.stat-card').count();
    expect(count).toBeGreaterThanOrEqual(3);
    await expect(page.locator('text=商品总数')).toBeVisible();
  });

  test('商品管理-表格加载数据', async ({ page }) => {
    await page.goto(ADMIN_URL + '/products');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('input[placeholder="商品名称"]')).toBeVisible();
    await expect(page.locator('button:has-text("搜索")')).toBeVisible();
  });

  test('商品管理-搜索商品名称', async ({ page }) => {
    await page.goto(ADMIN_URL + '/products');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await page.locator('input[placeholder="商品名称"]').fill('手机');
    await page.locator('button:has-text("搜索")').click();
    await page.waitForTimeout(1500);
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('分类管理-列表加载', async ({ page }) => {
    await page.goto(ADMIN_URL + '/categories');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('.el-table, .category-list, h2, h3').first())
      .toBeVisible({ timeout: 5000 });
  });

  test('AI监控看板-页面加载', async ({ page }) => {
    await page.goto(ADMIN_URL + '/ai-monitor');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('h2, h3, .el-card, .chart, canvas').first())
      .toBeVisible({ timeout: 5000 });
  });

  test('知识库管理-页面加载', async ({ page }) => {
    await page.goto(ADMIN_URL + '/knowledge');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('h2, h3, .el-card, .el-table, .el-form').first())
      .toBeVisible({ timeout: 5000 });
  });

  test('订单管理-列表加载', async ({ page }) => {
    await page.goto(ADMIN_URL + '/orders');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await expect(page.locator('.el-table, h2, h3').first())
      .toBeVisible({ timeout: 5000 });
  });

  test('侧边栏导航-点击子菜单切换页面', async ({ page }) => {
    const productMenu = page.locator('.el-menu-item:has-text("商品管理")');
    await expect(productMenu).toBeVisible({ timeout: 3000 });
    await productMenu.click();
    await page.waitForTimeout(500);
    expect(page.url()).toContain('/products');

    const dashboardMenu = page.locator('.el-menu-item:has-text("数据统计")');
    await dashboardMenu.click();
    await page.waitForTimeout(500);
    expect(page.url()).toBe(ADMIN_URL + '/');
  });
});
