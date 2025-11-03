import { useEffect, useState } from 'react'
import {
  Box,
  Paper,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Button,
  Typography,
  TextField,
} from '@mui/material'
import Autocomplete from '@mui/material/Autocomplete'
import '../styles/transactions.css'
import { formatAmount } from '../utils/numberFormatter'
import { useCurrency } from '../context/CurrencyContext'
import DualAmount from '../components/DualAmount'

export default function TransactionsPage() {
  const { displayCurrency } = useCurrency()
  const [transactions, setTransactions] = useState([])
  const [selectedTransactionIds, setSelectedTransactionIds] = useState([])
  const [selectedDirection, setSelectedDirection] = useState(null)
  const [categories, setCategories] = useState([])
  const [newCategoryName, setNewCategoryName] = useState('')
  const [message, setMessage] = useState('')
  const [filterCategory, setFilterCategory] = useState(null)

  useEffect(() => {
    const token = localStorage.getItem('token')
    fetch('/api/transactions', { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(setTransactions)
  }, [])

  useEffect(() => {
    const token = localStorage.getItem('token')
    fetch('/api/categories', { headers: { Authorization: `Bearer ${token}` } })
      .then(res => res.json())
      .then(setCategories)
  }, [])

  const handleRowClick = tx => {
    const already = selectedTransactionIds.includes(tx.id)
    if (already) {
      const next = selectedTransactionIds.filter(id => id !== tx.id)
      setSelectedTransactionIds(next)
      if (!next.length) setSelectedDirection(null)
    } else if (!selectedDirection || selectedDirection === tx.transactionDirection) {
      setSelectedDirection(tx.transactionDirection)
      setSelectedTransactionIds([...selectedTransactionIds, tx.id])
    } else {
      alert('Cannot mix POSITIVE and NEGATIVE')
    }
  }

  const positiveCategories = categories.filter(c => c.direction === 'POSITIVE')
  const negativeCategories = categories.filter(c => c.direction === 'NEGATIVE')

  const handleCategoryClick = async catId => {
    if (!selectedTransactionIds.length) {
      alert('No transactions selected.')
      return
    }
    const token = localStorage.getItem('token')
    const res = await fetch('/api/transactions/bulk-category', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ transactionIds: selectedTransactionIds, categoryId: catId }),
    })
    if (res.ok) {
      setMessage('Category updated!')
      refreshTransactions()
      setSelectedTransactionIds([])
      setSelectedDirection(null)
      return
    }
    const txt = await res.text()
    setMessage('Error: ' + txt)
  }

  const handleCreateCategory = async () => {
    if (!selectedDirection) {
      alert('Select transactions first.')
      return
    }
    if (!newCategoryName.trim()) {
      alert('Enter a category name.')
      return
    }
    const token = localStorage.getItem('token')
    const res = await fetch('/api/categories', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ name: newCategoryName.trim(), direction: selectedDirection }),
    })
    if (res.ok) {
      const created = await res.json()
      setCategories([...categories, created])
      setNewCategoryName('')
      setMessage('New category created!')
      return
    }
    const txt = await res.text()
    setMessage('Error: ' + txt)
  }

  const refreshTransactions = async () => {
    const token = localStorage.getItem('token')
    const res = await fetch('/api/transactions', { headers: { Authorization: `Bearer ${token}` } })
    if (res.ok) {
      setTransactions(await res.json())
    }
  }

  const headers = ['ID', 'Date', 'Amount', 'Currency', 'Partner', 'Account', 'Memo', 'Category']

  const filteredTransactions = filterCategory
    ? transactions.filter(tx => tx.category?.name === filterCategory)
    : transactions

  const categoryOptions = categories.map(c => c.name)

  return (
    <Box className="transactions-page">
      <Paper className="transactions-container">
        <Typography variant="h6" align="center" gutterBottom>
          All Transactions
        </Typography>

        <Box sx={{ mb: 2, display: 'flex', justifyContent: 'flex-end' }}>
          <Autocomplete
            size="small"
            options={categoryOptions}
            value={filterCategory}
            onChange={(_, v) => setFilterCategory(v)}
            clearOnEscape
            sx={{ width: 300 }}
            renderInput={params => (
              <TextField {...params} label="Filter by Category" placeholder="Select category" />
            )}
          />
        </Box>

        <TableContainer>
          <Table className="transaction-table" stickyHeader size="small">
            <TableHead>
              <TableRow>
                {headers.map(h => (
                  <TableCell key={h} className="transaction-header-cell">
                    {h}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredTransactions.map(tx => {
                const isSelected = selectedTransactionIds.includes(tx.id)
                const isPos = tx.transactionDirection === 'POSITIVE'
                const sign = isPos ? '+' : '–'
                // Inline dual amount logic (avoid hook inside map until backend enriches data)
                const same = tx.currency === displayCurrency || tx.convertedAmount == null
                const nativeFormatted = formatAmount(tx.amount, { currency: tx.currency })
                let dual = { single: true, nativeFormatted }
                if (!same) {
                  let tooltip = 'Converted amount'
                  if (tx.rateDate) {
                    tooltip = 'Rate date: ' + tx.rateDate
                    if (tx.rateSource) tooltip += ' (' + tx.rateSource + ')'
                  }
                  dual = {
                    single: false,
                    nativeFormatted,
                    convertedFormatted: formatAmount(tx.convertedAmount, { currency: displayCurrency }),
                    tooltip,
                    displayCurrency,
                    currency: tx.currency,
                  }
                }
                return (
                  <TableRow
                    key={tx.id}
                    onClick={() => handleRowClick(tx)}
                    className={isSelected ? 'selected-row' : ''}
                  >
                    <TableCell>{tx.id}</TableCell>
                    <TableCell>{tx.bookingDate}</TableCell>
                    <TableCell
                      sx={{
                        color: isPos ? 'success.light' : 'error.light',
                        fontWeight: 'bold',
                      }}
                    >
                      <DualAmount dual={dual} sign={sign} />
                    </TableCell>
                    <TableCell>{tx.currency}</TableCell>
                    <TableCell>{tx.partnerName || 'N/A'}</TableCell>
                    <TableCell>{tx.partnerAccountNumber || 'N/A'}</TableCell>
                    <TableCell>{tx.memo || 'N/A'}</TableCell>
                    <TableCell>{tx.category?.name || 'Uncategorized'}</TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </TableContainer>

        {message && <div className="info-message">{message}</div>}
      </Paper>

      <Paper className="category-panel">
        <Typography variant="h6" gutterBottom>
          Categories
        </Typography>
        {!selectedDirection && (
          <Typography variant="body2" color="textSecondary">
            Select transactions to define a direction.
          </Typography>
        )}
        {selectedDirection === 'POSITIVE' &&
          positiveCategories.map(cat => (
            <Button
              key={cat.id}
              variant="outlined"
              className="category-button"
              onClick={() => handleCategoryClick(cat.id)}
            >
              {cat.name}
            </Button>
          ))}
        {selectedDirection === 'NEGATIVE' &&
          negativeCategories.map(cat => (
            <Button
              key={cat.id}
              variant="outlined"
              className="category-button"
              onClick={() => handleCategoryClick(cat.id)}
            >
              {cat.name}
            </Button>
          ))}
        <div className="new-category">
          <Typography variant="subtitle1">Add New Category</Typography>
          <TextField
            size="small"
            placeholder="Category name..."
            fullWidth
            value={newCategoryName}
            onChange={e => setNewCategoryName(e.target.value)}
          />
          <Button
            variant="contained"
            color="primary"
            fullWidth
            sx={{ mt: 1 }}
            onClick={handleCreateCategory}
          >
            Create Category
          </Button>
        </div>
      </Paper>
    </Box>
  )
}
