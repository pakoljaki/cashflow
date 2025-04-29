// src/components/charts/CashflowChart.jsx
import React, { useMemo } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend
} from 'recharts';

/**
 * Props:
 *   monthlyData: Array<{
 *     month: number,
 *     accountingCategorySums: { [category: string]: number },
 *     transactionCategorySums: { [category: string]: number }
 *   }>
 */
export default function CashflowChart({ monthlyData }) {
  // 1) Collect all category names
  const categories = useMemo(() => {
    const set = new Set();
    monthlyData.forEach(m => {
      Object.keys(m.accountingCategorySums || {}).forEach(c => set.add(c));
      Object.keys(m.transactionCategorySums || {}).forEach(c => set.add(c));
    });
    return Array.from(set);
  }, [monthlyData]);

  // 2) Build the chart data array
  const data = useMemo(() => {
    return monthlyData.map(m => {
      const obj = {
        // you could format month → 'May 2018' if you pass down year/month strings
        month: `M${m.month}`
      };
      categories.forEach(cat => {
        const a = m.accountingCategorySums[cat] || 0;
        const t = m.transactionCategorySums[cat] || 0;
        obj[cat] = Number(a) + Number(t);
      });
      return obj;
    });
  }, [monthlyData, categories]);

  // 3) A simple palette—swap these out or style with CSS
  const colors = [
    '#4caf50','#ffca28','#2196f3','#ff7043',
    '#9575cd','#26a69a','#ef5350','#ec407a'
  ];

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart
        data={data}
        margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
      >
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="month" />
        <YAxis />
        <Tooltip />
        <Legend />
        {categories.map((cat, i) => (
          <Bar
            key={cat}
            dataKey={cat}
            stackId="stack"               // one stack group → positives stack up, negatives down
            fill={colors[i % colors.length]}
          />
        ))}
      </BarChart>
    </ResponsiveContainer>
  );
}
