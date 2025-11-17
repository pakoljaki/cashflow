import '../styles/monthlydatatable.css'
import { formatAmount } from '../utils/numberFormatter'
import PropTypes from 'prop-types'
import DualAmount from './DualAmount'
import { useCurrency } from '../context/CurrencyContext'

export default function MonthlyDataTable({ startBalance = 0, originalStartBalance, baseCurrency = 'HUF', monthlyData = [] }) {
  const { displayCurrency } = useCurrency()
  if (!monthlyData.length) return null
  const rows   = [...monthlyData].sort((a, b) => a.month - b.month)
  const months = rows.map(r => r.month)

  // Collect both income and expense categories separately
  const incomeCategories = new Set()
  const expenseCategories = new Set()
  for (const r of rows) {
    // Use new separate maps if available; fall back to legacy combined map for backward compat
    const incomeMap = r.incomeAccountingCategorySums || {}
    const expenseMap = r.expenseAccountingCategorySums || {}
    
    for (const c of Object.keys(incomeMap)) {
      if (c && c.trim()) { // Only add non-empty category names
        incomeCategories.add(c)
      }
    }
    for (const c of Object.keys(expenseMap)) {
      if (c && c.trim()) { // Only add non-empty category names
        expenseCategories.add(c)
      }
    }
  }

  const incomesConverted  = rows.map(r => Number(r.totalIncome))
  const expensesConverted = rows.map(r => Number(r.totalExpense))
  const incomesOriginal   = rows.map(r => Number(r.originalTotalIncome ?? r.totalIncome))
  const expensesOriginal  = rows.map(r => Number(r.originalTotalExpense ?? r.totalExpense))

  const openBalancesConverted = []
  const openBalancesOriginal  = []
  for (let i = 0; i < incomesConverted.length; i++) {
    if (i === 0) {
      openBalancesConverted[i] = Number(startBalance)
      openBalancesOriginal[i]  = Number(originalStartBalance ?? startBalance)
    } else {
      openBalancesConverted[i] = openBalancesConverted[i - 1] + incomesConverted[i - 1] - expensesConverted[i - 1]
      openBalancesOriginal[i]  = openBalancesOriginal[i - 1] + incomesOriginal[i - 1] - expensesOriginal[i - 1]
    }
  }

  const netFlowsConverted = incomesConverted.map((inc, i) => inc - expensesConverted[i])
  const netFlowsOriginal  = incomesOriginal.map((inc, i) => inc - expensesOriginal[i])
  const endBalancesConverted = openBalancesConverted.map((ob, i) => ob + netFlowsConverted[i])
  const endBalancesOriginal  = openBalancesOriginal.map((ob, i) => ob + netFlowsOriginal[i])

  const hasAnyOriginals = rows.some(r => r.originalTotalIncome != null || r.originalTotalExpense != null)
  const isConverted = displayCurrency !== baseCurrency && hasAnyOriginals
  const legacyMode = displayCurrency !== baseCurrency && !hasAnyOriginals

  const buildTooltip = (rateDate, rateSource) => {
    if (!rateDate) return 'Converted amount'
    let tip = 'Rate date: ' + rateDate
    if (rateSource) tip += ' (' + rateSource + ')'
    return tip
  }

  return (
    <div className="mdt-container">
      {legacyMode && (
        <div style={{ marginBottom:'0.35rem', fontSize:'0.65rem', color:'#666' }} role="note">
          Legacy data: original currency amounts unavailable – showing converted values only.
        </div>
      )}
      <table className="mdt-table">
        <caption style={{ captionSide:'top', textAlign:'left', fontSize:'0.65rem', padding:'0 0 4px 0' }}>
          {(() => {
            if (displayCurrency === baseCurrency) return (<span>Values shown in {baseCurrency} (plan base currency).</span>)
            if (legacyMode) return (<span>Values shown in converted {displayCurrency} (original {baseCurrency} unavailable).</span>)
            return (<span>Dual figures: bold is native {baseCurrency}; parentheses show converted {displayCurrency}.</span>)
          })()}
        </caption>
        <thead>
          <tr>
            <th>Item</th>
            {months.map(m => (
              <th key={m}>{`M${m}`}</th>
            ))}
          </tr>
          {isConverted && (
            <tr className="mdt-subhead">
              <th />
              {months.map(m => (
                <th key={`sub-${m}`}>
                  <span style={{ fontSize: '0.65rem', fontWeight: 500 }}>
                    {baseCurrency} → {displayCurrency}
                  </span>
                </th>
              ))}
            </tr>
          )}
        </thead>
        <tbody>
          <tr className="mdt-divider-bottom">
            <td>Open Balance</td>
            {openBalancesConverted.map((val, i) => {
              const converted = val
              const original = openBalancesOriginal[i]
              const rateDate = rows[i]?.rateDate || rows[i-1]?.rateDate
              const rateSource = rows[i]?.rateSource || rows[i-1]?.rateSource
              const single = legacyMode || !isConverted || original === converted
              const tooltip = buildTooltip(rateDate, rateSource)
              return (
                <td key={`open-${months[i]}`}>
                  {single ? formatAmount(converted, { currency: displayCurrency }) : (
                    <DualAmount dual={{
                      single: false,
                      nativeFormatted: formatAmount(original, { currency: baseCurrency }),
                      convertedFormatted: formatAmount(converted, { currency: displayCurrency }),
                      tooltip,
                      displayCurrency,
                      currency: baseCurrency,
                    }} />
                  )}
                </td>
              )
            })}
          </tr>
          <tr className="mdt-row-divider">
            <td colSpan={months.length + 1} />
          </tr>
          {/* Income categories - always shown as positive */}
          {Array.from(incomeCategories).sort().map(cat => (
            <tr key={cat}>
              <td>{cat}</td>
              {rows.map((r) => {
                const rawValue = Number(r.incomeAccountingCategorySums?.[cat] || 0)
                const cls = rawValue > 0 ? 'mdt-positive' : 'mdt-zero'
                return (
                  <td key={`${cat}-${r.month}`}>
                    <span className={cls}>{formatAmount(rawValue, { currency: displayCurrency })}</span>
                  </td>
                )
              })}
            </tr>
          ))}
          {/* Expense categories - always shown as negative */}
          {Array.from(expenseCategories).sort().map(cat => (
            <tr key={cat}>
              <td>{cat}</td>
              {rows.map((r) => {
                const rawValue = Number(r.expenseAccountingCategorySums?.[cat] || 0)
                const cls = rawValue > 0 ? 'mdt-negative' : 'mdt-zero'
                return (
                  <td key={`${cat}-${r.month}`}>
                    <span className={cls}>{formatAmount(-rawValue, { currency: displayCurrency })}</span>
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
            {incomesConverted.map((v, i) => {
              const original = incomesOriginal[i]
              const rateDate = rows[i]?.rateDate
              const rateSource = rows[i]?.rateSource
              const single = legacyMode || !isConverted || original === v
              const tooltip = buildTooltip(rateDate, rateSource)
              return (
                <td key={`income-${months[i]}`}>
                  {single ? (
                    <span className="mdt-positive">{formatAmount(v, { currency: displayCurrency })}</span>
                  ) : (
                    <DualAmount dual={{
                      single: false,
                      nativeFormatted: formatAmount(original, { currency: baseCurrency }),
                      convertedFormatted: formatAmount(v, { currency: displayCurrency }),
                      tooltip,
                      displayCurrency,
                      currency: baseCurrency,
                    }} />
                  )}
                </td>
              )
            })}
          </tr>
          <tr>
            <td>Total Expense</td>
            {expensesConverted.map((v, i) => {
              const original = expensesOriginal[i]
              const rateDate = rows[i]?.rateDate
              const rateSource = rows[i]?.rateSource
              const single = legacyMode || !isConverted || original === v
              const tooltip = buildTooltip(rateDate, rateSource)
              return (
                <td key={`expense-${months[i]}`}>
                  {single ? (
                    <span className="mdt-negative">{formatAmount(-v, { currency: displayCurrency })}</span>
                  ) : (
                    <DualAmount dual={{
                      single: false,
                      nativeFormatted: formatAmount(original, { currency: baseCurrency }),
                      convertedFormatted: formatAmount(v, { currency: displayCurrency }),
                      tooltip,
                      displayCurrency,
                      currency: baseCurrency,
                    }} />
                  )}
                </td>
              )
            })}
          </tr>
          <tr>
            <td>Net Cash Flow</td>
            {netFlowsConverted.map((v, i) => {
              const original = netFlowsOriginal[i]
              const rateDate = rows[i]?.rateDate
              const rateSource = rows[i]?.rateSource
              const single = legacyMode || !isConverted || original === v
              const tooltip = buildTooltip(rateDate, rateSource)
              return (
                <td key={`net-${months[i]}`}>
                  {single ? (
                    <span className={v < 0 ? 'mdt-negative' : 'mdt-positive'}>
                      {formatAmount(v, { currency: displayCurrency })}
                    </span>
                  ) : (
                    <DualAmount dual={{
                      single: false,
                      nativeFormatted: formatAmount(original, { currency: baseCurrency }),
                      convertedFormatted: formatAmount(v, { currency: displayCurrency }),
                      tooltip,
                      displayCurrency,
                      currency: baseCurrency,
                    }} />
                  )}
                </td>
              )
            })}
          </tr>
          <tr className="mdt-divider-top">
            <td>End Balance</td>
            {endBalancesConverted.map((v, i) => {
              const original = endBalancesOriginal[i]
              const rateDate = rows[i]?.rateDate
              const rateSource = rows[i]?.rateSource
              const single = legacyMode || !isConverted || original === v
              const tooltip = buildTooltip(rateDate, rateSource)
              return (
                <td key={`end-${months[i]}`}>
                  {single ? formatAmount(v, { currency: displayCurrency }) : (
                    <DualAmount dual={{
                      single: false,
                      nativeFormatted: formatAmount(original, { currency: baseCurrency }),
                      convertedFormatted: formatAmount(v, { currency: displayCurrency }),
                      tooltip,
                      displayCurrency,
                      currency: baseCurrency,
                    }} />
                  )}
                </td>
              )
            })}
          </tr>
        </tbody>
      </table>
    </div>
  )
}

MonthlyDataTable.propTypes = {
  startBalance: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  originalStartBalance: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  baseCurrency: PropTypes.string,
  monthlyData: PropTypes.arrayOf(PropTypes.shape({
    month: PropTypes.number,
    totalIncome: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    totalExpense: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    accountingCategorySums: PropTypes.object,
    rateDate: PropTypes.string,
    rateSource: PropTypes.string,
    originalTotalIncome: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    originalTotalExpense: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    originalNetCashFlow: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    originalBankBalance: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  })),
}
