import { useState } from 'react'
import { useCurrency } from '../context/AppContext'
import { buildQuery } from '../utils/queryParams'
import YearBalanceForm from '../components/YearBalanceForm'
import MyButton from '../components/MyButton'
import AccountingCategoryPieChart from '../components/AccountingCategoryPieChart'
import TransactionCategoryPieChart from '../components/TransactionCategoryPieChart'
import MonthlyDataTable from '../components/MonthlyDataTable'
import MonthlyBarChart from '../components/MonthlyBarChart'
import '../styles/KpiPage.css'
import { formatAmount } from '../utils/numberFormatter'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { Popover } from '@mui/material'

export default function KpiPage() {
  const [stage, setStage] = useState('form')
  const [kpiData, setKpiData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i)
  const { warnAfterDays, getCachedKpi, setCachedKpi } = useCurrency()
  const [infoAnchor, setInfoAnchor] = useState(null)
  const openInfo = Boolean(infoAnchor)
  const handleInfoClick = (e) => setInfoAnchor(e.currentTarget)
  const handleInfoClose = () => setInfoAnchor(null)

  const reset = () => {
    setStage('form')
    setKpiData(null)
    setError('')
  }

  const fetchKpis = async (year, startBalance, baseCurrency) => {
    setLoading(true)
    setError('')
    const startDate = `${year}-01-01`
    const endDate = `${year}-12-31`
    const cached = getCachedKpi(year, baseCurrency, baseCurrency)
    if (cached?.data) {
      setKpiData({ ...cached.data, year, startBalance: Number(startBalance), baseCurrency })
      setStage('dashboard')
      setLoading(false)
      return
    }
    try {
      const token = localStorage.getItem('token')
      const qs = buildQuery({
        startDate,
        endDate,
        startBalance,
        baseCurrency,
      })
      const res = await fetch(`/api/business-kpi${qs}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error('Failed to load KPI data')
      const data = await res.json()
      setKpiData({ ...data, year, startBalance: Number(startBalance), baseCurrency })
      setCachedKpi(year, baseCurrency, baseCurrency, data)
      setStage('dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="kpi-container">
      {stage === 'form' && <YearBalanceForm years={years} onSubmit={fetchKpis} />}

      {stage === 'dashboard' && (
        <>
          <header className="kpi-header">
            <h2>KPI Dashboard — {kpiData.year}</h2>
            <p className="kpi-start-balance">
              Starting Balance: <strong>{formatAmount(kpiData.startBalance, { currency: kpiData.baseCurrency })} {kpiData.baseCurrency}</strong>
            </p>
            <div className="kpi-controls">
              <button onClick={handleInfoClick} className="kpi-info-btn" aria-label="Information about this dashboard">
                <InfoOutlinedIcon fontSize="small" />
              </button>
              <Popover
                open={openInfo}
                anchorEl={infoAnchor}
                onClose={handleInfoClose}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
              >
                <div className="kpi-info-popover">
                  <strong>About This Dashboard</strong>
                  <p>All amounts are displayed in <strong>{kpiData.baseCurrency}</strong>, the currency you selected for your starting balance.</p>
                  <p>To change currencies, click "Change Settings" below to select a different starting balance currency.</p>
                  {warnAfterDays && <p><small>Rates may become stale after {warnAfterDays} days.</small></p>}
                </div>
              </Popover>
              <MyButton variant="outline-primary" onClick={reset} className="kpi-change-btn">
                Change Settings
              </MyButton>
            </div>
          </header>

          {loading && <div className="kpi-loading">Loading...</div>}
          <div aria-live="polite" style={{ minHeight: '1rem' }}>
            {/* Refetching messages removed since currency switching is no longer available */}
          </div>
          {error && <div className="kpi-error">{error}</div>}

          {!loading && !error && kpiData && (
            <div className="kpi-content">
              {/* 1) Full-width table */}
              <div className="chart-card">
                <MonthlyDataTable
                  startBalance={kpiData.startBalance}
                  originalStartBalance={kpiData.originalStartBalance}
                  baseCurrency={kpiData.baseCurrency}
                  monthlyData={kpiData.monthlyData}
                />
              </div>

              {/* 2) Full-width bar chart */}
              <div className="chart-card">
                <MonthlyBarChart data={kpiData.monthlyData} displayCurrency={kpiData.baseCurrency} baseCurrency={kpiData.baseCurrency} />
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
