import { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  Button,
  Switch,
  FormControlLabel,
  Chip,
  CircularProgress,
  Alert,
} from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ToggleOffIcon from '@mui/icons-material/ToggleOff'
import ToggleOnIcon from '@mui/icons-material/ToggleOn'

export default function FxSettingsPanel() {
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [toggling, setToggling] = useState(false)
  const [dynamicFetchEnabled, setDynamicFetchEnabled] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [lastRefresh, setLastRefresh] = useState(null)

  useEffect(() => {
    fetchMode()
  }, [])

  const fetchMode = async () => {
    try {
      const token = localStorage.getItem('token')
      const res = await fetch('/api/fx/mode', {
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!res.ok) throw new Error('Failed to fetch FX mode')
      const data = await res.json()
      setDynamicFetchEnabled(data.dynamicFetchEnabled)
      setLastRefresh(data.lastRefresh)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleToggleMode = async () => {
    setToggling(true)
    setError(null)
    setSuccess(null)
    try {
      const token = localStorage.getItem('token')
      const res = await fetch('/api/fx/mode/toggle', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!res.ok) throw new Error('Failed to toggle FX mode')
      const data = await res.json()
      setDynamicFetchEnabled(data.dynamicFetchEnabled)
      setSuccess(`Mode switched to: ${data.dynamicFetchEnabled ? 'Dynamic Fetch' : 'Cache-only'}`)
    } catch (err) {
      setError(err.message)
    } finally {
      setToggling(false)
    }
  }

  const handleRefresh = async () => {
    setRefreshing(true)
    setError(null)
    setSuccess(null)
    try {
      const token = localStorage.getItem('token')
      const res = await fetch('/api/fx/refresh', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` }
      })
      if (!res.ok) throw new Error('Failed to refresh exchange rates')
      const data = await res.json()
      setSuccess(`Refreshed ${data.refreshedCount} exchange rates for past ${data.days} days`)
      setLastRefresh(new Date().toISOString())
    } catch (err) {
      setError(err.message)
    } finally {
      setRefreshing(false)
    }
  }

  if (loading) {
    return (
      <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', py: 4 }}>
          <CircularProgress size={40} />
        </Box>
      </Paper>
    )
  }

  return (
    <Paper elevation={2} sx={{ p: 3, borderRadius: 2 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
        {/* Left section - Title and mode indicator */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box sx={{ 
            bgcolor: 'success.main', 
            color: 'white', 
            p: 1.5, 
            borderRadius: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            💱
          </Box>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              Exchange Rate Management
            </Typography>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 0.5 }}>
              <Typography variant="body2" color="text.secondary">
                Current Mode:
              </Typography>
              <Chip 
                label={dynamicFetchEnabled ? 'Dynamic Fetch' : 'Cache-only'}
                color={dynamicFetchEnabled ? 'warning' : 'success'}
                size="small"
                sx={{ fontWeight: 600 }}
              />
            </Box>
          </Box>
        </Box>

        {/* Middle section - Mode description */}
        <Box sx={{ flex: 1, minWidth: 300, maxWidth: 500 }}>
          <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
            {dynamicFetchEnabled 
              ? '🔄 Dynamic mode: Missing rates are fetched from Frankfurter API on-demand'
              : '💾 Cache mode: All rates served from local database (past 1000 days pre-loaded)'}
          </Typography>
        </Box>

        {/* Right section - Action buttons */}
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
          <Button
            variant="outlined"
            color="primary"
            startIcon={dynamicFetchEnabled ? <ToggleOffIcon /> : <ToggleOnIcon />}
            onClick={handleToggleMode}
            disabled={toggling}
            sx={{ minWidth: 160 }}
          >
            {toggling ? 'Toggling...' : 'Toggle Mode'}
          </Button>
          <Button
            variant="contained"
            color="success"
            startIcon={refreshing ? <CircularProgress size={16} color="inherit" /> : <RefreshIcon />}
            onClick={handleRefresh}
            disabled={refreshing}
            sx={{ minWidth: 180 }}
          >
            {refreshing ? 'Refreshing...' : 'Refresh 1000 Days'}
          </Button>
        </Box>
      </Box>

      {/* Status messages */}
      {error && (
        <Alert severity="error" sx={{ mt: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}
      {success && (
        <Alert severity="success" sx={{ mt: 2 }} onClose={() => setSuccess(null)}>
          {success}
        </Alert>
      )}
      {lastRefresh && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1, textAlign: 'right' }}>
          Last refresh: {new Date(lastRefresh).toLocaleString()}
        </Typography>
      )}
    </Paper>
  )
}
