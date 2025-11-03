import { useState } from 'react'
import PropTypes from 'prop-types'
import CurrencySelect from './CurrencySelect'
import { useCurrency } from '../context/CurrencyContext'
import MyButton from './MyButton'
import { formatAmount } from '../utils/numberFormatter'
import '../styles/YearBalanceForm.css'

export default function YearBalanceForm({ years, onSubmit }) {
  const [year, setYear] = useState(years[0])
  const [balance, setBalance] = useState('')
  const { basePlanCurrency, setBasePlanCurrency } = useCurrency()
  const [localBaseCurrency, setLocalBaseCurrency] = useState(basePlanCurrency || 'HUF')

  const handleSelectCurrency = (code) => {
    setLocalBaseCurrency(code)
    setBasePlanCurrency(code)
  }

  return (
    <div className="ybf-container">
  <h2 className="ybf-title">Select Year, Starting Balance &amp; Base Currency</h2>
      <div className="ybf-year-buttons">
        {years.map(y => (
          <MyButton
            key={y}
            variant={y === year ? 'primary' : 'outline-primary'}
            onClick={() => setYear(y)}
          >
            {y}
          </MyButton>
        ))}
      </div>
      <div className="ybf-input">
        <label htmlFor="startBalance">Starting Balance (HUF):</label>
        <input
          id="startBalance"
          type="number"
          value={balance}
          onChange={e => setBalance(e.target.value)}
          placeholder="Enter starting balance"
        />
      </div>
      <div className="ybf-input" style={{ marginTop: '1rem', width: '100%' }}>
        <CurrencySelect
          label="Base Currency"
          value={localBaseCurrency}
          onChange={handleSelectCurrency}
          helperText="Currency used for starting balance & plan base"
          fullWidth
        />
      </div>
      {balance && (
  <p className="ybf-helper">{formatAmount(Number(balance), { currency: localBaseCurrency })} {localBaseCurrency}</p>
      )}
      <MyButton
        variant="success"
        disabled={!balance}
        onClick={() => onSubmit(year, balance, localBaseCurrency)}
      >
        See KPIs
      </MyButton>
    </div>
  )
}

YearBalanceForm.propTypes = {
  years: PropTypes.arrayOf(PropTypes.number).isRequired,
  onSubmit: PropTypes.func.isRequired,
}
