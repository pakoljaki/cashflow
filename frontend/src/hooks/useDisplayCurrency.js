import { useMemo } from 'react'
import { useCurrency } from '../context/CurrencyContext'
import { formatAmount } from '../utils/numberFormatter'

// Hook centralizing display currency formatting & dual detection logic.
// Returns helpers:
//  - displayCurrency, baseCurrency
//  - formatDisplay(value)
//  - legendSuffix() -> e.g. "HUF→EUR" or "HUF"
//  - isConverted
//  - dualMeta(nativeValue, convertedValue, opts)
export function useDisplayCurrency() {
  const { displayCurrency, basePlanCurrency: baseCurrency } = useCurrency()
  const isConverted = displayCurrency !== baseCurrency

  const formatDisplay = (value, opts = {}) => formatAmount(value, { currency: displayCurrency, ...opts })
  const legendSuffix = () => isConverted ? `${baseCurrency}→${displayCurrency}` : displayCurrency

  // Build dual metadata object consumed by <DualAmount />
  const dualMeta = useMemo(() => (nativeValue, convertedValue, { tooltip, forceSingle = false } = {}) => {
    const nativeFormatted = formatAmount(nativeValue, { currency: baseCurrency })
    const convertedFormatted = formatAmount(convertedValue, { currency: displayCurrency })
    return {
      single: forceSingle || !isConverted || nativeValue === convertedValue,
      nativeFormatted,
      convertedFormatted,
      tooltip,
      displayCurrency,
      currency: baseCurrency,
    }
  }, [baseCurrency, displayCurrency, isConverted])

  return useMemo(() => ({
    displayCurrency,
    baseCurrency,
    isConverted,
    formatDisplay,
    legendSuffix,
    dualMeta,
  }), [displayCurrency, baseCurrency, isConverted, formatDisplay, legendSuffix, dualMeta])
}

export default useDisplayCurrency
