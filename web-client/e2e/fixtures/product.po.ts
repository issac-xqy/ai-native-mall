import { Page, expect } from '@playwright/test';

/** Page Object for /products list page */
export class ProductListPage {
  constructor(private page: Page) {}

  readonly searchInput    = () => this.page.locator('input[placeholder="搜索商品名称、描述"]');
  readonly searchBtn      = () => this.page.locator('button:has-text("搜索")');
  readonly productCards   = () => this.page.locator('.product-card');
  readonly pagination     = () => this.page.locator('.el-pagination');
  readonly emptyState     = () => this.page.locator('.el-empty');
  readonly categoryFilter = () => this.page.locator('.category-filter');

  async goto() { await this.page.goto('/products'); await this.page.waitForTimeout(500); }

  async search(keyword: string) {
    await this.searchInput().fill(keyword);
    await this.searchBtn().click();
    await this.page.waitForTimeout(1500);
  }

  async clickFirstProduct() {
    await this.productCards().first().click();
    await expect(this.page).toHaveURL(/\/product\/\d+/, { timeout: 5000 });
  }

  async expectLoaded() {
    await expect(this.productCards().first()).toBeVisible({ timeout: 5000 });
  }

  async expectEmpty() {
    await expect(this.emptyState()).toBeVisible({ timeout: 3000 });
  }
}

/** Page Object for /product/:id detail page */
export class ProductDetailPage {
  constructor(private page: Page) {}

  readonly title        = () => this.page.locator('.product-title, h1, h2').first();
  readonly currentPrice = () => this.page.locator('.current-price');
  readonly addToCartBtn = () => this.page.locator('button:has-text("加入购物车")').first();
  readonly buyNowBtn    = () => this.page.locator('button:has-text("立即购买")').first();
  readonly reviewArea   = () => this.page.locator('.reviews');

  async goto(productId: number) { await this.page.goto(`/product/${productId}`); }

  async addToCart() {
    await this.addToCartBtn().click();
    await expect(this.page.locator('.el-message--success').first()).toBeVisible({ timeout: 3000 });
  }

  async expectLoaded() {
    await expect(this.title()).toBeVisible({ timeout: 5000 });
    await expect(this.currentPrice()).toBeVisible();
  }
}

/** Page Object for /cart page */
export class CartPage {
  constructor(private page: Page) {}

  readonly items        = () => this.page.locator('.cart-item');
  readonly checkoutBtn  = () => this.page.locator('button:has-text("结算")');
  readonly emptyState   = () => this.page.locator('.el-empty');

  async goto() { await this.page.goto('/cart'); await this.page.waitForTimeout(500); }

  async checkout() {
    await this.checkoutBtn().first().click();
    await this.page.waitForTimeout(500);
  }

  async expectHasItems() {
    await expect(this.items().first()).toBeVisible({ timeout: 3000 });
  }
}
