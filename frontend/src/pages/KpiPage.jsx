import { useState } from 'react'
import YearBalanceForm from '../components/YearBalanceForm'
import MyButton from '../components/MyButton'
import AccountingCategoryPieChart from '../components/AccountingCategoryPieChart'
import TransactionCategoryPieChart from '../components/TransactionCategoryPieChart'
import MonthlyDataTable from '../components/MonthlyDataTable'
import MonthlyBarChart from '../components/MonthlyBarChart'
import '../styles/KpiPage.css'
import { amountFormatter } from '../utils/numberFormatter'

export default function KpiPage() {
  const [stage, setStage] = useState('form')
  const [kpiData, setKpiData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i)

  const fetchKpis = async (year, startBalance) => {
    setLoading(true)
    setError('')
    const startDate = `${year}-01-01`
    const endDate = `${year}-12-31`
    try {
      const token = localStorage.getItem('token')
      const res = await fetch(
        `/api/business-kpi?startDate=${startDate}&endDate=${endDate}&startBalance=${encodeURIComponent(
          startBalance
        )}`,
        { headers: { Authorization: `Bearer ${token}` } }
      )
      if (!res.ok) throw new Error('Failed to load KPI data')
      const data = await res.json()
      setKpiData({ ...data, year, startBalance: Number(startBalance) })
      setStage('dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const reset = () => {
    setStage('form')
    setKpiData(null)
    setError('')
  }

  return (
    <div className="kpi-container">
      {stage === 'form' && <YearBalanceForm years={years} onSubmit={fetchKpis} />}

      {stage === 'dashboard' && (
        <>
          <header className="kpi-header">
            <h2>KPI Dashboard — {kpiData.year}</h2>
            <p>Start Balance: {amountFormatter.format(kpiData.startBalance)} HUF</p>
            <MyButton variant="outline-primary" onClick={reset}>
              Change Settings
            </MyButton>
          </header>

          {loading && <div className="kpi-loading">Loading...</div>}
          {error && <div className="kpi-error">{error}</div>}

          {!loading && !error && kpiData && (
            <div className="kpi-content">
              {/* 1) Full-width table */}
              <div className="chart-card">
                <MonthlyDataTable
                  startBalance={kpiData.startBalance}
                  monthlyData={kpiData.monthlyData}
                />
              </div>

              {/* 2) Full-width bar chart */}
              <div className="chart-card">
                <MonthlyBarChart data={kpiData.monthlyData} />
              </div>

              {/* 3) Four pie charts in 2×2 grid */}
              <div className="kpi-pie-grid">
                <div className="chart-card">
                  <AccountingCategoryPieChart
                    data={kpiData.monthlyData}
                    chartType="INCOME"
                    title="Accounting Income"
                  />
                </div>
                <div className="chart-card">
                  <AccountingCategoryPieChart
                    data={kpiData.monthlyData}
                    chartType="EXPENSE"
                    title="Accounting Expense"
                  />
                </div>
                <div className="chart-card">
                  <TransactionCategoryPieChart
                    data={kpiData.monthlyData}
                    chartType="INCOME"
                    title="Transaction Income"
                  />
                </div>
                <div className="chart-card">
                  <TransactionCategoryPieChart
                    data={kpiData.monthlyData}
                    chartType="EXPENSE"
                    title="Transaction Expense"
                  />
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
