import { useEffect, useState, useCallback } from 'react'
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
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import '../styles/transactions.css'
import { formatAmount } from '../utils/numberFormatter'
import { useCurrency } from '../context/AppContext'
import DualAmount from '../components/DualAmount'
import CurrencyBadge from '../components/CurrencyBadge'
import { CURRENCIES } from '../constants/currencies'

const STORAGE_KEY = 'transactionsDisplayCurrency'
const DEFAULT_CHOICE = 'ORIGINAL'

const readStoredChoice = () => {
  if (typeof window === 'undefined') return DEFAULT_CHOICE
  try {
    const stored = window.localStorage?.getItem(STORAGE_KEY)
    if (stored && (stored === DEFAULT_CHOICE || CURRENCIES.includes(stored))) {
      return stored
    }
  } catch (err) {
    console.warn('Unable to read transactions currency preference', err)
  }
  return DEFAULT_CHOICE
}

export default function TransactionsPage() {
  const { displayCurrency } = useCurrency()
  const [transactions, setTransactions] = useState([])
  const [selectedTransactionIds, setSelectedTransactionIds] = useState([])
  const [selectedDirection, setSelectedDirection] = useState(null)
  const [categories, setCategories] = useState([])
  const [newCategoryName, setNewCategoryName] = useState('')
  const [message, setMessage] = useState('')
  const [filterCategory, setFilterCategory] = useState(null)
  const [tableCurrency, setTableCurrency] = useState(readStoredChoice)

  const fetchTransactions = useCallback(async (targetCurrency) => {
    const token = localStorage.getItem('token')
    if (!token) return

    const choice = targetCurrency && targetCurrency !== DEFAULT_CHOICE ? targetCurrency : null
    const query = choice ? `?displayCurrency=${choice}` : ''

    const res = await fetch(`/api/transactions${query}`, {
      headers: { Authorization: `Bearer ${token}` },
    })

    if (!res.ok) {
      setMessage('Unable to load transactions')
      return
    }

    setTransactions(await res.json())
  }, [])

  const refreshTransactions = useCallback(async () => {
    await fetchTransactions(tableCurrency)
  }, [fetchTransactions, tableCurrency])

  useEffect(() => {
    fetchTransactions(tableCurrency)
  }, [fetchTransactions, tableCurrency])

  useEffect(() => {
    const token = localStorage.getItem('token')
    if (!token) return

    fetch('/api/categories', { headers: { Authorization: `Bearer ${token}` } })
      .then(res => (res.ok ? res.json() : []))
      .then(setCategories)
  }, [])

  const handleRowClick = tx => {
    const already = selectedTransactionIds.includes(tx.id)
    if (already) {
      const next = selectedTransactionIds.filter(id => id !== tx.id)
      setSelectedTransactionIds(next)
      if (!next.length) setSelectedDirection(null)
      return
    }

    if (selectedDirection && selectedDirection !== tx.transactionDirection) {
      alert('Cannot mix POSITIVE and NEGATIVE')
      return
    }

    setSelectedDirection(tx.transactionDirection)
    setSelectedTransactionIds([...selectedTransactionIds, tx.id])
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
      await refreshTransactions()
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

  const handleCurrencyChange = (_, value) => {
    if (!value) return
    setTableCurrency(value)
    try {
      window.localStorage?.setItem(STORAGE_KEY, value)
    } catch (err) {
      console.warn('Unable to persist transactions currency preference', err)
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

        <Box sx={{ mb: 2, display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', gap: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography variant="body2" color="textSecondary">
              Display currency
            </Typography>
            <ToggleButtonGroup
              color="primary"
              exclusive
              size="small"
              value={tableCurrency}
              onChange={handleCurrencyChange}
            >
              <ToggleButton value={DEFAULT_CHOICE}>Original</ToggleButton>
              {CURRENCIES.map(code => (
                <ToggleButton key={code} value={code} sx={{ px: 1.5 }}>
                  <CurrencyBadge code={code} />
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
          </Box>
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
                const requestedCurrency =
                  tx.displayCurrency || (tableCurrency === DEFAULT_CHOICE ? null : tableCurrency)
                const sameCurrency = !tx.convertedAmount || !requestedCurrency || tx.currency === requestedCurrency
                const nativeFormatted = formatAmount(tx.amount, { currency: tx.currency })
                let dual = { single: true, nativeFormatted }

                if (!sameCurrency) {
                  let tooltip = 'Converted amount'
                  if (tx.rateDate) {
                    tooltip = 'Rate date: ' + tx.rateDate
                    if (tx.rateSource) tooltip += ' (' + tx.rateSource + ')'
                  }
                  dual = {
                    single: false,
                    nativeFormatted,
                    convertedFormatted: formatAmount(tx.convertedAmount, {
                      currency: requestedCurrency || displayCurrency,
                    }),
                    tooltip,
                    displayCurrency: requestedCurrency || displayCurrency,
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
