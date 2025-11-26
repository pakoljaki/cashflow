import { useState, useEffect, useMemo, useCallback } from 'react'
import PropTypes from 'prop-types'
import { useParams } from 'react-router-dom'
import {
  Box,
  Grid,
  Paper,
  Typography,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  Divider
} from '@mui/material'
import OneTimeForm from '../components/lineitems/OneTimeForm'
import RecurringForm from '../components/lineitems/RecurringForm'
import CategoryForm from '../components/lineitems/CategoryForm'
import CashflowChart from '../components/charts/CashflowChart'
import MonthlyDataTable from '../components/MonthlyDataTable'
import { formatAmount } from '../utils/numberFormatter'
import CurrencyBadge from '../components/CurrencyBadge'
import { useCurrency } from '../context/CurrencyContext'

export default function ScenarioGroupLineItemsPage() {
  const { groupKey } = useParams()
  const { setBasePlanCurrency, displayCurrency } = useCurrency()
  const [plans, setPlans] = useState([])
  const [message, setMessage] = useState('')
  const [selectedForm, setSelectedForm] = useState(null)
  const [monthlyData, setMonthlyData] = useState([])
  const [realKpiData, setRealKpiData] = useState([])

  const fetchPlans = useCallback(() => {
    (async () => {
      const token = localStorage.getItem('token')
      if (!token) {
        setMessage('Not logged in.')
        return
      }
      try {
        const resp = await fetch(`/api/cashflow-plans/group/${groupKey}/plans`, {
          headers: { Authorization: `Bearer ${token}` }
        })
        if (!resp.ok) throw new Error(await resp.text())
        const data = await resp.json()
        setPlans(data)
        const realistic = data.find(p => p.scenario === 'REALISTIC')
        if (realistic?.baseCurrency) setBasePlanCurrency(realistic.baseCurrency)
      } catch (err) {
        setMessage('Error: ' + err.message)
      }
    })()
  }, [groupKey, setBasePlanCurrency])

  useEffect(() => { fetchPlans() }, [fetchPlans])

  useEffect(() => {
    if (!plans.length) return
    const token = localStorage.getItem('token')
    if (!token) {
      setMessage('Not logged in.')
      return
    }
    const fetchKpi = (planId) => {
      const param = displayCurrency && displayCurrency !== 'HUF' ? `?displayCurrency=${encodeURIComponent(displayCurrency)}` : ''
      return fetch(`/api/cashflow-plans/${planId}/monthly-kpi${param}`, {
        headers: { Authorization: `Bearer ${token}` }
      }).then((r) => (r.ok ? r.json() : []))
    }

    const worstPlan     = plans.find((p) => p.scenario === 'WORST')
    const realisticPlan = plans.find((p) => p.scenario === 'REALISTIC')
    const bestPlan      = plans.find((p) => p.scenario === 'BEST')

    Promise.all([
      worstPlan     ? fetchKpi(worstPlan.id)     : Promise.resolve([]),
      realisticPlan ? fetchKpi(realisticPlan.id) : Promise.resolve([]),
      bestPlan      ? fetchKpi(bestPlan.id)      : Promise.resolve([])
    ]).then(([worstData, realData, bestData]) => {
      setRealKpiData(realData)
      if (!realData.length) {
        setMonthlyData([])
        return
      }
      const combined = []
      for (const realItem of realData) {
        const month = realItem.month
        const worstItem = worstData.find((w) => w.month === month) || {}
        const bestItem  = bestData.find((b) => b.month === month)  || {}
        const directions = realItem.transactionCategoryDirections || {}
        const categories = Object.keys(directions)
        const sumsReal = {}
        const sumsWorst = {}
        const sumsBest = {}
        for (const cat of categories) {
          const sr = realItem.transactionCategorySums?.[cat] || 0
          sumsReal[cat] = directions[cat] === 'NEGATIVE' ? -Math.abs(sr) : Math.abs(sr)
          const sw = worstItem.transactionCategorySums?.[cat] || 0
          sumsWorst[cat] = (worstItem.transactionCategoryDirections?.[cat] === 'NEGATIVE') ? -Math.abs(sw) : Math.abs(sw)
          const sb = bestItem.transactionCategorySums?.[cat] || 0
          sumsBest[cat] = (bestItem.transactionCategoryDirections?.[cat] === 'NEGATIVE') ? -Math.abs(sb) : Math.abs(sb)
        }
        combined.push({
          month: `M${month}`,
          directions,
          categories,
          sums: { REALISTIC: sumsReal, WORST: sumsWorst, BEST: sumsBest },
          bankBalance: {
            REALISTIC: realItem.bankBalance,
            WORST: worstItem.bankBalance,
            BEST: bestItem.bankBalance
          }
        })
      }
      setMonthlyData(combined)
    }).catch((err) => {
      console.error(err)
      setMonthlyData([])
      setRealKpiData([])
    })
  }, [plans, displayCurrency])

  const allItems = plans.flatMap((plan) =>
    (plan.lineItems || []).map((item) => ({ planId: plan.id, scenario: plan.scenario, baseCurrency: plan.baseCurrency, ...item }))
  )

  const planCurrencyMap = useMemo(() => {
    const map = new Map()
    for (const p of plans) map.set(p.id, p.baseCurrency)
    return map
  }, [plans])

  const groupedAssumptions = useMemo(() => {
    // We originally grouped strictly by assumptionId. When users add the same logical
    // assumption (same title/date/type/category) to different scenarios at different times,
    // the backend generates distinct assumptionIds, causing separate rows. To make the UI
    // friendlier we collapse items across scenarios using a signature of stable fields.
    // If multiple assumptionIds map to one signature we join them (e.g. "3,6").
    const map = new Map()
    for (const it of allItems) {
      const signature = [
        (it.title || '').trim().toLowerCase(),
        it.type || '',
        // one-time items use transactionDate; recurring/category may have startDate/endDate
        it.transactionDate || it.startDate || '',
        it.categoryId || ''
      ].join('|')
      if (!map.has(signature)) {
        map.set(signature, { assumptionIds: new Set(), worst: null, realistic: null, best: null })
      }
      const grp = map.get(signature)
      if (it.assumptionId != null) grp.assumptionIds.add(it.assumptionId)
      if (it.scenario === 'WORST') grp.worst = it
      else if (it.scenario === 'REALISTIC') grp.realistic = it
      else if (it.scenario === 'BEST') grp.best = it
    }
    const groups = Array.from(map.values()).map(g => ({
      assumptionId: g.assumptionIds.size === 0
        ? null
        : (g.assumptionIds.size === 1
            ? [...g.assumptionIds][0]
            : [...g.assumptionIds].sort((a,b)=>a-b).join(',')),
      worst: g.worst,
      realistic: g.realistic,
      best: g.best
    }))
    // Sort deterministically by first numeric assumption id if present
    groups.sort((a,b) => {
      const aFirst = a.assumptionId ? Number(String(a.assumptionId).split(',')[0]) : Infinity
      const bFirst = b.assumptionId ? Number(String(b.assumptionId).split(',')[0]) : Infinity
      if (!Number.isNaN(aFirst) && !Number.isNaN(bFirst)) return aFirst - bFirst
      return 0
    })
    return groups
  }, [allItems])

  async function handleDelete(item) {
    const token = localStorage.getItem('token')
    if (!token) return
    try {
      const resp = await fetch(`/api/cashflow-plans/${item.planId}/line-items/${item.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!resp.ok) throw new Error(await resp.text())
      fetchPlans()
    } catch (err) {
      setMessage('Error deleting item: ' + err.message)
    }
  }

  return (
    <Box sx={{ p: 2, display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid item xs={12} md={8}>
          <Paper elevation={3} sx={{ p: 2 }}>
            <Typography variant="h5">Scenario Group: {groupKey}</Typography>
            {Boolean(plans.length) && (
              <Box sx={{ mt: 1, mb: 1, display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
                <Typography variant="caption" sx={{ fontWeight: 600 }}>Plan Base Currencies:</Typography>
                {['WORST','REALISTIC','BEST'].map(scen => {
                  const plan = plans.find(p => p.scenario === scen)
                  return plan ? (
                    <CurrencyBadge key={scen} code={plan.baseCurrency} title={`${scen} scenario base currency`} />
                  ) : null
                })}
                {(() => {
                  const bases = plans.map(p => p.baseCurrency).filter(Boolean)
                  const distinct = Array.from(new Set(bases))
                  if (distinct.length <= 1) return null
                  return (
                    <span role="alert" aria-label={`Base currency mismatch across scenarios: ${distinct.join(', ')}`}
                      style={{ background:'#ed6c02', color:'#fff', padding:'4px 8px', borderRadius:4, fontSize:'0.65rem' }}
                      title={`Base currency mismatch across scenarios: ${distinct.join(', ')}`}>Mismatch: {distinct.join(' / ')}</span>
                  )
                })()}
              </Box>
            )}
            {message && (
              <Typography variant="body2" color="error" sx={{ mt: 1 }}>
                {message}
              </Typography>
            )}
            <Typography variant="body2" sx={{ mb: 1 }}>
              We have {plans.length} plans in this group.
            </Typography>
            <TableContainer>
              <Table stickyHeader>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ bgcolor: 'primary.main', color: 'common.white', fontWeight: 'bold' }}>Assumption ID</TableCell>
                    <TableCell sx={{ bgcolor: 'primary.main', color: 'common.white', fontWeight: 'bold' }}>Worst</TableCell>
                    <TableCell sx={{ bgcolor: 'primary.main', color: 'common.white', fontWeight: 'bold' }}>Realistic</TableCell>
                    <TableCell sx={{ bgcolor: 'primary.main', color: 'common.white', fontWeight: 'bold' }}>Best</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {groupedAssumptions.map((grp) => (
                    <TableRow key={grp.assumptionId ?? Math.random()}>
                      <TableCell>{grp.assumptionId ?? 'N/A'}</TableCell>
                      <TableCell>
                        {grp.worst
                          ? <ItemCell item={grp.worst} baseCurrency={planCurrencyMap.get(grp.worst.planId)} onDelete={() => handleDelete(grp.worst)} />
                          : <Typography color="text.secondary">— none —</Typography>}
                      </TableCell>
                      <TableCell>
                        {grp.realistic
                          ? <ItemCell item={grp.realistic} baseCurrency={planCurrencyMap.get(grp.realistic.planId)} onDelete={() => handleDelete(grp.realistic)} />
                          : <Typography color="text.secondary">— none —</Typography>}
                      </TableCell>
                      <TableCell>
                        {grp.best
                          ? <ItemCell item={grp.best} baseCurrency={planCurrencyMap.get(grp.best.planId)} onDelete={() => handleDelete(grp.best)} />
                          : <Typography color="text.secondary">— none —</Typography>}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper elevation={3} sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>Add Assumption</Typography>
            <Box sx={{ mb: 1 }}>
              <Button variant={selectedForm === 'ONE_TIME' ? 'contained' : 'outlined'} onClick={() => setSelectedForm('ONE_TIME')} sx={{ mr: 1 }}>One-Time</Button>
              <Button variant={selectedForm === 'RECURRING' ? 'contained' : 'outlined'} onClick={() => setSelectedForm('RECURRING')} sx={{ mr: 1 }}>Recurring</Button>
              <Button variant={selectedForm === 'CATEGORY' ? 'contained' : 'outlined'} onClick={() => setSelectedForm('CATEGORY')}>Category Adjust</Button>
            </Box>
            <Divider sx={{ mb: 2 }} />
            {selectedForm === 'ONE_TIME' && <OneTimeForm plans={plans} onSuccess={fetchPlans} />}
            {selectedForm === 'RECURRING' && <RecurringForm plans={plans} onSuccess={fetchPlans} />}
            {selectedForm === 'CATEGORY' && <CategoryForm plans={plans} onSuccess={fetchPlans} />}
          </Paper>
        </Grid>
      </Grid>

      <Paper elevation={3} sx={{ p: 2, mb: 2 }}>
        <Typography variant="h6" gutterBottom>Cashflow Forecast</Typography>
        <CashflowChart monthlyData={monthlyData} />
      </Paper>

      <Paper elevation={3} sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>Monthly Data (Realistic)</Typography>
        <MonthlyDataTable
          startBalance={plans.find((p) => p.scenario === 'REALISTIC')?.startBalance || 0}
          monthlyData={realKpiData}
        />
      </Paper>
    </Box>
  )
}


function ItemCell({ item, onDelete, baseCurrency }) {
  return (
    <Box sx={{ border: '1px solid #ccc', p: 1, mb: 1, borderRadius: 1 }}>
      <Typography variant="subtitle2">{item.title}</Typography>
      {item.amount != null && (
        <Typography variant="body2" sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          Amt: {formatAmount(item.amount, { currency: item.currency || baseCurrency })}
          {item.currency && item.currency !== baseCurrency && (
            <Typography component="span" variant="caption" sx={{ fontWeight: 600 }} title={`Item currency differs from plan base (${baseCurrency || 'N/A'})`}>
              {item.currency}
            </Typography>
          )}
          {item.currency && item.currency === baseCurrency && (
            <Typography component="span" variant="caption" color="text.secondary" title="Same as plan base currency">
              {item.currency}
            </Typography>
          )}
        </Typography>
      )}
      {item.percentChange != null && (
        <Typography variant="body2">{item.percentChange.toFixed(2)}%</Typography>
      )}
      <Button size="small" color="error" onClick={onDelete}>
        Delete
      </Button>
    </Box>
  )
}

ItemCell.propTypes = {
  item: PropTypes.shape({
    title: PropTypes.string,
    amount: PropTypes.number,
    currency: PropTypes.string,
    percentChange: PropTypes.number,
  }).isRequired,
  onDelete: PropTypes.func.isRequired,
  baseCurrency: PropTypes.string,
}
