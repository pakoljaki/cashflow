import React from 'react'
import '../styles/monthlydatatable.css'
import { amountFormatter } from '../utils/numberFormatter'

export default function MonthlyDataTable({ startBalance = 0, monthlyData = [] }) {
  if (!monthlyData.length) return null

  const rows   = [...monthlyData].sort((a, b) => a.month - b.month)
  const months = rows.map(r => r.month)

  const categorySet = new Set()
  rows.forEach(r =>
    Object.keys(r.accountingCategorySums || {}).forEach(c =>
      categorySet.add(c)
    )
  )
  const categories = Array.from(categorySet)

  const incomes  = rows.map(r => Number(r.totalIncome))
  const expenses = rows.map(r => Number(r.totalExpense))

  const openBalances = []
  for (let i = 0; i < incomes.length; i++) {
    if (i === 0) {
      openBalances[i] = Number(startBalance)
    } else {
      openBalances[i] = openBalances[i - 1] + incomes[i - 1] - expenses[i - 1]
    }
  }

  const netFlows = incomes.map((inc, i) => inc - expenses[i])
  const endBalances = openBalances.map((ob, i) => ob + netFlows[i])

  const expenseCodes = ['COGS','OPEX','DEPR','TAX','FIN','OTHER_EXP','REPAY']

  return (
    <div className="mdt-container">
      <table className="mdt-table">
        <thead>
          <tr>
            <th>Item</th>
            {months.map(m => (
              <th key={m}>{`M${m}`}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          <tr className="mdt-divider-bottom">
            <td>Open Balance</td>
            {openBalances.map((val, i) => (
              <td key={i}>{amountFormatter.format(val)}</td>
            ))}
          </tr>
          <tr className="mdt-row-divider">
            <td colSpan={months.length + 1} />
          </tr>
          {categories.map(cat => (
            <tr key={cat}>
              <td>{cat}</td>
              {rows.map((r, i) => {
                let raw = Number(r.accountingCategorySums[cat] || 0)
                if (expenseCodes.includes(cat)) raw = -Math.abs(raw)
                const cls = raw < 0 ? 'mdt-negative' : 'mdt-positive'
                return (
                  <td key={i}>
                    <span className={cls}>
                      {amountFormatter.format(raw)}
                    </span>
                  </td>
                )
              })}
            </tr>
          ))}
          <tr className="mdt-row-divider">
            <td colSpan={months.length + 1} />
          </tr>
          <tr>
            <td>Total Income</td>
            {incomes.map((v, i) => (
              <td key={i}>
                <span className="mdt-positive">
                  {amountFormatter.format(v)}
                </span>
              </td>
            ))}
          </tr>
          <tr>
            <td>Total Expense</td>
            {expenses.map((v, i) => (
              <td key={i}>
                <span className="mdt-negative">
                  {amountFormatter.format(-v)}
                </span>
              </td>
            ))}
          </tr>
          <tr>
            <td>Net Cash Flow</td>
            {netFlows.map((v, i) => (
              <td key={i}>
                <span className={v < 0 ? 'mdt-negative' : 'mdt-positive'}>
                  {amountFormatter.format(v)}
                </span>
              </td>
            ))}
          </tr>
          <tr className="mdt-divider-top">
            <td>End Balance</td>
            {endBalances.map((v, i) => (
              <td key={i}>{amountFormatter.format(v)}</td>
            ))}
          </tr>
        </tbody>
      </table>
    </div>
  )
}
