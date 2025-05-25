import { useState, useEffect } from 'react'
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
  InputAdornment
} from '@mui/material'

export default function CategoryForm({ plans, onSuccess }) {
  const scenarios = ['WORST', 'REALISTIC', 'BEST']
  const [title, setTitle] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [categories, setCategories] = useState([])
  const [selectedCategoryId, setSelectedCategoryId] = useState('')
  const [scenarioData, setScenarioData] = useState(
    scenarios.reduce((acc, s) => ({ ...acc, [s]: { active: false, percent: '100' } }), {})
  )
  const [message, setMessage] = useState('')

  useEffect(() => {
    fetch('/api/categories', {
      headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
    })
      .then((r) => r.json())
      .then(setCategories)
      .catch(console.error)
  }, [])

  const toggleScenario = (s) =>
    setScenarioData((prev) => ({
      ...prev,
      [s]: { ...prev[s], active: !prev[s].active }
    }))

  const handlePercentChange = (s, val) =>
    setScenarioData((prev) => ({
      ...prev,
      [s]: { ...prev[s], percent: val }
    }))

  const handleSubmit = async () => {
    if (!title.trim()) return
    let sharedId = null
    let count = 0
    for (let plan of plans) {
      const sc = plan.scenario
      if (!scenarioData[sc].active) continue
      const body = {
        title: title.trim(),
        type: 'CATEGORY_ADJUSTMENT',
        percentChange: parseFloat(scenarioData[sc].percent) / 100,
        categoryId: selectedCategoryId || null,
        startDate: startDate || null,
        endDate: endDate || null
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
    setMessage(`Added to ${count} scenario(s).`)
    onSuccess && onSuccess()
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
        onChange={(e) => setTitle(e.target.value)}
        fullWidth
      />
      <Box sx={{ display: 'flex', gap: 1 }}>
        <TextField
          label="Start Date"
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
        <TextField
          label="End Date"
          type="date"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          InputLabelProps={{ shrink: true }}
          fullWidth
        />
      </Box>
      <FormControl fullWidth>
        <InputLabel>Category</InputLabel>
        <Select
          value={selectedCategoryId}
          label="Category"
          onChange={(e) => setSelectedCategoryId(e.target.value)}
        >
          <MenuItem value="">None</MenuItem>
          {categories.map((cat) => (
            <MenuItem key={cat.id} value={cat.id}>
              {cat.name} ({cat.direction})
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      <Typography variant="subtitle1">Scenarios (100% = original amount)</Typography>
      <FormGroup>
        {scenarios.map((s) => (
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
              label="% of original"
              type="number"
              size="small"
              value={scenarioData[s].percent}
              onChange={(e) => handlePercentChange(s, e.target.value)}
              disabled={!scenarioData[s].active}
              InputProps={{ endAdornment: <InputAdornment position="end">%</InputAdornment> }}
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
