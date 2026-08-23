import { Page } from '@playwright/test'

export async function login(page: Page, username = 'pesho', password = '1234') {
  await page.goto('/auth/login')
  await page.locator('input[name="username"]').fill(username)
  await page.locator('#password').fill(password)
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/(play|$)/)
}

export async function goToPlay(page: Page) {
  await page.goto('/play')
}

/** Submit a POST form using the page's CSRF token. */
export async function postAction(page: Page, path: string, params: Record<string, string> = {}) {
  await page.evaluate(
    async ({ path, params }) => {
      const csrf = document.querySelector<HTMLInputElement>('input[name="_csrf"]')?.value ?? ''
      const body = new URLSearchParams({ _csrf: csrf, ...params })
      await fetch(path, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body,
        redirect: 'manual',
      })
    },
    { path, params }
  )
}

/** Read all `var BJ_* = ...` JS variables injected by the server into /play. */
export async function getBJVars(page: Page): Promise<Record<string, string>> {
  const html = await page.evaluate(async () => {
    const r = await fetch('/play')
    return r.text()
  })
  const entries = [...html.matchAll(/var (BJ_\w+)\s*=\s*([^;]+);/g)]
  return Object.fromEntries(entries.map(m => [m[1], m[2].trim()]))
}

/** Return wallet DB values by scraping them from the rendered page. */
export async function getWalletDisplay(page: Page) {
  await page.goto('/play')
  return {
    balance: await page.locator('.balance').first().innerText(),
    lastBet: await page.locator('.last-bet-value, [data-last-bet]').first().innerText().catch(() => ''),
    currentBet: await page.locator('.curr-bet').first().innerText(),
  }
}
