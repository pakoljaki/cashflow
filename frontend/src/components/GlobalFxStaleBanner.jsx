import { Alert, Box, Tooltip } from '@mui/material'
import PropTypes from 'prop-types'
import { stalenessColor } from '../utils/rateStaleness'
import { useCurrency } from '../context/CurrencyContext'
import useAnalytics from '../analytics/useAnalytics'

export default function GlobalFxStaleBanner({ monthlyData = [] }) {
  const { fxEnabled, fxFlags, warnAfterDays, staleAfterDays, displayCurrency, basePlanCurrency, aggregateRateStaleness } = useCurrency()
  const logEvent = useAnalytics('GlobalFxStaleBanner')
  if (!fxEnabled) return null
  if (!fxFlags?.rateBanner) return null // reuse same flag for simplicity; could split later
  if (displayCurrency === basePlanCurrency) return null

  const agg = aggregateRateStaleness(monthlyData, { warnAfterDays, staleAfterDays })
  if (!agg || agg.level === 'fresh' || agg.level === 'unknown') return null
  logEvent('global_rate_banner', { level: agg.level, maxDays: agg.maxDays })
  const color = stalenessColor(agg.level)
  const msg = agg.level === 'stale'
    ? `Some FX rates are stale (oldest ${agg.maxDays} days; threshold ${staleAfterDays}).`
    : `Some FX rates are aging (oldest ${agg.maxDays} days; warn ${warnAfterDays}).`

  return (
    <Box sx={{ mb: 1 }}>
      <Tooltip title={`Oldest rate date: ${agg.oldestDate || 'n/a'}`}> 
        <Alert severity={agg.level === 'stale' ? 'error' : 'warning'} sx={{ bgcolor: color, color: 'common.white' }}>
          {msg} Consider refreshing FX data.
        </Alert>
      </Tooltip>
    </Box>
  )
}

GlobalFxStaleBanner.propTypes = {
  monthlyData: PropTypes.array,
}
