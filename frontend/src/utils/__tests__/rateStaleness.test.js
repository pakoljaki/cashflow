import { daysSince, classifyStaleness, stalenessColor } from '../rateStaleness'

// Helper to freeze Date.now for deterministic tests
const REAL_NOW = Date.now
beforeAll(() => {
  // Set a fixed 'now' = 2025-11-02T00:00:00Z
  Date.now = () => new Date('2025-11-02T00:00:00Z').getTime()
})
afterAll(() => { Date.now = REAL_NOW })

describe('rateStaleness utilities', () => {
  test('daysSince returns Infinity for invalid date', () => {
    expect(daysSince('not-a-date')).toBe(Infinity)
    expect(daysSince(null)).toBe(Infinity)
  })

  test('daysSince computes whole day difference', () => {
    expect(daysSince('2025-11-01')).toBe(1)
    expect(daysSince('2025-10-30')).toBe(3) // 30,31,1 -> diff in ms floors
  })

  test('classifyStaleness fresh/warn/stale thresholds', () => {
    const warnAfterDays = 2
    const staleAfterDays = 5
    expect(classifyStaleness('2025-11-01', { warnAfterDays, staleAfterDays }).level).toBe('fresh') // 1 day
    expect(classifyStaleness('2025-10-31', { warnAfterDays, staleAfterDays }).level).toBe('warn')  // 2 days
    expect(classifyStaleness('2025-10-28', { warnAfterDays, staleAfterDays }).level).toBe('stale') // 5 days
  })

  test('classifyStaleness unknown for invalid', () => {
    expect(classifyStaleness('bad').level).toBe('unknown')
  })

  test('stalenessColor mapping', () => {
    expect(stalenessColor('fresh')).toBe('success.main')
    expect(stalenessColor('warn')).toBe('warning.main')
    expect(stalenessColor('stale')).toBe('error.main')
    expect(stalenessColor('unknown')).toBe('text.disabled')
  })
})
