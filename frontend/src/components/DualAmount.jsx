import PropTypes from 'prop-types'
import { Tooltip, Box } from '@mui/material'
import CurrencyBadge from './CurrencyBadge'
import { useCurrency } from '../context/AppContext'
import { useDisplayCurrency } from '../hooks/useDisplayCurrency'


export default function DualAmount({ dual, sign }) {
  const { fxFlags } = useCurrency()
  const { displayCurrency } = useDisplayCurrency()
  if (!dual) return null
  const disabled = fxFlags?.dualAmounts === false
  if (dual.single || disabled) {
    return <Box component="span" sx={{ fontWeight: 'bold' }}>{sign}{dual.nativeFormatted}</Box>
  }
  const tooltip = dual.tooltip || ''
  return (
    <Tooltip title={tooltip} placement="top" arrow>
      <Box component="span" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
        <Box component="span" sx={{ fontWeight: 'bold' }}>{sign}{dual.nativeFormatted}</Box>
        <Box component="span" sx={{ fontSize: '0.7rem', opacity: 0.7 }}>
          ({dual.convertedFormatted} <CurrencyBadge code={dual.displayCurrency || displayCurrency} />)
        </Box>
      </Box>
    </Tooltip>
  )
}

DualAmount.propTypes = {
  dual: PropTypes.shape({
    single: PropTypes.bool,
    nativeFormatted: PropTypes.string,
    convertedFormatted: PropTypes.string,
    tooltip: PropTypes.string,
    displayCurrency: PropTypes.string,
    currency: PropTypes.string,
  }),
  sign: PropTypes.string,
}
