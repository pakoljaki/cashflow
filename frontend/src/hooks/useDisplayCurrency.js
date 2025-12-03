import { useMemo } from 'react'
import { useCurrency } from '../context/AppContext'
import { formatAmount } from '../utils/numberFormatter'


export function useDisplayCurrency() {
  const { displayCurrency, basePlanCurrency: baseCurrency } = useCurrency()
  const isConverted = displayCurrency !== baseCurrency

  const formatDisplay = (value, opts = {}) => formatAmount(value, { currency: displayCurrency, ...opts })
  const legendSuffix = () => isConverted ? `${baseCurrency}→${displayCurrency}` : displayCurrency

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
