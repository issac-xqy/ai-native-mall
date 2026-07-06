import { test as base, expect, Page } from '@playwright/test';

/** Login fixture — reusable across all tests that need auth */
export const test = base.extend<{ loggedInPage: Page }>({
  loggedInPage: async ({ page }, use) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');

    await page.fill('input[placeholder="请输入用户名/手机号"]', 'test_user');
    await page.fill('input[placeholder="请输入密码"]', 'admin123');
    await page.click('.submit-btn');

    await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 8000 });
    await page.waitForTimeout(500);

    await use(page);
  },
});

export { expect };

/** Reusable login helper — call directly in beforeEach */
export async function login(page: Page, username = 'test_user', password = 'admin123') {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');
  await page.fill('input[placeholder="请输入用户名/手机号"]', username);
  await page.fill('input[placeholder="请输入密码"]', password);
  await page.click('.submit-btn');
  await page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 8000 });
  await page.waitForTimeout(500);
}

/** Wait for success message and verify it appears */
export async function expectSuccess(page: Page) {
  await expect(page.locator('.el-message--success').first()).toBeVisible({ timeout: 5000 });
}

/** Wait for error message */
export async function expectError(page: Page) {
  await expect(page.locator('.el-message--error').first()).toBeVisible({ timeout: 5000 });
}
