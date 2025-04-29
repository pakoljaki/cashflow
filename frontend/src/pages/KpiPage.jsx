import React, { useState, useEffect } from 'react';
import { Container, Row, Col, Card, Spinner } from 'react-bootstrap';
import MonthlyKpiTable from '../components/MonthlyKpiTable';
import MonthlyBarChart from '../components/MonthlyBarChart';
import AccountingCategoryPieChart from '../components/AccountingCategoryPieChart';
import TransactionCategoryPieChart from '../components/TransactionCategoryPieChart';
import MyButton from '../components/MyButton';

const KpiPage = () => {
  const [kpiData, setKpiData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const dateRange = "2024-01-01 to 2024-12-31";
  const fetchKpiData = async () => {
    setLoading(true);
    try {
      const token = localStorage.getItem('token');
      const response = await fetch('/api/business-kpi?startDate=2024-01-01&endDate=2024-12-31&startBalance=0', { //TODO: THIS WILL HAVE TO REFACTORED TO INCLUDE THE START BALANCE
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!response.ok) {
        throw new Error('Failed to fetch KPI data');
      }
      const data = await response.json();
      setKpiData(data);
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  };
  useEffect(() => {
    fetchKpiData();
  }, []);
  return (
    <Container fluid>
      <Row className="mb-3">
        <Col>
          <h2>KPI Dashboard</h2>
          <h5>Date Range: {dateRange}</h5>
        </Col>
        <Col md="auto">
          <MyButton onClick={fetchKpiData}>Refresh Data</MyButton>
        </Col>
      </Row>
      {loading && <Spinner animation="border" variant="primary" />}
      {error && <div className="text-danger">{error}</div>}
      {kpiData && (
        <Row>
          <Col md={4}>
            <Row className="mb-3">
              <Col xs={6}>
                <AccountingCategoryPieChart data={kpiData.monthlyData} chartType="INCOME" title="Acc. Income" />
              </Col>
              <Col xs={6}>
                <AccountingCategoryPieChart data={kpiData.monthlyData} chartType="EXPENSE" title="Acc. Expense" />
              </Col>
            </Row>
            <Row>
              <Col xs={6}>
                <TransactionCategoryPieChart data={kpiData.monthlyData} chartType="INCOME" title="Tx. Income" />
              </Col>
              <Col xs={6}>
                <TransactionCategoryPieChart data={kpiData.monthlyData} chartType="EXPENSE" title="Tx. Expense" />
              </Col>
            </Row>
          </Col>
          <Col md={8}>
            <MonthlyKpiTable data={kpiData.monthlyData} />
            <MonthlyBarChart data={kpiData.monthlyData} />
          </Col>
        </Row>
      )}
    </Container>
  );
};
export default KpiPage;
