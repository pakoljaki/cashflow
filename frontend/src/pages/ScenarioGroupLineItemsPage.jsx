// src/pages/ScenarioGroupLineItemsPage.jsx
import React, { useState, useEffect } from 'react'
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
  Divider,
} from '@mui/material'
import OneTimeForm from '../components/lineitems/OneTimeForm'
import RecurringForm from '../components/lineitems/RecurringForm'
import CategoryForm from '../components/lineitems/CategoryForm'
import CashflowChart from '../components/charts/CashflowChart'

export default function ScenarioGroupLineItemsPage() {
  const { groupKey } = useParams()
  const [plans, setPlans] = useState([])
  const [message, setMessage] = useState('')
  const [selectedForm, setSelectedForm] = useState(null)
  const [monthlyData, setMonthlyData] = useState([])

  useEffect(() => {
    fetchPlans()
  }, [groupKey])

  async function fetchPlans() {
    const token = localStorage.getItem('token')
    if (!token) {
      setMessage('Not logged in.')
      return
    }
    try {
      const resp = await fetch(`/api/cashflow-plans/group/${groupKey}/plans`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!resp.ok) throw new Error(await resp.text())
      setPlans(await resp.json())
    } catch (err) {
      setMessage('Error: ' + err.message)
    }
  }

  useEffect(() => {
    if (!plans.length) return
    const realistic = plans.find(p => p.scenario === 'REALISTIC')
    if (!realistic) return
    const token = localStorage.getItem('token')
    fetch(`/api/cashflow-plans/${realistic.id}/monthly-kpi`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(r => r.json())
      .then(setMonthlyData)
      .catch(console.error)
  }, [plans])

  const allItems = plans.flatMap(plan =>
    plan.lineItems.map(item => ({ planId: plan.id, scenario: plan.scenario, ...item }))
  )

  const groupedAssumptions = React.useMemo(() => {
    const map = new Map()
    let fallback = 100000
    allItems.forEach(it => {
      const key = it.assumptionId ?? `missing-${fallback++}`
      if (!map.has(key)) {
        map.set(key, { assumptionId: it.assumptionId, worst: null, realistic: null, best: null })
      }
      const grp = map.get(key)
      if (it.scenario === 'WORST') grp.worst = it
      if (it.scenario === 'REALISTIC') grp.realistic = it
      if (it.scenario === 'BEST') grp.best = it
    })
    return Array.from(map.values())
  }, [allItems])

  async function handleDelete(item) {
    const token = localStorage.getItem('token')
    if (!token) return
    try {
      const resp = await fetch(`/api/cashflow-plans/${item.planId}/line-items/${item.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
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
                    {['Assumption ID', 'Worst', 'Realistic', 'Best'].map(col => (
                      <TableCell
                        key={col}
                        sx={{ bgcolor: 'primary.main', color: 'common.white', fontWeight: 'bold' }}
                      >
                        {col}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {groupedAssumptions.map(grp => (
                    <TableRow key={grp.assumptionId ?? Math.random()}>
                      <TableCell>{grp.assumptionId ?? 'N/A'}</TableCell>
                      <TableCell>
                        {grp.worst ? (
                          <ItemCell item={grp.worst} onDelete={() => handleDelete(grp.worst)} />
                        ) : (
                          <Typography color="text.secondary">— none —</Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {grp.realistic ? (
                          <ItemCell
                            item={grp.realistic}
                            onDelete={() => handleDelete(grp.realistic)}
                          />
                        ) : (
                          <Typography color="text.secondary">— none —</Typography>
                        )}
                      </TableCell>
                      <TableCell>
                        {grp.best ? (
                          <ItemCell item={grp.best} onDelete={() => handleDelete(grp.best)} />
                        ) : (
                          <Typography color="text.secondary">— none —</Typography>
                        )}
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
            <Typography variant="h6" gutterBottom>
              Add Assumption
            </Typography>
            <Box sx={{ mb: 1 }}>
              <Button
                variant={selectedForm === 'ONE_TIME' ? 'contained' : 'outlined'}
                onClick={() => setSelectedForm('ONE_TIME')}
                sx={{ mr: 1 }}
              >
                One-Time
              </Button>
              <Button
                variant={selectedForm === 'RECURRING' ? 'contained' : 'outlined'}
                onClick={() => setSelectedForm('RECURRING')}
                sx={{ mr: 1 }}
              >
                Recurring
              </Button>
              <Button
                variant={selectedForm === 'CATEGORY' ? 'contained' : 'outlined'}
                onClick={() => setSelectedForm('CATEGORY')}
              >
                Category Adjust
              </Button>
            </Box>
            <Divider sx={{ mb: 2 }} />
            {selectedForm === 'ONE_TIME' && (
              <OneTimeForm plans={plans} onSuccess={() => fetchPlans()} />
            )}
            {selectedForm === 'RECURRING' && (
              <RecurringForm plans={plans} onSuccess={() => fetchPlans()} />
            )}
            {selectedForm === 'CATEGORY' && (
              <CategoryForm plans={plans} onSuccess={() => fetchPlans()} />
            )}
          </Paper>
        </Grid>
      </Grid>

      <Paper elevation={3} sx={{ p: 2, flex: 1, overflow: 'auto' }}>
        <Typography variant="h6" gutterBottom>
          Cashflow Forecast
        </Typography>
        <CashflowChart monthlyData={monthlyData} />
      </Paper>
    </Box>
  )
}

function ItemCell({ item, onDelete }) {
  return (
    <Paper
      variant="outlined"
      sx={{ p: 1, mb: 1, display: 'flex', flexDirection: 'column', gap: 0.5 }}
    >
      <Typography variant="subtitle2">{item.title}</Typography>
      {item.amount != null && (
        <Typography variant="body2">Amt: {item.amount}</Typography>
      )}
      {item.percentChange != null && (
        <Typography variant="body2">
          ± {(item.percentChange * 100).toFixed(2)}%
        </Typography>
      )}
      <Button size="small" color="error" onClick={onDelete}>
        Delete
      </Button>
    </Paper>
  )
}
