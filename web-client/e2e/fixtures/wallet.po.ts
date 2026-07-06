import { Page, expect } from '@playwright/test';

/** Page Object for /wallet page */
export class WalletPage {
  constructor(private page: Page) {}

  readonly balance       = () => this.page.locator('.balance-amount');
  readonly rechargeInput = () => this.page.locator('.recharge-card input').first();
  readonly rechargeBtn   = () => this.page.locator('button:has-text("立即充值")');
  readonly quickAmount   = (n: number) => this.page.locator(`.quick-recharge button:has-text("${n}")`);
  readonly recordsTable  = () => this.page.locator('.el-table');

  async goto() { await this.page.goto('/wallet'); await this.page.waitForTimeout(500); }

  async getBalance(): Promise<string> {
    await expect(this.balance()).toBeVisible({ timeout: 3000 });
    return (await this.balance().textContent()) || '0';
  }

  async recharge(amount: number) {
    await this.rechargeInput().fill(String(amount));
    await this.rechargeBtn().click();
  }

  async expectLoaded() {
    await expect(this.balance()).toBeVisible({ timeout: 5000 });
  }
}

/** Page Object for /orders page */
export class OrdersPage {
  constructor(private page: Page) {}

  readonly orders      = () => this.page.locator('.order-card');
  readonly filterTabs  = () => this.page.locator('.order-tabs');
  readonly emptyState  = () => this.page.locator('.el-empty');

  async goto() { await this.page.goto('/orders'); await this.page.waitForTimeout(1000); }

  async expectLoaded() {
    const hasContent = await this.page.locator('.el-table, .el-empty, h2, h3')
      .first().isVisible({ timeout: 5000 }).catch(() => false);
    expect(hasContent).toBeTruthy();
  }
}

/** Page Object for /ai-chat page */
export class AIChatPage {
  constructor(private page: Page) {}

  readonly input    = () => this.page.locator('textarea[placeholder*="请输入您的问题"]');
  readonly sendBtn  = () => this.page.locator('button:has-text("发送")');
  readonly clearBtn = () => this.page.locator('button:has-text("清空对话")');
  readonly messages = () => this.page.locator('.message');
  readonly loading  = () => this.page.locator('button:has-text("AI 思考中")');
  readonly welcome  = () => this.page.locator('.message.ai .content').first();

  async goto() { await this.page.goto('/ai-chat'); await this.page.waitForLoadState('networkidle'); }

  async ask(question: string) {
    await this.input().fill(question);
    await this.sendBtn().click();
  }

  async waitForReply(timeout = 30000) {
    await expect(this.loading()).not.toBeVisible({ timeout });
    await this.page.waitForTimeout(500);
  }

  async clear() {
    await this.clearBtn().click();
    await expect(this.welcome()).toContainText('对话已清空');
  }

  async expectLoaded() {
    await expect(this.welcome()).toBeVisible({ timeout: 5000 });
    await expect(this.sendBtn()).toBeVisible();
  }
}
