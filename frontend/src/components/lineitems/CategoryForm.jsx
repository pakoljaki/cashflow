// src/components/lineitems/CategoryForm.jsx
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
  Divider,
} from '@mui/material'

export default function CategoryForm({ plans, onSuccess }) {
  const scenarios = ['WORST', 'REALISTIC', 'BEST']
  const [title, setTitle] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [categories, setCategories] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [showNewCategoryForm, setShowNewCategoryForm] = useState(false)
  const [newCatName, setNewCatName] = useState('')
  const [newCatDirection, setNewCatDirection] = useState('NEGATIVE')
  const [scenarioData, setScenarioData] = useState(
    scenarios.reduce((acc, s) => ({ ...acc, [s]: { active: false, percent: '' } }), {})
  )
  const [message, setMessage] = useState('')

  useEffect(() => {
    fetch('/api/categories', { headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } })
      .then(r => r.json())
      .then(setCategories)
      .catch(console.error)
  }, [])

  const toggleScenario = s =>
    setScenarioData(prev => ({
      ...prev,
      [s]: { ...prev[s], active: !prev[s].active },
    }))

  const handlePercentChange = (s, val) =>
    setScenarioData(prev => ({
      ...prev,
      [s]: { ...prev[s], percent: val },
    }))

  const handleCreateCategory = async () => {
    if (!newCatName.trim()) return
    const resp = await fetch('/api/categories', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ name: newCatName.trim(), direction: newCatDirection }),
    })
    if (resp.ok) {
      const cat = await resp.json()
      setCategories(prev => [...prev, cat])
      setSelectedCategoryId(cat.id)
      setShowNewCategoryForm(false)
      setNewCatName('')
      setNewCatDirection('NEGATIVE')
    }
  }

  const handleSubmit = async () => {
    if (!title.trim()) return
    let sharedId = null,
      count = 0
    for (let plan of plans) {
      const sc = plan.scenario
      if (!scenarioData[sc].active) continue
      const body = {
        title: title.trim(),
        type: 'CATEGORY_ADJUSTMENT',
        percentChange: parseFloat(scenarioData[sc].percent) || 0,
        categoryId: selectedCategoryId || null,
        startDate: startDate || null,
        endDate: endDate || null,
      }
      if (sharedId) body.assumptionId = sharedId
      const resp = await fetch(`/api/cashflow-plans/${plan.id}/line-items`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(body),
      })
      if (resp.ok) {
        const item = await resp.json()
        if (!sharedId) sharedId = item.assumptionId
        count++
      }
    }
    setMessage(`Added to ${count} scenario(s).`)
    if (onSuccess) onSuccess()
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6">Category Adjustment</Typography>
      {message && (
        <Typography variant="body2" color="success.main">
          {message}
        </Typography>
      )}
      <TextField
        label="Title"
        value={title}
        onChange={e => setTitle(e.target.value)}
        fullWidth
      />
      <Box sx={{ display: 'flex', gap: 1 }}>
        <TextField
          label="Start Date"
          type="date"
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
        <TextField
          label="End Date"
          type="date"
          value={endDate}
          onChange={e => setEndDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
      </Box>
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
          <Button variant="contained" sx={{ mt: 1 }} onClick={handleCreateCategory}>
            Save Category
          </Button>
        </Paper>
      )}
      <Divider />
      <Typography variant="subtitle1">Scenarios</Typography>
      <FormGroup>
        {scenarios.map(s => (
          <Box key={s} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
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
              label="% Change"
              type="number"
              size="small"
              disabled={!scenarioData[s].active}
              value={scenarioData[s].percent}
              onChange={e => handlePercentChange(s, e.target.value)}
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
