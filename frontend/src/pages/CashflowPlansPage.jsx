// src/pages/CashflowPlansPage.jsx
import React, { useState, useEffect } from 'react'
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

export default function CashflowPlansPage() {
  const navigate = useNavigate()
  const [allPlans, setAllPlans] = useState([])
  const [groupedPlans, setGroupedPlans] = useState([])
  const [basePlanName, setBasePlanName] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [startBalance, setStartBalance] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    fetchAllPlans()
  }, [])

  async function fetchAllPlans() {
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
  }

  function groupByScenario(plansArray) {
    const map = new Map()
    for (let plan of plansArray) {
      if (!map.has(plan.groupKey)) map.set(plan.groupKey, [])
      map.get(plan.groupKey).push(plan)
    }
    const result = []
    for (let [groupKey, plansInGroup] of map.entries()) {
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
    const body = { basePlanName, startDate, endDate, startBalance: startBalance || '0' }
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
      setBasePlanName('')
      setStartDate('')
      setEndDate('')
      setStartBalance('')
    } catch (err) {
      setMessage('Error: ' + err.message)
    }
  }

  const headers = ['ID', 'Plan Name', 'Start Date', 'End Date', 'Start Balance', 'Actions']

  return (
    <Box className="cashflowplans-page">
      <Paper className="cashflowplans-table-container">
        <Typography variant="h6" align="center" gutterBottom>
          Cashflow Plans (Grouped by Scenario)
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
                  <TableCell>{plan.startBalance}</TableCell>
                  <TableCell>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={() => navigate(`/scenario-group/${plan.groupKey}`)}
                    >
                      Manage
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

      <Paper className="cashflowplans-form-container">
        <Typography variant="h6" gutterBottom>
          Create 3-Scenario Plan Group
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
          InputLabelProps={{ shrink: true }}
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
          fullWidth
          sx={{ mb: 2 }}
        />
        <TextField
          label="End Date"
          type="date"
          InputLabelProps={{ shrink: true }}
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
        <Button variant="contained" color="primary" fullWidth onClick={handleCreateScenarios}>
          Create Plans
        </Button>
      </Paper>
    </Box>
  )
}
