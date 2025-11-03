import PropTypes from 'prop-types'
import { Chip } from '@mui/material'

export default function CurrencyBadge({ code, size = 'small', variant = 'outlined', title }) {
  if (!code) return null
  return (
    <Chip
      label={code}
      size={size}
      variant={variant}
      color="primary"
      sx={{ fontWeight: 600, letterSpacing: 0.5 }}
      title={title || `Currency: ${code}`}
    />
  )
}

CurrencyBadge.propTypes = {
  code: PropTypes.string,
  size: PropTypes.oneOf(['small', 'medium']),
  variant: PropTypes.oneOf(['filled', 'outlined']),
  title: PropTypes.string,
}
