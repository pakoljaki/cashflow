import React from 'react';
import { Table } from 'react-bootstrap';
const MonthlyKpiTable = ({ data }) => {
  return (
    <Table striped bordered hover responsive>
      <thead>
        <tr>
          <th>Month</th>
          <th>Total Income (HUF)</th>
          <th>Total Expense (HUF)</th>
          <th>Net Cash Flow (HUF)</th>
          <th>Bank Balance (HUF)</th>
        </tr>
      </thead>
      <tbody>
        {data.map(row => (
          <tr key={row.month}>
            <td>{row.month}</td>
            <td>{Number(row.totalIncome).toLocaleString()}</td>
            <td>{Number(row.totalExpense).toLocaleString()}</td>
            <td>{Number(row.netCashFlow).toLocaleString()}</td>
            <td>{Number(row.bankBalance).toLocaleString()}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};
export default MonthlyKpiTable;
