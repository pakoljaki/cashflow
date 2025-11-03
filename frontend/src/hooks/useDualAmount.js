import { useMemo } from 'react'
import { formatAmount } from '../utils/numberFormatter'
import { useCurrency } from '../context/CurrencyContext'


export function useDualAmount({ amount, currency, convertedAmount, rateDate, rateSource }) {
  const { displayCurrency } = useCurrency()
  return useMemo(() => {
    const nativeFormatted = formatAmount(amount, { currency })
    const same = currency === displayCurrency
    if (same || convertedAmount == null) {
      return { single: true, text: nativeFormatted, nativeFormatted }
    }
    const convertedFormatted = formatAmount(convertedAmount, { currency: displayCurrency })
    const tooltip = rateDate
      ? `Rate date: ${rateDate}${rateSource ? ` (source: ${rateSource})` : ''}`
      : 'Converted amount'
    return {
      single: false,
      nativeFormatted,
      convertedFormatted,
      tooltip,
      displayCurrency,
      currency,
    }
  }, [amount, currency, convertedAmount, rateDate, rateSource, displayCurrency])
}
