import { defineConfig, devices } from '@playwright/test'

const isCI = !!process.env.CI

// In CI the backend jar is built by a previous step; locally it runs through the Maven wrapper.
const backendCommand = isCI
  ? 'java -jar target/portwallet-backend.jar --spring.profiles.active=e2e'
  : './mvnw -q spring-boot:run -Dspring-boot.run.profiles=e2e'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 2 : 0,
  workers: isCI ? 2 : undefined,
  reporter: isCI
    ? [['html', { open: 'never' }], ['github']]
    : [['html', { open: 'never' }], ['list']],
  use: {
    baseURL: 'http://localhost:5173',
    // The UI follows the browser language; the suites assert the default language unless they set their own.
    locale: 'pt-BR',
    trace: 'on-first-retry',
    video: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
  ],
  webServer: [
    {
      command: backendCommand,
      cwd: '../backend',
      url: 'http://localhost:8080/actuator/health',
      timeout: 180_000,
      reuseExistingServer: !isCI,
    },
    {
      command: 'npm run dev',
      url: 'http://localhost:5173',
      timeout: 60_000,
      reuseExistingServer: !isCI,
    },
  ],
})
