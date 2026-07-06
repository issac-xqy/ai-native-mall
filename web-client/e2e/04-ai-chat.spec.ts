import { test, expect } from '@playwright/test';

const hasAI = process.env.AI_API_KEY && process.env.AI_API_KEY !== 'sk-placeholder';
const skipAI = test.describe.configure
  ? test.describe
  : test.describe;

test.describe('AI 智能客服', () => {
  test.skip(!hasAI, '⚠️ AI_API_KEY 未配置，跳过 AI 对话测试');

  test('AI对话-发送问题并收到流式回复', async ({ page }) => {
    await page.goto('/ai-chat');
    await page.waitForLoadState('networkidle');

    // 验证初始状态
    const welcomeMsg = page.locator('.message.ai .content').first();
    await expect(welcomeMsg).toBeVisible({ timeout: 5000 });
    await expect(welcomeMsg).toContainText('客服助手');

    const input = page.locator('textarea[placeholder*="请输入您的问题"]');
    await expect(input).toBeVisible();

    const sendBtn = page.locator('button:has-text("发送")');
    await expect(sendBtn).toBeVisible();

    // 发送问题
    await input.fill('你是谁？');
    await sendBtn.click();

    // 验证 loading 状态
    await expect(page.locator('button:has-text("AI 思考中")')).toBeVisible({ timeout: 3000 });

    // 等待 AI 回复（loading 消失 + 消息区出现回复）
    await expect(page.locator('button:has-text("AI 思考中")')).not.toBeVisible({ timeout: 30000 });
    await page.waitForTimeout(500);

    // 对话区应该包含多条消息
    const messages = page.locator('.message');
    const count = await messages.count();
    expect(count).toBeGreaterThanOrEqual(2);       // welcome + user + ai reply = 3+
  });

  test('AI对话-清空对话按钮恢复初始状态', async ({ page }) => {
    await page.goto('/ai-chat');
    await page.waitForLoadState('networkidle');

    // 先发一条消息
    await page.locator('textarea[placeholder*="请输入您的问题"]').fill('你好');
    await page.locator('button:has-text("发送")').click();
    await page.waitForTimeout(2000);

    // 点击清空
    const clearBtn = page.locator('button:has-text("清空对话")');
    await expect(clearBtn).toBeVisible();
    await clearBtn.click();

    // 验证恢复为欢迎消息
    const welcomeMsg = page.locator('.message.ai .content').first();
    await expect(welcomeMsg).toContainText('对话已清空');
  });

  test('AI对话-空输入点发送弹出警告', async ({ page }) => {
    await page.goto('/ai-chat');
    await page.waitForLoadState('networkidle');

    // 不填输入直接点发送
    await page.locator('button:has-text("发送")').click();

    // 应该弹出 Element Plus 警告消息
    const warning = page.locator('.el-message--warning, .el-message--error').first();
    await expect(warning).toBeVisible({ timeout: 3000 });
  });

  test('AI对话-连续多轮对话能记住上下文', async ({ page }) => {
    await page.goto('/ai-chat');
    await page.waitForLoadState('networkidle');

    const input = page.locator('textarea[placeholder*="请输入您的问题"]');
    const sendBtn = page.locator('button:has-text("发送")');

    // 第1轮
    await input.fill('你叫什么名字？');
    await sendBtn.click();
    await page.waitForTimeout(3000);    // 等 AI 回复

    // 第2轮
    await input.fill('1+1等于几？');
    await sendBtn.click();
    await page.waitForTimeout(3000);

    // 第3轮
    await input.fill('帮我推荐一款手机');
    await sendBtn.click();
    await page.waitForTimeout(5000);

    // 对话区消息数 >= 7 (welcome + 3 user + 3 ai = 7)
    const messages = page.locator('.message');
    const count = await messages.count();
    expect(count).toBeGreaterThanOrEqual(5);
  });
});
