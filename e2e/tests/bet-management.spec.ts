import { test, expect, Page } from '@playwright/test'

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function login(page: Page) {
  await page.goto('/auth/login')
  await page.locator('input[name="username"]').fill('pesho')
  await page.locator('#password').fill('1234')
  await page.locator('button[type="submit"]').click()
  await page.waitForURL(/\/(play|$)/, { timeout: 10_000 })
}

/** POST a game action via hidden form to ensure fresh CSRF on each call. */
async function postAction(page: Page, path: string, params: Record<string, string> = {}) {
  // Navigate to /play first to get a fresh CSRF token, then post
  await page.goto('/play')
  await page.evaluate(
    async ({ path, params }: { path: string; params: Record<string, string> }) => {
      const csrf =
        (document.querySelector('input[name="_csrf"]') as HTMLInputElement)?.value ?? ''
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

/** Read all server-injected `var BJ_* = ...` variables from a fresh /play page load. */
async function getBJVars(page: Page): Promise<Record<string, string>> {
  await page.goto('/play')
  const html = await page.content()
  return Object.fromEntries(
    [...html.matchAll(/var (BJ_\w+)\s*=\s*([^;]+);/g)].map(m => [m[1], m[2].trim()])
  )
}

/** Return the displayed balance as a number. Reads it from the current page DOM. */
async function readBalance(page: Page): Promise<number> {
  await page.goto('/play')
  return parseFloat(
    (await page.locator('.balance').first().innerText()).replace(/[£,]/g, '')
  )
}

/**
 * Ensure a clean pre-deal state before each test:
 * - finish any in-progress hand (stand)
 * - let payout run (GET /play)
 * - clear any staged pre-deal bets (server + client)
 */
async function resetToClean(page: Page) {
  // May need multiple passes if a hand is in-progress
  for (let attempt = 0; attempt < 3; attempt++) {
    const vars = await getBJVars(page)
    const dealt = vars.BJ_GAME_DEALT === 'true'
    const finalized = vars.BJ_FINALIZED === 'true'

    if (!dealt) break // already clean

    if (dealt && !finalized) {
      // Stand to finalize
      await page.evaluate(async () => {
        const csrf =
          (document.querySelector('input[name="_csrf"]') as HTMLInputElement)?.value ?? ''
        const body = new URLSearchParams({ _csrf: csrf })
        await fetch('/play/stand', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body,
          redirect: 'manual',
        })
      })
      // GET triggers payout
      await page.goto('/play')
    } else if (dealt && finalized) {
      // GET /play triggers payout automatically
      await page.goto('/play')
    }
  }

  // Delete any leftover last_games record (pre-deal state like after repeat)
  await page.evaluate(async () => {
    const csrf =
      (document.querySelector('input[name="_csrf"]') as HTMLInputElement)?.value ?? ''
    const body = new URLSearchParams({ _csrf: csrf })
    await fetch('/play/clear-bet', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
      redirect: 'manual',
    })
  })
  await page.goto('/play')
}

// ---------------------------------------------------------------------------
// Test suite
// ---------------------------------------------------------------------------

test.describe('Bet management — repeat, double, bet circles', () => {
  test.beforeAll(async ({ browser }) => {
    // Top up pesho's balance before the suite so server-side tests have funds
    const page = await browser.newPage()
    await login(page)
    await page.goto('/credit-card/deposit')
    await page.evaluate(async () => {
      const csrf = document.querySelector<HTMLInputElement>('input[name="_csrf"]')?.value ?? ''
      const body = new URLSearchParams({
        _csrf: csrf,
        cardNumber: '5345 7812 3319 0988',
        cvcDeposit: '778',
        depositSum: '10000',
      })
      await fetch('/credit-card/deposit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body,
        redirect: 'manual',
      })
    })
    await page.close()
  })

  test.beforeEach(async ({ page }) => {
    await login(page)
    await resetToClean(page)
  })

  // ── Repeat last bet ────────────────────────────────────────────────────────

  test('repeat-last-bet places the same main bet as the previous hand', async ({ page }) => {
    const vars0 = await getBJVars(page)
    const lastBet = parseFloat(vars0.BJ_LAST_BET ?? '0')
    test.skip(lastBet <= 0, 'No last bet in DB — play a hand first')

    const balBefore = await readBalance(page)
    await postAction(page, '/play/repeat-last-bet')

    const vars1 = await getBJVars(page)
    expect(vars1.BJ_GAME_DEALT).toBe('true')
    expect(parseFloat(vars1.BJ_LAST_BET)).toBeCloseTo(lastBet, 2)

    const balAfter = await readBalance(page)
    expect(balAfter).toBeCloseTo(balBefore - lastBet, 2)
  })

  test('clear-bet does not zero out last_bet', async ({ page }) => {
    const vars0 = await getBJVars(page)
    const lastBet = parseFloat(vars0.BJ_LAST_BET ?? '0')
    test.skip(lastBet <= 0, 'No last bet in DB — play a hand first')

    await postAction(page, '/play/clear-bet')
    const vars1 = await getBJVars(page)
    expect(parseFloat(vars1.BJ_LAST_BET)).toBeCloseTo(lastBet, 2)
  })

  test('full hand → stand → repeat places the same bet', async ({ page }) => {
    // 1. Deal £25
    await postAction(page, '/play/deal', { betStr: '25' })

    // 2. Stand — finalize (random outcome is fine, we only care about bet tracking)
    await postAction(page, '/play/stand')

    // 3. After payout last_bet must be 25
    const afterPayout = await getBJVars(page)
    expect(parseFloat(afterPayout.BJ_LAST_BET)).toBeCloseTo(25, 2)
    expect(afterPayout.BJ_GAME_DEALT).toBe('false')

    // 4. Repeat
    const balBefore = await readBalance(page)
    await postAction(page, '/play/repeat-last-bet')

    const afterRepeat = await getBJVars(page)
    expect(afterRepeat.BJ_GAME_DEALT).toBe('true')

    const balAfter = await readBalance(page)
    expect(balAfter).toBeCloseTo(balBefore - 25, 2)
  })

  // ── Double bet (client-side chip button) ──────────────────────────────────

  test('client-side double-chip button doubles the staged main bet', async ({ page }) => {
    await page.goto('/play')

    // Intercept form submission so the page doesn't navigate away
    await page.evaluate(() => {
      document.querySelectorAll<HTMLFormElement>('form[action*="double-bet"]').forEach(f => {
        f.addEventListener('submit', e => e.preventDefault(), true)
      })
    })

    // Click the £25 chip (from 0 staged)
    await page.locator('button.chip-2500').click()
    const betAfterChip = parseFloat(
      (await page.locator('.curr-bet').first().innerText()).replace(/[£,]/g, '')
    )
    expect(betAfterChip).toBeCloseTo(25, 2)

    // Double it (JS handler runs; form submit is suppressed)
    await page.locator('.btn-chip-double').click()
    const betAfterDouble = parseFloat(
      (await page.locator('.curr-bet').first().innerText()).replace(/[£,]/g, '')
    )
    expect(betAfterDouble).toBeCloseTo(50, 2)
  })

  test('client-side double-chip is capped at MAX_BET', async ({ page }) => {
    await page.goto('/play')

    // Intercept form submission so the page doesn't navigate away
    await page.evaluate(() => {
      document.querySelectorAll<HTMLFormElement>('form[action*="double-bet"]').forEach(f => {
        f.addEventListener('submit', e => e.preventDefault(), true)
      })
    })

    // Seed the DOM state: set bet = £500, balance = £5000 (plenty of room)
    // so we can test the MAX_BET cap without depleting the real DB balance
    await page.evaluate(() => {
      const hiddenBet = document.querySelector<HTMLInputElement>('.curr-bet-value')!
      const currBetSpan = document.querySelector<HTMLElement>('.curr-bet')!
      const balanceEl = document.querySelector<HTMLElement>('.balance')!
      hiddenBet.value = '500'
      currBetSpan.innerText = '£500.00'
      balanceEl.innerText = '£5000.00'
    })

    const betAt500 = parseFloat(
      (await page.locator('.curr-bet').first().innerText()).replace(/[£,]/g, '')
    )
    expect(betAt500).toBeCloseTo(500, 2)

    // Double: 500 * 2 = 1000 which equals MAX_BET exactly → allowed
    await page.locator('.btn-chip-double').click()
    const betAt1000 = parseFloat(
      (await page.locator('.curr-bet').first().innerText()).replace(/[£,]/g, '')
    )
    expect(betAt1000).toBeCloseTo(1000, 2)

    // Double again: would exceed MAX_BET → clamped back to 1000
    await page.locator('.btn-chip-double').click()
    const betAfterCap = parseFloat(
      (await page.locator('.curr-bet').first().innerText()).replace(/[£,]/g, '')
    )
    expect(betAfterCap).toBeCloseTo(1000, 2)
  })

  test('server double-bet doubles committed wallet hand bet', async ({ page }) => {
    const vars0 = await getBJVars(page)
    const lastBet = parseFloat(vars0.BJ_LAST_BET ?? '0')
    test.skip(lastBet <= 0, 'No last bet — play a hand first')

    // Stage via repeat (commits to wallet)
    await postAction(page, '/play/repeat-last-bet')

    // POST double-bet
    await postAction(page, '/play/double-bet')

    // Read total stake from fresh page
    await page.goto('/play')
    const totalStake = parseFloat(
      (await page.locator('.curr-bet.total-stake').first().innerText()).replace(/[£,]/g, '')
    )
    expect(totalStake).toBeCloseTo(Math.min(lastBet * 2, 1000), 2)
  })

  // ── Bet circles visibility ─────────────────────────────────────────────────

  test('bet-circles-wrapper is visible before deal', async ({ page }) => {
    // resetToClean guarantees no active hand
    await page.goto('/play')
    const wrapper = page.locator('.bet-circles-wrapper')
    await expect(wrapper).not.toHaveClass(/bet-circles-wrapper--in-play/)
    const visibility = await wrapper.evaluate(el => getComputedStyle(el).visibility)
    expect(visibility).toBe('visible')
  })

  test('bet-circles-wrapper is hidden while a hand is in progress', async ({ page }) => {
    // Deal a hand, then navigate to /play (PRG GET renders dealt=true)
    await postAction(page, '/play/deal', { betStr: '25' })
    // postAction ends with page at /play (pre-POST state); navigate again to get fresh render
    await page.goto('/play')

    const wrapper = page.locator('.bet-circles-wrapper')
    await expect(wrapper).toHaveClass(/bet-circles-wrapper--in-play/)
    const visibility = await wrapper.evaluate(el => getComputedStyle(el).visibility)
    expect(visibility).toBe('hidden')
  })

  test('bet-circles-wrapper becomes visible again after hand finishes', async ({ page }) => {
    await postAction(page, '/play/deal', { betStr: '25' })
    await postAction(page, '/play/stand')
    // Final GET: payout runs, game deleted, dealt=false
    await page.goto('/play')

    const wrapper = page.locator('.bet-circles-wrapper')
    await expect(wrapper).not.toHaveClass(/bet-circles-wrapper--in-play/)
    const visibility = await wrapper.evaluate(el => getComputedStyle(el).visibility)
    expect(visibility).toBe('visible')
  })
})
