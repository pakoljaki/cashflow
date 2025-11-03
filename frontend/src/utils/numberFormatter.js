// Central currency-aware amount formatting utility.
// Usage: formatAmount(value, { currency: 'HUF', fractionDigitsOverride: 2 })
// Defaults: currency 'HUF', style 'decimal'. This will evolve to handle locales dynamically.
import { CURRENCY_META } from '../constants/currencies'

const DEFAULT_CURRENCY = 'HUF'

export function formatAmount(value, { currency = DEFAULT_CURRENCY, fractionDigitsOverride, style = 'decimal' } = {}) {
  if (value == null || isNaN(value)) return '—'
  const meta = CURRENCY_META[currency] || { symbol: currency, fractionDigits: 0 }
  const minimumFractionDigits = fractionDigitsOverride != null ? fractionDigitsOverride : meta.fractionDigits
  const maximumFractionDigits = minimumFractionDigits
  // For now pick a locale per currency fallback; can expand later.
  const locale = currency === 'USD' ? 'en-US' : currency === 'EUR' ? 'de-DE' : 'hu-HU'
  const nf = new Intl.NumberFormat(locale, {
    minimumFractionDigits,
    maximumFractionDigits,
    useGrouping: true,
    style: style === 'currency' ? 'currency' : 'decimal',
    currency: style === 'currency' ? currency : undefined,
    currencyDisplay: 'code'
  })
  const formatted = nf.format(Number(value))
  if (style === 'currency') return formatted
  return formatted
}

// Backwards compatibility: object with a .format method (used by existing components)
export const amountFormatter = { format: (v) => formatAmount(v) }

