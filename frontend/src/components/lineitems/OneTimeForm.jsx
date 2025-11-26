import { useState, useEffect } from 'react'
import PropTypes from 'prop-types'
import CurrencySelect from '../CurrencySelect'
import { useCurrency } from '../../context/CurrencyContext'
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
import { formatAmount } from '../../utils/numberFormatter'

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
  const { basePlanCurrency } = useCurrency()
  const [currency, setCurrency] = useState(basePlanCurrency || 'HUF')

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

  const handleCreateCategory = async () => {
    if (!newCatName.trim()) return
    const token = localStorage.getItem('token')
    try {
      const resp = await fetch('/api/categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({ name: newCatName.trim(), direction: newCatDirection })
      })
      if (!resp.ok) throw new Error(await resp.text())
      const newCat = await resp.json()
      setCategories([...categories, newCat])
      setSelectedCategoryId(newCat.id)
      setNewCatName('')
      setNewCatDirection('POSITIVE')
      setShowNewCategoryForm(false)
    } catch (err) {
      setMessage('Error creating category: ' + err.message)
    }
  }

  const handleSubmit = async () => {
    if (!title.trim() || !transactionDate) return
    let sharedId = null, count = 0, warnings = []
    for (let plan of plans) {
      const sc = plan.scenario
      if (!scenarioData[sc].active) continue
      const body = {
        title: title.trim(),
        type: 'ONE_TIME',
        amount: Number.parseFloat(scenarioData[sc].amount) || 0,
        transactionDate,
        categoryId: selectedCategoryId || null,
        currency
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
        if (!sharedId) sharedId = item?.assumptionId
        if (item?.warning && !warnings.includes(item.warning)) {
          warnings.push(item.warning)
        }
        count++
      }
    }
    let msg = `Created for ${count} scenario(s).`
    if (warnings.length > 0) {
      msg += ' ⚠️ ' + warnings.join(' ')
    }
    setMessage(msg)
  onSuccess?.()
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
      <Typography variant="h6">One-Time Assumption</Typography>
      {message && (
        <Typography 
          variant="body2" 
          color={message.includes('⚠️') ? 'warning.main' : 'success.main'}
          sx={{ whiteSpace: 'pre-wrap' }}
        >
          {message}
        </Typography>
      )}
      <TextField
        label="Title"
        value={title}
        onChange={e => setTitle(e.target.value)}
        fullWidth
      />
      <TextField
        label="Date"
        type="date"
        slotProps={{ inputLabel: { shrink: true } }}
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
          <Button variant="contained" sx={{ mt: 1 }} onClick={handleCreateCategory}>
            Save Category
          </Button>
        </Paper>
      )}
      <CurrencySelect
        label="Currency"
        value={currency}
        onChange={setCurrency}
        helperText="Currency for these one-time amounts"
        sx={{ maxWidth: 220 }}
      />
      <Typography variant="subtitle1" sx={{ mt: 2 }}>Scenarios</Typography>
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
                  ? formatAmount(Number(scenarioData[s].amount), { currency })
                  : ''
              }
              slotProps={{ input: { startAdornment: <InputAdornment position="start">{currency}</InputAdornment> } }}
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

OneTimeForm.propTypes = {
  plans: PropTypes.arrayOf(PropTypes.shape({ id: PropTypes.number, scenario: PropTypes.string })).isRequired,
  onSuccess: PropTypes.func,
}
