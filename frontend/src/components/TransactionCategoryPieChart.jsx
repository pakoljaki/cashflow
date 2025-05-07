// src/components/TransactionCategoryPieChart.jsx
import React from 'react'
import { PieChart, Pie, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts'
import '../styles/PieCharts.css'

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#AF19FF', '#FF4560', '#775DD0', '#00E396']

export default function TransactionCategoryPieChart({ data, chartType, title }) {
  const desired = chartType === 'INCOME' ? 'POSITIVE' : 'NEGATIVE'
  const agg = {}
  data.forEach(m => {
    const sums = m.transactionCategorySums || {}
    const dirs = m.transactionCategoryDirections || {}
    Object.entries(sums).forEach(([code, amt]) => {
      if (dirs[code] === desired) {
        agg[code] = (agg[code] || 0) + Number(amt)
      }
    })
  })
  const chartData = Object.entries(agg)
    .map(([name, value]) => ({ name, value }))
    .filter(i => i.value > 0)
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
          <Tooltip />
          <Legend verticalAlign="bottom" height={36} />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
