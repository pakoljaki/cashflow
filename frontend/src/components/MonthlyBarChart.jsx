import React from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
const MonthlyBarChart = ({ data }) => {
  const formattedData = data.map(item => ({
    month: item.month,
    Income: Number(item.totalIncome),
    Expense: Number(item.totalExpense)
  }));
  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={formattedData}>
        <CartesianGrid strokeDasharray="3 3"/>
        <XAxis dataKey="month"/>
        <YAxis/>
        <Tooltip/>
        <Legend/>
        <Bar dataKey="Income" fill="#82ca9d"/>
        <Bar dataKey="Expense" fill="#ff4d4f"/>
      </BarChart>
    </ResponsiveContainer>
  );
};
export default MonthlyBarChart;
