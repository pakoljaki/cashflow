import React from 'react';
import { PieChart, Pie, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts';
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#AF19FF', '#FF4560', '#775DD0', '#00E396'];
const CategoryPieChart = ({ data }) => {
  let categoryAggregates = {};
  data.forEach(month => {
    if(month.accountingCategorySums) {
      Object.keys(month.accountingCategorySums).forEach(code => {
        const amt = Number(month.accountingCategorySums[code]);
        categoryAggregates[code] = (categoryAggregates[code] || 0) + amt;
      });
    }
  });
  const chartData = Object.keys(categoryAggregates).map(code => ({
    name: code,
    value: categoryAggregates[code]
  })).filter(item => item.value !== 0);
  return (
    <ResponsiveContainer width="100%" height={300}>
      <PieChart>
        <Pie data={chartData} dataKey="value" nameKey="name" label>
          {chartData.map((entry, index) => <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>)}
        </Pie>
        <Tooltip/>
        <Legend verticalAlign="bottom" height={36}/>
      </PieChart>
    </ResponsiveContainer>
  );
};
export default CategoryPieChart;
