// src/components/YearBalanceForm.jsx
import React, { useState } from 'react'
import MyButton from './MyButton'
import '../styles/YearBalanceForm.css'

export default function YearBalanceForm({ years, onSubmit }) {
  const [year, setYear] = useState(years[0])
  const [balance, setBalance] = useState('')

  return (
    <div className="ybf-container">
      <h2 className="ybf-title">Select Year &amp; Starting Balance</h2>
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
      <MyButton
        variant="success"
        disabled={!balance}
        onClick={() => onSubmit(year, balance)}
      >
        See KPIs
      </MyButton>
    </div>
  )
}
