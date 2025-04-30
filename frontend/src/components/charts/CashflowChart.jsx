import React, { useMemo } from 'react';
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
} from 'recharts';

export default function CashflowChart({ monthlyData = [] }) {
  const directions = useMemo(
    () => monthlyData[0]?.transactionCategoryDirections || {},
    [monthlyData]
  );

  const categories = useMemo(
    () => Object.keys(directions),
    [directions]
  );

  const data = useMemo(
    () =>
      monthlyData.map(m => {
        const row = {
          month: `M${m.month}`,
          bankBalance: Number(m.bankBalance || 0)
        };
        categories.forEach(cat => {
          const raw = Number(m.transactionCategorySums[cat] || 0);
          row[cat] = directions[cat] === 'NEGATIVE'
            ? -Math.abs(raw)
            : Math.abs(raw);
        });
        return row;
      }),
    [monthlyData, categories, directions]
  );

  const positiveColors = [
    '#2e7d32','#388e3c','#43a047','#66bb6a','#81c784','#a5d6a7'
  ];
  const negativeColors = [
    '#b71c1c','#c62828','#d32f2f','#e53935','#ef5350','#f44336'
  ];

  const colors = useMemo(() => {
    let pi = 0, ni = 0;
    return categories.map(cat =>
      directions[cat] === 'POSITIVE'
        ? positiveColors[(pi++) % positiveColors.length]
        : negativeColors[(ni++) % negativeColors.length]
    );
  }, [categories, directions]);

  if (!data.length || !categories.length) return null;

  return (
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
          tick={{ fontSize: 12 }}
          width={40}
        />

        <YAxis
          yAxisId="right"
          orientation="right"
          domain={['auto','auto']}
          tick={{ fontSize: 12 }}
          width={40}
        />

        <Tooltip />

        <Legend
          layout="horizontal"
          verticalAlign="bottom"
          wrapperStyle={{ paddingTop: 20 }}
        />

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
          dataKey="bankBalance"
          stroke="#222"
          dot={{ r: 3 }}
          yAxisId="right"
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
}
