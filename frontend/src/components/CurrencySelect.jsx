import React from 'react'
import { FormControl, InputLabel, Select, MenuItem, FormHelperText } from '@mui/material'
import { CURRENCIES } from '../constants/currencies'
import PropTypes from 'prop-types'


export default function CurrencySelect({
  value,
  onChange,
  label = 'Currency',
  exclude = [],
  disabledOptions = [],
  helperText,
  size = 'small',
  fullWidth = true,
  variant = 'outlined',
  sx,
  id,
}) {
  const handleChange = (e) => {
    onChange?.(e.target.value)
  }

  const options = CURRENCIES.filter(c => !exclude.includes(c))

  const selectId = id || `${label.replaceAll(/\s+/g,'-').toLowerCase()}-select`

  return (
    <FormControl size={size} fullWidth={fullWidth} variant={variant} sx={sx}>
      <InputLabel id={`${selectId}-label`}>{label}</InputLabel>
      <Select
        labelId={`${selectId}-label`}
        id={selectId}
        value={value}
        label={label}
        onChange={handleChange}
        aria-label={label}
        aria-describedby={helperText ? `${selectId}-help` : undefined}
      >
        {options.map(code => (
          <MenuItem key={code} value={code} disabled={disabledOptions.includes(code)}>
            {code}
          </MenuItem>
        ))}
      </Select>
      {helperText && <FormHelperText id={`${selectId}-help`}>{helperText}</FormHelperText>}
    </FormControl>
  )
}

CurrencySelect.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string,
  exclude: PropTypes.arrayOf(PropTypes.string),
  disabledOptions: PropTypes.arrayOf(PropTypes.string),
  helperText: PropTypes.node,
  size: PropTypes.oneOf(['small','medium']),
  fullWidth: PropTypes.bool,
  variant: PropTypes.oneOf(['outlined','filled','standard']),
  sx: PropTypes.object,
  id: PropTypes.string,
}
