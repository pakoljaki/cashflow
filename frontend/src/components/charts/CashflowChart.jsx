import React, { useState, useMemo } from 'react'
import { Box, FormControl, InputLabel, Select, MenuItem } from '@mui/material'
import {
  ResponsiveContainer,
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine
} from 'recharts'
import { amountFormatter } from '../../utils/numberFormatter'

export default function CashflowChart({ monthlyData = [] }) {
  const [selectedScenario, setSelectedScenario] = useState('REALISTIC')
  const directions = useMemo(() => monthlyData[0]?.directions || {}, [monthlyData])
  const categories = useMemo(() => monthlyData[0]?.categories || [], [monthlyData])
  const data = useMemo(() => monthlyData.map(entry => {
    const row = { month: entry.month }
    categories.forEach(cat => {
      row[cat] = entry.sums[selectedScenario]?.[cat] ?? 0
    })
    row.bankBalanceRealistic = Number(entry.bankBalance.REALISTIC || 0)
    row.bankBalanceWorst     = Number(entry.bankBalance.WORST     || 0)
    row.bankBalanceBest      = Number(entry.bankBalance.BEST      || 0)
    return row
  }), [monthlyData, selectedScenario, categories])

  const positiveColors = ['#2e7d32','#388e3c','#43a047','#66bb6a','#81c784','#a5d6a7']
  const negativeColors = ['#b71c1c','#c62828','#d32f2f','#e53935','#ef5350','#f44336']
  const colors = useMemo(() => {
    let pi = 0, ni = 0
    return categories.map(cat =>
      directions[cat] === 'POSITIVE'
        ? positiveColors[(pi++) % positiveColors.length]
        : negativeColors[(ni++) % negativeColors.length]
    )
  }, [categories, directions])

  if (!data.length || !categories.length) return null

  return (
    <Box>
      <FormControl variant="outlined" size="small" sx={{ mb: 2, minWidth: 200 }}>
        <InputLabel>Scenario</InputLabel>
        <Select
          value={selectedScenario}
          label="Scenario"
          onChange={e => setSelectedScenario(e.target.value)}
        >
          <MenuItem value="REALISTIC">Realistic</MenuItem>
          <MenuItem value="WORST">Worst</MenuItem>
          <MenuItem value="BEST">Best</MenuItem>
        </Select>
      </FormControl>

      <ResponsiveContainer width="100%" height={400}>
        <ComposedChart
          data={data}
          stackOffset="sign"
          margin={{ top: 20, right: 50, left: 20, bottom: 20 }}
        >
          <CartesianGrid strokeDasharray="3 3" />
          <ReferenceLine yAxisId="left" y={0} stroke="#000" />
          <XAxis dataKey="month" />
          <YAxis
            yAxisId="left"
            orientation="left"
            domain={['auto','auto']}
            tickFormatter={val => amountFormatter.format(val)}
            tick={{ fontSize: 12 }}
            width={40}
          />
          <YAxis
            yAxisId="right"
            orientation="right"
            domain={['auto','auto']}
            tickFormatter={val => amountFormatter.format(val)}
            tick={{ fontSize: 12 }}
            width={40}
          />
          <Tooltip
            formatter={(value, name) => [amountFormatter.format(value), name]}
          />
          <Legend layout="horizontal" verticalAlign="bottom" wrapperStyle={{ paddingTop: 20 }} />
          {categories.map((cat, i) => (
            <Bar
              key={cat}
              dataKey={cat}
              stackId="a"
              fill={colors[i]}
              yAxisId="left"
            />
          ))}
          <Line
            type="monotone"
            dataKey="bankBalanceWorst"
            name="Worst"
            stroke="#888"
            dot={false}
            strokeDasharray="3 3"
            yAxisId="right"
          />
          <Line
            type="monotone"
            dataKey="bankBalanceRealistic"
            name="Realistic"
            stroke="#222"
            dot={{ r: 3 }}
            yAxisId="right"
          />
          <Line
            type="monotone"
            dataKey="bankBalanceBest"
            name="Best"
            stroke="#444"
            dot={false}
            strokeDasharray="5 5"
            yAxisId="right"
          />
        </ComposedChart>
      </ResponsiveContainer>
    </Box>
  )
}
