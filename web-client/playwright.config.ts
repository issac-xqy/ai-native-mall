import { defineConfig, devices } from '@playwright/test';

// Environment switching: set TEST_ENV=staging|test|ci
const env = process.env.TEST_ENV || 'local';
const baseURLs: Record<string, string> = {
  local:   'http://localhost:3001',
  test:    process.env.TEST_BASE_URL  || 'http://localhost:3001',
  staging: process.env.STG_BASE_URL   || 'http://localhost:4173',
  ci:      process.env.CI_BASE_URL    || 'http://localhost:3001',
};
const backendURLs: Record<string, string> = {
  local:   'http://localhost:8081',
  test:    process.env.TEST_API_URL   || 'http://localhost:8081',
  staging: process.env.STG_API_URL    || 'http://localhost:8081',
  ci:      process.env.CI_API_URL     || 'http://localhost:8081',
};
const baseURL = baseURLs[env] || baseURLs.local;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: 'html',
  timeout: 60000,

  use: {
    baseURL,
    trace: 'on',
    screenshot: 'on',
    headless: !!process.env.CI,
  },

  projects: [
    {
      name: 'edge',
      use: {
        ...devices['Desktop Edge'],
        channel: 'msedge',
      },
    },
  ],

  // Only auto-start webServer in local/dev mode
  webServer: env === 'local' ? {
    command: 'npm run dev',
    url: 'http://localhost:3001',
    reuseExistingServer: !process.env.CI,
    timeout: 30000,
  } : undefined,
});

export { baseURL, backendURLs };
