import PropTypes from 'prop-types'
import { Alert, Box } from '@mui/material'
import { classifyStaleness, stalenessColor } from '../utils/rateStaleness'
import { useCurrency } from '../context/CurrencyContext'
import useAnalytics from '../analytics/useAnalytics'

export default function FxRateBanner({ monthlyData = [] }) {
  const { displayCurrency, basePlanCurrency, fxEnabled, fxSettings, fxFlags } = useCurrency()
  const logEvent = useAnalytics('FxRateBanner')
  if (!fxFlags?.rateBanner) return null
  if (!fxEnabled) return null
  if (displayCurrency === basePlanCurrency) return null
  const first = monthlyData[0]
  const rateDate = first?.rateDate
  if (!rateDate) return null
  const warnAfterDays = fxSettings?.warnAfterDays ?? 2
  const staleAfterDays = fxSettings?.staleAfterDays ?? 5
  const { level, days } = classifyStaleness(rateDate, { warnAfterDays, staleAfterDays })
  if (level === 'fresh') return null
  logEvent('rate_banner', { level, days })
  const color = stalenessColor(level)
  const msg = level === 'warn'
    ? `FX rate is ${days} days old (warn threshold ${warnAfterDays}).`
    : `FX rate is ${days} days old (stale threshold ${staleAfterDays}).`
  return (
    <Box sx={{ mb: 1 }}>
      <Alert severity={level === 'stale' ? 'error' : 'warning'} sx={{ bgcolor: color, color: 'common.white' }}>
        {msg} Consider refreshing rates.
      </Alert>
    </Box>
  )
}

FxRateBanner.propTypes = {
  monthlyData: PropTypes.array,
}