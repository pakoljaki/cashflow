import { useState } from 'react'
import { useCurrency } from '../context/CurrencyContext'
import { classifyStaleness } from '../utils/rateStaleness'
import CurrencySelect from '../components/CurrencySelect'
import { buildQuery } from '../utils/queryParams'
import YearBalanceForm from '../components/YearBalanceForm'
import MyButton from '../components/MyButton'
import AccountingCategoryPieChart from '../components/AccountingCategoryPieChart'
import TransactionCategoryPieChart from '../components/TransactionCategoryPieChart'
import MonthlyDataTable from '../components/MonthlyDataTable'
import DualAmount from '../components/DualAmount'
import FxRateBanner from '../components/FxRateBanner'
import GlobalFxStaleBanner from '../components/GlobalFxStaleBanner'
import FxHealthPanel from '../components/FxHealthPanel'
import MonthlyBarChart from '../components/MonthlyBarChart'
import '../styles/KpiPage.css'
import { formatAmount } from '../utils/numberFormatter'
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined'
import { Popover } from '@mui/material'
import useAnalytics from '../analytics/useAnalytics'

export default function KpiPage() {
  const [stage, setStage] = useState('form')
  const [kpiData, setKpiData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [refetching, setRefetching] = useState(false) // Soft loading indicator during currency switch
  const [pendingDisplayCurrency, setPendingDisplayCurrency] = useState(null) // Optimistic target currency
  const [error, setError] = useState('')
  const years = Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i)
  const { displayCurrency, effectiveDisplayCurrency, unsupportedDisplayCurrency, setDisplayCurrency, warnAfterDays, staleAfterDays, getCachedKpi, setCachedKpi, fxFlags } = useCurrency()
  const logEvent = useAnalytics('KpiPage')
  const [infoAnchor, setInfoAnchor] = useState(null)
  const openInfo = Boolean(infoAnchor)
  const handleInfoClick = (e) => setInfoAnchor(e.currentTarget)
  const handleInfoClose = () => setInfoAnchor(null)

  // Reset back to initial form stage (used by "Change Settings" button)
  const reset = () => {
    setStage('form')
    setKpiData(null)
    setError('')
  }

  const fetchKpis = async (year, startBalance, baseCurrency, opts) => {
    const isSwitch = !!opts?.isSwitch
    isSwitch ? setRefetching(true) : setLoading(true)
    setError('')
    const startDate = `${year}-01-01`
    const endDate = `${year}-12-31`
    // Check cache first (simple reuse irrespective of staleness; enhancement: add ttl logic)
    const cached = getCachedKpi(year, baseCurrency, displayCurrency)
    if (cached?.data) {
      setKpiData({ ...cached.data, year, startBalance: Number(startBalance), baseCurrency })
      setStage('dashboard')
      isSwitch ? setRefetching(false) : setLoading(false)
      return
    }
    try {
      const token = localStorage.getItem('token')
      const qs = buildQuery({
        startDate,
        endDate,
        startBalance,
        displayCurrency: displayCurrency === 'HUF' ? undefined : displayCurrency,
      })
      const res = await fetch(`/api/business-kpi${qs}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error('Failed to load KPI data')
      const data = await res.json()
      setKpiData({ ...data, year, startBalance: Number(startBalance), baseCurrency })
      setCachedKpi(year, baseCurrency, displayCurrency, data)
      setPendingDisplayCurrency(null) // resolved
      setStage('dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      isSwitch ? setRefetching(false) : setLoading(false)
    }
  }

  return (
    <div className="kpi-container">
      {stage === 'form' && <YearBalanceForm years={years} onSubmit={fetchKpis} />}

      {stage === 'dashboard' && (
        <>
          <header className="kpi-header">
            <h2>KPI Dashboard — {kpiData.year}</h2>
            <p>
              Start Balance:{' '}
              {kpiData.displayCurrency === kpiData.baseCurrency || !kpiData.originalStartBalance ? (
                `${formatAmount(kpiData.startBalance, { currency: kpiData.baseCurrency })} ${kpiData.baseCurrency}`
              ) : (
                <DualAmount
                  dual={{
                    single: false,
                    nativeFormatted: formatAmount(kpiData.originalStartBalance, { currency: kpiData.baseCurrency }),
                    convertedFormatted: formatAmount(kpiData.startBalance, { currency: kpiData.displayCurrency }),
                    tooltip: (() => {
                      if (!kpiData.startBalanceRateDate) return 'Converted amount'
                      let tip = 'Rate date: ' + kpiData.startBalanceRateDate
                      if (kpiData.startBalanceRateSource) tip += ' (' + kpiData.startBalanceRateSource + ')'
                      return tip
                    })(),
                    displayCurrency: kpiData.displayCurrency,
                    currency: kpiData.baseCurrency,
                  }}
                />
              )}
            </p>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
              <CurrencySelect
                label="Display Currency"
                value={pendingDisplayCurrency || displayCurrency}
                onChange={(code) => {
                  if (fxFlags.optimisticSwitch) {
                    setPendingDisplayCurrency(code)
                  }
                  const prev = displayCurrency
                  setDisplayCurrency(code)
                  if (code !== prev) {
                    logEvent('currency_switch', { from: prev, to: code })
                  }
                  fetchKpis(kpiData.year, kpiData.startBalance, kpiData.baseCurrency, { isSwitch: true })
                }}
                helperText="Conversion currency for dashboard metrics"
                sx={{ minWidth: 160 }}
              />
              <button onClick={handleInfoClick} style={{ background:'transparent', border:'none', cursor:'pointer', display:'flex', alignItems:'center' }} aria-label="Conversion info">
                <InfoOutlinedIcon fontSize="small" />
              </button>
              <Popover
                open={openInfo}
                anchorEl={infoAnchor}
                onClose={handleInfoClose}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
              >
                <div style={{ padding:'0.75rem', maxWidth:280, fontSize:'0.75rem' }}>
                  <strong>Currency Conversion</strong>
                  <br />Amounts are shown in <b>{effectiveDisplayCurrency}</b>{unsupportedDisplayCurrency && ' (fallback)'}.
                  <br />Original plan currency values are preserved; hover to view native + converted.
                  <br />Rates may become stale after {warnAfterDays} days (warning) and {staleAfterDays} days (stale).
                  <br />Switching currency uses cached data when possible for speed.
                </div>
              </Popover>
              {unsupportedDisplayCurrency && (
                <span style={{ padding:'4px 6px', background:'#d32f2f', color:'#fff', borderRadius:4, fontSize:'0.65rem' }}
                  title={`Unsupported currency ${displayCurrency}; falling back to ${effectiveDisplayCurrency}`}>Fallback → {effectiveDisplayCurrency}</span>
              )}
              {fxFlags.optimisticSwitch && pendingDisplayCurrency && pendingDisplayCurrency !== displayCurrency && (
                <span style={{ fontSize: '0.65rem', fontStyle: 'italic' }}>Applying {pendingDisplayCurrency}…</span>
              )}
              {fxFlags.stalenessBadges && (() => {
                const rateDate = kpiData.monthlyData?.[0]?.rateDate
                if (!rateDate || displayCurrency === kpiData.baseCurrency) return null
                const status = classifyStaleness(rateDate, { warnAfterDays, staleAfterDays })
                if (status.level === 'fresh' || status.level === 'unknown') return null
                if (status.level !== 'fresh' && status.level !== 'unknown') {
                  logEvent('rate_staleness_badge', { level: status.level, days: status.days })
                }
                return (
                  <span
                    style={{
                      padding: '4px 8px',
                      borderRadius: 4,
                      fontSize: '0.7rem',
                      fontWeight: 600,
                      background: status.level === 'stale' ? '#d32f2f' : '#ed6c02',
                      color: '#fff'
                    }}
                    title={`FX rate ${status.level}; ${status.days} day(s) old; date: ${rateDate}`}
                  >
                    {status.level === 'stale' ? 'Rate STALE' : 'Rate Aging'}
                  </span>
                )
              })()}
              <MyButton variant="outline-primary" onClick={reset}>
                Change Settings
              </MyButton>
            </div>
          </header>

          {loading && <div className="kpi-loading">Loading...</div>}
          <div aria-live="polite" style={{ minHeight: '1rem' }}>
            {refetching && !loading && (
              <span style={{ marginBottom: '0.75rem', fontSize: '0.8rem', color: '#666', display:'inline-block' }}>
                Updating conversion to {pendingDisplayCurrency || displayCurrency}…
              </span>
            )}
          </div>
          {error && <div className="kpi-error">{error}</div>}

          {!loading && !error && kpiData && (
            <div className="kpi-content">
              {fxFlags.rateBanner && <FxRateBanner monthlyData={kpiData.monthlyData} />}
              {fxFlags.rateBanner && <GlobalFxStaleBanner monthlyData={kpiData.monthlyData} />}
              {fxFlags.healthPanel && <FxHealthPanel />}
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
                <MonthlyBarChart data={kpiData.monthlyData} displayCurrency={displayCurrency} baseCurrency={kpiData.baseCurrency} />
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
