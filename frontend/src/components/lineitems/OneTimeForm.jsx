import React, { useState, useEffect } from 'react'
import {
  Box,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  FormGroup,
  FormControlLabel,
  Checkbox,
  Paper,
  InputAdornment
} from '@mui/material'
import { amountFormatter } from '../../utils/numberFormatter'

export default function OneTimeForm({ plans, onSuccess }) {
  const scenarios = ['WORST', 'REALISTIC', 'BEST']
  const [title, setTitle] = useState('')
  const [transactionDate, setTransactionDate] = useState('')
  const [categories, setCategories] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [showNewCategoryForm, setShowNewCategoryForm] = useState(false)
  const [newCatName, setNewCatName] = useState('')
  const [newCatDirection, setNewCatDirection] = useState('POSITIVE')
  const [scenarioData, setScenarioData] = useState(
    scenarios.reduce((acc, s) => ({ ...acc, [s]: { active: false, amount: '' } }), {})
  )
  const [message, setMessage] = useState('')

  useEffect(() => {
    fetch('/api/categories', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
      .then(r => r.json())
      .then(setCategories)
      .catch(console.error)
  }, [])

  const toggleScenario = s =>
    setScenarioData(prev => ({
      ...prev,
      [s]: { ...prev[s], active: !prev[s].active }
    }))

  const handleAmountChange = (s, val) =>
    setScenarioData(prev => ({
      ...prev,
      [s]: { ...prev[s], amount: val }
    }))

  const handleSubmit = async () => {
    if (!title.trim() || !transactionDate) return
    let sharedId = null, count = 0
    for (let plan of plans) {
      const sc = plan.scenario
      if (!scenarioData[sc].active) continue
      const body = {
        title: title.trim(),
        type: 'ONE_TIME',
        amount: parseFloat(scenarioData[sc].amount) || 0,
        transactionDate,
        categoryId: selectedCategoryId || null
      }
      if (sharedId) body.assumptionId = sharedId
      const resp = await fetch(`/api/cashflow-plans/${plan.id}/line-items`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify(body)
      })
      if (resp.ok) {
        const item = await resp.json()
        if (!sharedId) sharedId = item.assumptionId
        count++
      }
    }
    setMessage(`Created for ${count} scenario(s).`)
    onSuccess && onSuccess()
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6">One-Time Assumption</Typography>
      {message && <Typography variant="body2" color="success.main">{message}</Typography>}
      <TextField
        label="Title"
        value={title}
        onChange={e => setTitle(e.target.value)}
        fullWidth
      />
      <TextField
        label="Date"
        type="date"
        InputLabelProps={{ shrink: true }}
        value={transactionDate}
        onChange={e => setTransactionDate(e.target.value)}
        fullWidth
      />
      <FormControl fullWidth>
        <InputLabel>Category</InputLabel>
        <Select
          value={selectedCategoryId}
          label="Category"
          onChange={e => setSelectedCategoryId(e.target.value)}
        >
          <MenuItem value="">None</MenuItem>
          {categories.map(cat => (
            <MenuItem key={cat.id} value={cat.id}>
              {cat.name} ({cat.direction})
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <Button onClick={() => setShowNewCategoryForm(prev => !prev)}>
        {showNewCategoryForm ? 'Cancel' : 'New Category'}
      </Button>
      {showNewCategoryForm && (
        <Paper variant="outlined" sx={{ p: 2 }}>
          <Typography variant="subtitle1">New Category</Typography>
          <TextField
            label="Name"
            value={newCatName}
            onChange={e => setNewCatName(e.target.value)}
            fullWidth
            sx={{ mb: 1 }}
          />
          <FormControl fullWidth>
            <InputLabel>Direction</InputLabel>
            <Select
              value={newCatDirection}
              label="Direction"
              onChange={e => setNewCatDirection(e.target.value)}
            >
              <MenuItem value="POSITIVE">POSITIVE</MenuItem>
              <MenuItem value="NEGATIVE">NEGATIVE</MenuItem>
            </Select>
          </FormControl>
          <Button variant="contained" sx={{ mt: 1 }} onClick={handleSubmit}>
            Save Category
          </Button>
        </Paper>
      )}
      <Typography variant="subtitle1">Scenarios</Typography>
      <FormGroup>
        {scenarios.map(s => (
          <Box key={s} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={scenarioData[s].active}
                  onChange={() => toggleScenario(s)}
                />
              }
              label={s}
            />
            <TextField
              label="Amount"
              type="number"
              size="small"
              disabled={!scenarioData[s].active}
              value={scenarioData[s].amount}
              onChange={e => handleAmountChange(s, e.target.value)}
              helperText={
                scenarioData[s].amount
                  ? amountFormatter.format(Number(scenarioData[s].amount))
                  : ''
              }
              InputProps={{
                startAdornment: <InputAdornment position="start">HUF</InputAdornment>
              }}
            />
          </Box>
        ))}
      </FormGroup>
      <Button variant="contained" onClick={handleSubmit}>
        Save
      </Button>
    </Box>
  )
}
