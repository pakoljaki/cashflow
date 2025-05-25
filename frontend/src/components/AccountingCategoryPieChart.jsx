import { useState, useEffect } from 'react'
import { PieChart, Pie, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts'

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#AF19FF', '#FF4560', '#775DD0', '#00E396']

export default function AccountingCategoryPieChart({ data, chartType, title }) {
  const [categoriesList, setCategoriesList] = useState([])

  useEffect(() => {
    const token = localStorage.getItem('token')
    fetch('/api/accounting-categories', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(r => r.json())
      .then(setCategoriesList)
      .catch(console.error)
  }, [])

  const desiredDir = chartType === 'INCOME' ? 'POSITIVE' : 'NEGATIVE'
  const agg = {}

  categoriesList
    .filter(c => c.direction === desiredDir)
    .forEach(c => {
      agg[c.code] = 0
    })

  data.forEach(month => {
    const sums = month.accountingCategorySums || {}
    Object.entries(sums).forEach(([code, amt]) => {
      if (agg.hasOwnProperty(code)) {
        const val = Number(amt)
        agg[code] += chartType === 'EXPENSE' ? Math.abs(val) : val
      }
    })
  })

  const chartData = categoriesList
    .filter(c => c.direction === desiredDir)
    .map(c => ({ name: c.displayName, value: agg[c.code] || 0 }))
    .filter(d => d.value > 0)

  return (
    <div className="pie-chart-container">
      <h4 className="pie-title">{title}</h4>
      <ResponsiveContainer width="100%" height={200}>
        <PieChart>
          <Pie data={chartData} dataKey="value" nameKey="name" label>
            {chartData.map((_, i) => (
              <Cell key={i} fill={COLORS[i % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip formatter={v => v.toLocaleString()} />
          <Legend verticalAlign="bottom" height={36} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
