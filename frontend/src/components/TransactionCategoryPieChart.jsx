import { useState, useEffect } from 'react'
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Tooltip,
  Cell,
  Legend
} from 'recharts'
import { formatAmount } from '../utils/numberFormatter'

const COLORS = [
  '#0088FE','#00C49F','#FFBB28','#FF8042',
  '#AF19FF','#FF4560','#775DD0','#00E396'
]

export default function TransactionCategoryPieChart({ data, chartType, title }) {
  const [cats, setCats] = useState([])

  useEffect(() => {
    const token = localStorage.getItem('token')
    fetch('/api/categories', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(r => r.json())
      .then(setCats)
      .catch(console.error)
  }, [])

  const desiredDir = chartType === 'INCOME' ? 'POSITIVE' : 'NEGATIVE'

  // initialize every transaction category of this direction to zero
  const agg = cats
    .filter(tc => tc.direction === desiredDir)
    .reduce((acc, tc) => ({ ...acc, [tc.name]: 0 }), {})

  // sum over every month’s transactionCategorySums
  data.forEach(month => {
    const sums = month.transactionCategorySums || {}
    Object.entries(sums).forEach(([name, amt]) => {
      if (name in agg) {
        agg[name] += Number(amt)
      }
    })
  })

  const chartData = cats
    .filter(tc => tc.direction === desiredDir)
    .map((tc, i) => ({
      name: tc.name,
      value: agg[tc.name] || 0
    }))

  return (
    <div className="pie-chart-container">
      <h4 className="pie-title">{title}</h4>
      <ResponsiveContainer width="100%" height={300}>
        <PieChart>
          <Pie
            data={chartData}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={80}
            label
          >
            {chartData.map((_, i) => (
              <Cell key={i} fill={COLORS[i % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip formatter={v => formatAmount(v)} />
          <Legend
            layout="horizontal"
            verticalAlign="bottom"
            align="center"
            wrapperStyle={{ paddingTop: 10, fontSize: '0.8rem', flexWrap: 'wrap' }}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
