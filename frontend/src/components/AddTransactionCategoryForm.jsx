import { useState } from 'react'
import {
  Box,
  Typography,
  TextField,
  Button,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material'

export default function AddTransactionCategoryForm({ onNewCategory }) {
  const [name, setName] = useState('')
  const [direction, setDirection] = useState('POSITIVE')
  const [message, setMessage] = useState('')

  const handleSubmit = async e => {
    e.preventDefault()
    const token = localStorage.getItem('token')
    const payload = { name, direction }
    try {
      const resp = await fetch('/api/categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      })
      if (!resp.ok) throw new Error(await resp.text())
      const newCategory = await resp.json()
      onNewCategory(newCategory)
      setName('')
      setDirection('POSITIVE')
      setMessage('New transaction category added.')
    } catch (err) {
      setMessage('Error: ' + err.message)
    }
  }

  return (
    <Paper variant="outlined" sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Add Transaction Category
      </Typography>
      {message && (
        <Typography variant="body2" color="success.main" sx={{ mb: 1 }}>
          {message}
        </Typography>
      )}
      <Box
        component="form"
        onSubmit={handleSubmit}
        sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
      >
        <TextField
          label="Name"
          value={name}
          onChange={e => setName(e.target.value)}
          required
          fullWidth
        />
        <FormControl fullWidth>
          <InputLabel>Direction</InputLabel>
          <Select
            value={direction}
            label="Direction"
            onChange={e => setDirection(e.target.value)}
          >
            <MenuItem value="POSITIVE">POSITIVE</MenuItem>
            <MenuItem value="NEGATIVE">NEGATIVE</MenuItem>
          </Select>
        </FormControl>
        <Button type="submit" variant="contained" color="primary">
          Add Category
        </Button>
      </Box>
    </Paper>
  )
}
