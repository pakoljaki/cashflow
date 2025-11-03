import { useEffect, useState } from 'react'
import { Box, Typography, Chip, Paper } from '@mui/material'
import { useCurrency } from '../context/CurrencyContext'
import { classifyStaleness, stalenessColor } from '../utils/rateStaleness'

export default function FxHealthPanel() {
  const { warnAfterDays, staleAfterDays, roles, fxFlags } = useCurrency()
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      setLoading(true)
      setError('')
      try {
        const token = localStorage.getItem('token')
        const res = await fetch('/api/fx/health', { headers: { Authorization: `Bearer ${token}` } })
        if (!res.ok) throw new Error('Failed to load FX health: ' + res.status)
        const json = await res.json()
        if (active) setData(Array.isArray(json) ? json : [])
      } catch (e) {
        if (active) setError(e.message)
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [])

  if (!roles.includes('ROLE_ADMIN') || !fxFlags?.healthPanel) return null

  return (
    <Paper elevation={2} sx={{ p: 2, mt: 2 }}>
      <Typography variant="h6" gutterBottom>FX Rate Health</Typography>
      {loading && <Typography variant="body2">Loading FX health...</Typography>}
      {error && <Typography variant="body2" color="error">{error}</Typography>}
      {!loading && !error && data.length === 0 && (
        <Typography variant="body2">No FX health data available.</Typography>
      )}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
        {data.map(r => {
          const status = classifyStaleness(r.rateDate, { warnAfterDays, staleAfterDays })
          const color = stalenessColor(status.level)
          return (
            <Chip
              key={r.currency}
              label={`${r.currency}: ${status.level}`}
              title={`Currency ${r.currency}\nDate: ${r.rateDate || 'n/a'}\nProvider: ${r.provider || 'n/a'}\nAge: ${status.days === Infinity ? 'unknown' : status.days + ' day(s)'}`}
              sx={{ bgcolor: color, color: '#fff' }}
              size="small"
            />
          )
        })}
      </Box>
    </Paper>
  )
}
