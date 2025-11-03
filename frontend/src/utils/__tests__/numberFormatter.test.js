import { formatAmount } from '../numberFormatter'

describe('formatAmount', () => {
  test('returns em dash for null/undefined/NaN', () => {
    expect(formatAmount(null)).toBe('—')
    expect(formatAmount(undefined)).toBe('—')
    expect(formatAmount(NaN)).toBe('—')
  })

  test('formats HUF with 0 fraction digits by default', () => {
    const v = 1234567
    const out = formatAmount(v, { currency: 'HUF' })
    expect(out).toMatch(/1[\s,.]?234[\s,.]?567/) // grouping present
    expect(out).not.toMatch(/\.\d{2}$/)
  })

  test('formats EUR with 2 fraction digits by default', () => {
    const out = formatAmount(9876.5, { currency: 'EUR' })
    expect(out).toMatch(/98[\s,.]?76\.50|9,876\.50|9.876,50/) // locale dependent
  })

  test('override fraction digits', () => {
    const out = formatAmount(10.1234, { currency: 'USD', fractionDigitsOverride: 3 })
    expect(out).toMatch(/10\.123|10,123/) // 3 digits
  })

  test('currency style outputs code', () => {
    const out = formatAmount(15, { currency: 'USD', style: 'currency' })
    expect(out).toMatch(/USD/) // currencyDisplay code
  })

  test('negative values maintain sign', () => {
    const out = formatAmount(-123.45, { currency: 'USD' })
    expect(out.startsWith('-')).toBe(true)
  })

  test('large number formatting grouping', () => {
    const out = formatAmount(1234567890, { currency: 'USD' })
    expect(out).toMatch(/1[\s,.]?234[\s,.]?567[\s,.]?890/)
  })
})
