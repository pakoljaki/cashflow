import React from 'react';
import { PieChart, Pie, Tooltip, Cell, Legend, ResponsiveContainer } from 'recharts';
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#AF19FF', '#FF4560', '#775DD0', '#00E396'];
const TransactionCategoryPieChart = ({ data, chartType, title }) => {
  let aggregates = {};
  data.forEach(month => {
    if (month.transactionCategorySums) {
      Object.keys(month.transactionCategorySums).forEach(code => {
        let amt = month.transactionCategorySums[code];
        if (chartType === "EXPENSE") { amt = amt < 0 ? Math.abs(amt) : 0; }
        else { amt = amt > 0 ? amt : 0; }
        aggregates[code] = (aggregates[code] || 0) + amt;
      });
    }
  });
  const chartData = Object.keys(aggregates).map(code => ({
    name: code,
    value: aggregates[code]
  })).filter(item => item.value > 0);
  return (
    <div>
      <h6 className="text-center">{title}</h6>
      <ResponsiveContainer width="100%" height={250}>
        <PieChart>
          <Pie data={chartData} dataKey="value" nameKey="name" label>
            {chartData.map((entry, index) => (<Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]}/>))}
          </Pie>
          <Tooltip/>
          <Legend verticalAlign="bottom" height={36}/>
        </PieChart>
      </ResponsiveContainer>
    </div>
  );
};
export default TransactionCategoryPieChart;
