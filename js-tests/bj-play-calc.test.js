'use strict'

const BJ_CALC = require('../src/main/resources/static/js/play/bj-play-calc.js')

const { calcChipValues, calcSideChipValues, calcTotalStake, isDealEnabled, MIN_BET, MAX_BET, SIDE_BET_MAX } = BJ_CALC

// ─────────────────────────────────────────────────────────────
// calcChipValues — main bet chip logic
// ─────────────────────────────────────────────────────────────

describe('calcChipValues — add chip', () => {
  test('adds chip value to current bet and deducts from balance', () => {
    const { newBet, newBalance, noFunds } = calcChipValues(0, 200, 25, false)
    expect(newBet).toBeCloseTo(25)
    expect(newBalance).toBeCloseTo(175)
    expect(noFunds).toBe(false)
  })

  test('accumulates multiple chips correctly', () => {
    const { newBet, newBalance } = calcChipValues(25, 175, 10, false)
    expect(newBet).toBeCloseTo(35)
    expect(newBalance).toBeCloseTo(165)
  })

  test('clamps bet to MAX_BET and refunds the overage to balance', () => {
    // bet=975 + chip=100 = 1075, clamped to 1000 (diff=75 refunded)
    // newBalance = 200 - 100 + 75 = 175
    const { newBet, newBalance } = calcChipValues(975, 200, 100, false)
    expect(newBet).toBeCloseTo(MAX_BET)
    expect(newBalance).toBeCloseTo(175)
  })

  test('clamps chip to available balance when balance is insufficient', () => {
    const { newBet, newBalance } = calcChipValues(0, 15, 25, false)
    expect(newBet).toBeCloseTo(15)
    expect(newBalance).toBeCloseTo(0)
  })

  test('returns noFunds=true and leaves state unchanged when balance is 0', () => {
    const { newBet, newBalance, noFunds } = calcChipValues(50, 0, 25, false)
    expect(noFunds).toBe(true)
    expect(newBet).toBeCloseTo(50)
    expect(newBalance).toBeCloseTo(0)
  })
})

describe('calcChipValues — double chip', () => {
  test('doubles the current bet and deducts the original amount from balance', () => {
    const { newBet, newBalance, noFunds } = calcChipValues(25, 200, null, true)
    expect(newBet).toBeCloseTo(50)
    expect(newBalance).toBeCloseTo(175)
    expect(noFunds).toBe(false)
  })

  test('caps doubled bet at MAX_BET and refunds overage', () => {
    // bet=600, double→1200. Clamped to 1000 (diff=200 refunded).
    // newBalance = 400 - 600 + 200 = 0. Then newBalance<0 check: 0 is not < 0.
    const { newBet, newBalance } = calcChipValues(600, 400, null, true)
    expect(newBet).toBeCloseTo(MAX_BET)
    expect(newBalance).toBeCloseTo(0)
  })

  test('caps doubled bet at MAX_BET when result equals MAX_BET exactly', () => {
    // 500 * 2 = 1000 = MAX_BET → allowed
    const { newBet, newBalance } = calcChipValues(500, 500, null, true)
    expect(newBet).toBeCloseTo(MAX_BET)
    expect(newBalance).toBeCloseTo(0)
  })

  test('clamps doubled bet to available balance when insufficient', () => {
    // bet=100, balance=30: can only add 30 more → newBet=130
    const { newBet, newBalance } = calcChipValues(100, 30, null, true)
    expect(newBet).toBeCloseTo(130)
    expect(newBalance).toBeCloseTo(0)
  })

  test('returns noFunds=true when balance is 0 during double', () => {
    const { noFunds } = calcChipValues(100, 0, null, true)
    expect(noFunds).toBe(true)
  })

  test('doubling 0 bet leaves bet at 0', () => {
    const { newBet, newBalance } = calcChipValues(0, 200, null, true)
    expect(newBet).toBeCloseTo(0)
    expect(newBalance).toBeCloseTo(200)
  })
})

// ─────────────────────────────────────────────────────────────
// calcSideChipValues — side bet staging
// ─────────────────────────────────────────────────────────────

describe('calcSideChipValues', () => {
  test('adds chip to empty side bet slot', () => {
    const { newSideBet, newBalance, noFunds, atCap } = calcSideChipValues(0, 100, 10)
    expect(newSideBet).toBeCloseTo(10)
    expect(newBalance).toBeCloseTo(90)
    expect(noFunds).toBe(false)
    expect(atCap).toBe(false)
  })

  test('accumulates side bets up to SIDE_BET_MAX', () => {
    const { newSideBet, newBalance } = calcSideChipValues(20, 100, 10)
    // Only 5 room left (SIDE_BET_MAX=25), chip=10 → toAdd=5
    expect(newSideBet).toBeCloseTo(25)
    expect(newBalance).toBeCloseTo(95)
  })

  test('returns atCap=true when already at SIDE_BET_MAX', () => {
    const { newSideBet, atCap } = calcSideChipValues(SIDE_BET_MAX, 100, 5)
    expect(atCap).toBe(true)
    expect(newSideBet).toBeCloseTo(SIDE_BET_MAX)
  })

  test('clamps chip to available balance', () => {
    const { newSideBet, newBalance } = calcSideChipValues(0, 3, 10)
    expect(newSideBet).toBeCloseTo(3)
    expect(newBalance).toBeCloseTo(0)
  })

  test('returns noFunds=true when balance is 0', () => {
    const { noFunds, newSideBet } = calcSideChipValues(0, 0, 5)
    expect(noFunds).toBe(true)
    expect(newSideBet).toBeCloseTo(0)
  })

  test('clamps to min of chip, room, and balance simultaneously', () => {
    // balance=4, room=3, chip=10 → toAdd=min(10,3,4)=3
    const { newSideBet, newBalance } = calcSideChipValues(22, 4, 10)
    expect(newSideBet).toBeCloseTo(25)
    expect(newBalance).toBeCloseTo(1)
  })
})

// ─────────────────────────────────────────────────────────────
// calcTotalStake
// ─────────────────────────────────────────────────────────────

describe('calcTotalStake', () => {
  test('sums all four bet areas', () => {
    expect(calcTotalStake(50, 10, 15, 5)).toBeCloseTo(80)
  })

  test('handles all zeros', () => {
    expect(calcTotalStake(0, 0, 0, 0)).toBeCloseTo(0)
  })

  test('treats undefined/null values as zero', () => {
    expect(calcTotalStake(25, undefined, null, 0)).toBeCloseTo(25)
  })

  test('main-only stake', () => {
    expect(calcTotalStake(100, 0, 0, 0)).toBeCloseTo(100)
  })
})

// ─────────────────────────────────────────────────────────────
// isDealEnabled
// ─────────────────────────────────────────────────────────────

describe('isDealEnabled', () => {
  test('enabled when bet equals MIN_BET exactly', () => {
    expect(isDealEnabled(MIN_BET)).toBe(true)
  })

  test('enabled when bet is above MIN_BET', () => {
    expect(isDealEnabled(50)).toBe(true)
    expect(isDealEnabled(MAX_BET)).toBe(true)
  })

  test('disabled when bet is below MIN_BET', () => {
    expect(isDealEnabled(9.99)).toBe(false)
    expect(isDealEnabled(0)).toBe(false)
    expect(isDealEnabled(2.5)).toBe(false)
  })

  test('disabled when bet is NaN', () => {
    expect(isDealEnabled(NaN)).toBe(false)
  })

  test('disabled when bet is negative', () => {
    expect(isDealEnabled(-1)).toBe(false)
  })
})
