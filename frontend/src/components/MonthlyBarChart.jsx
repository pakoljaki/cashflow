import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import PropTypes from 'prop-types'
import { formatAmount } from '../utils/numberFormatter'

const MonthlyBarChart = ({ data = [], displayCurrency, baseCurrency, showLegendHint = true }) => {
  if (!data.length) return null
  const formattedData = data.map(it => ({
    month: `M${it.month}`,
    Income: Number(it.totalIncome || 0),
    Expense: Number(it.totalExpense || 0)
  }))
  const converted = displayCurrency !== baseCurrency
  // Use the actual currencies from props instead of context
  const legendSuffix = converted ? `${baseCurrency}→${displayCurrency}` : displayCurrency
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={formattedData} margin={{ top: 10, right: 30, left: 0, bottom: 10 }}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="month" />
        <YAxis tickFormatter={v => formatAmount(v, { currency: displayCurrency })} label={{ value: displayCurrency, angle: -90, position: 'insideLeft', fontSize: 10 }} />
  <Tooltip formatter={(v, name) => [formatAmount(v, { currency: displayCurrency }), `${name} (${legendSuffix})`]} />
  <Legend formatter={(value) => `${value} (${legendSuffix})`} />
        <Bar dataKey="Income" name={converted ? 'Income (converted)' : 'Income'} fill="#82ca9d" />
        <Bar dataKey="Expense" name={converted ? 'Expense (converted)' : 'Expense'} fill="#ff4d4f" />
      </BarChart>
      {showLegendHint && converted && (
        <div style={{ fontSize: '0.65rem', marginTop: 4, textAlign: 'right', opacity: 0.75 }}>
          Values converted from {baseCurrency} → {displayCurrency}
        </div>
      )}
    </ResponsiveContainer>
  )
}

MonthlyBarChart.propTypes = {
  data: PropTypes.arrayOf(PropTypes.shape({
    month: PropTypes.number,
    totalIncome: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
    totalExpense: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  })),
  displayCurrency: PropTypes.string.isRequired,
  baseCurrency: PropTypes.string,
  showLegendHint: PropTypes.bool,
}

export default MonthlyBarChart
