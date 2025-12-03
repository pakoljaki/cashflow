import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Box,
  Paper,
  Typography,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TextField,
  Button,
} from '@mui/material'
import '../styles/cashflowplans.css'
import { formatAmount } from '../utils/numberFormatter'
import CurrencyBadge from '../components/CurrencyBadge'
import CurrencySelect from '../components/CurrencySelect'
import { useCurrency } from '../context/AppContext'

export default function CashflowPlansPage() {
  const navigate = useNavigate()
  const [allPlans, setAllPlans] = useState([])
  const [groupedPlans, setGroupedPlans] = useState([])
  const [basePlanName, setBasePlanName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [startBalance, setStartBalance] = useState('')
  const [message, setMessage] = useState('')
  const { setBasePlanCurrency } = useCurrency()
  const [baseCurrency, setBaseCurrency] = useState('HUF')

  const fetchAllPlans = useCallback(() => {
    (async () => {
      const token = localStorage.getItem('token')
      if (!token) {
        setMessage('Not logged in')
        return
      }
      try {
        const resp = await fetch('/api/cashflow-plans', {
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
        })
        if (!resp.ok) throw new Error('Failed to fetch plans')
        const data = await resp.json()
        setAllPlans(data)
        groupByScenario(data)
      } catch (err) {
        setMessage(err.message)
      }
    })()
  }, [])

  useEffect(() => { fetchAllPlans() }, [fetchAllPlans])

  function groupByScenario(plansArray) {
    const map = new Map()
    for (let plan of plansArray) {
      if (!map.has(plan.groupKey)) map.set(plan.groupKey, [])
      map.get(plan.groupKey).push(plan)
    }
    const result = []
  for (let [, plansInGroup] of map.entries()) {
      let displayed = plansInGroup.find(p => p.scenario === 'REALISTIC') || plansInGroup[0]
      result.push(displayed)
    }
    setGroupedPlans(result)
  }

  async function handleCreateScenarios() {
    if (!basePlanName || !startDate || !endDate) {
      alert('Please fill in all fields')
      return
    }
    const token = localStorage.getItem('token')
    if (!token) {
      setMessage('Not logged in')
      return
    }
    const body = { basePlanName, startDate, endDate, startBalance: startBalance || '0', baseCurrency }
    try {
      const resp = await fetch('/api/cashflow-plans/scenarios', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      })
      if (!resp.ok) throw new Error(await resp.text())
      const newGroup = await resp.json()
      setMessage('Created 3 scenario plans successfully!')
      const updated = [...allPlans, ...newGroup]
      setAllPlans(updated)
      groupByScenario(updated)
      // Attempt to set context base plan currency from REALISTIC scenario if present
      const realistic = newGroup.find(p => p.scenario === 'REALISTIC')
      if (realistic?.baseCurrency) {
        setBasePlanCurrency(realistic.baseCurrency)
      } else {
        setBasePlanCurrency(baseCurrency)
      }
      setBasePlanName('')
      setStartDate('')
      setEndDate('')
      setStartBalance('')
      setBaseCurrency('HUF')
    } catch (err) {
      setMessage('Error: ' + err.message)
    }
  }

  async function handleDeleteGroup(groupKey) {
    const safeConfirm = (msg) => {
      // Use globalThis.confirm if available; fallback true in non-browser env (tests)
      /* eslint-disable no-undef */
      try { return typeof globalThis !== 'undefined' && globalThis.confirm ? globalThis.confirm(msg) : true } catch { return true }
      /* eslint-enable no-undef */
    }
    if (!safeConfirm('Are you sure you want to delete this plan group?')) {
      return
    }
    const token = localStorage.getItem('token')
    if (!token) {
      setMessage('Not logged in')
      return
    }
    try {
      const resp = await fetch(`/api/cashflow-plans/group/${groupKey}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (resp.status === 404) throw new Error('Plan group not found')
      if (!resp.ok) throw new Error(await resp.text() || 'Delete failed')
      const remaining = allPlans.filter(p => p.groupKey !== groupKey)
      setAllPlans(remaining)
      groupByScenario(remaining)
      setMessage('Deleted plan group successfully!')
    } catch (err) {
      setMessage('Error: ' + err.message)
    }
  }

  const headers = ['ID', 'Plan Name', 'Start Date', 'End Date', 'Start Balance', 'Base Currency', 'Actions']

  return (
    <Box className="cashflowplans-page">
      <Paper className="cashflowplans-table-container">
        <Typography variant="h6" align="center" gutterBottom>
          Cashflow Plans
        </Typography>
        {message && (
          <Typography color="error" align="center" sx={{ mb: 1 }}>
            {message}
          </Typography>
        )}
        <TableContainer>
          <Table stickyHeader size="small">
            <TableHead>
              <TableRow>
                {headers.map(h => (
                  <TableCell key={h} className="cashflow-header-cell">
                    {h}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {groupedPlans.map(plan => (
                <TableRow key={plan.id} hover>
                  <TableCell>{plan.id}</TableCell>
                  <TableCell>{plan.planName}</TableCell>
                  <TableCell>{plan.startDate}</TableCell>
                  <TableCell>{plan.endDate}</TableCell>
                  <TableCell>{formatAmount(plan.startBalance, { currency: plan.baseCurrency })}</TableCell>
                  <TableCell>
                    <CurrencyBadge code={plan.baseCurrency} title={`Base currency for this scenario plan`} />
                  </TableCell>
                  <TableCell>
                    <Box sx={{ display: 'flex', gap: 1 }}>
                      <Button
                        variant="contained"
                        size="small"
                        onClick={() => navigate(`/scenario-group/${plan.groupKey}`)}
                      >
                        Manage
                      </Button>
                      <Button
                        variant="contained"
                        size="small"
                        color="error"
                        onClick={() => handleDeleteGroup(plan.groupKey)}
                      >
                        Delete
                      </Button>
                    </Box>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Paper className="cashflowplans-form-container">
        <Typography variant="h6" gutterBottom>
          Create 3-Scenario Plan
        </Typography>
        <TextField
          label="Base Plan Name"
          value={basePlanName}
          onChange={e => setBasePlanName(e.target.value)}
          fullWidth
          sx={{ mb: 2 }}
        />
        <TextField
          label="Start Date"
          type="date"
          slotProps={{ inputLabel: { shrink: true } }}
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
          fullWidth
          sx={{ mb: 2 }}
        />
        <TextField
          label="End Date"
          type="date"
          slotProps={{ inputLabel: { shrink: true } }}
          value={endDate}
          onChange={e => setEndDate(e.target.value)}
          fullWidth
          sx={{ mb: 2 }}
        />
        <TextField
          label="Starting Balance"
          type="number"
          value={startBalance}
          onChange={e => setStartBalance(e.target.value)}
          fullWidth
          sx={{ mb: 3 }}
        />
        <CurrencySelect
          label="Base Currency"
          value={baseCurrency}
          onChange={setBaseCurrency}
          helperText="Currency in which the plan's line items are denominated"
          sx={{ mb: 3 }}
        />
        <Button variant="contained" color="primary" fullWidth onClick={handleCreateScenarios}>
          Create Plans
        </Button>
      </Paper>
    </Box>
  )
}
