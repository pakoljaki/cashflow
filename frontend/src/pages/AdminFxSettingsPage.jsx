import { useEffect, useState } from 'react'
import { Box, Paper, Typography, TextField, Switch, FormControlLabel, Button, Autocomplete } from '@mui/material'
import CurrencySelect from '../components/CurrencySelect'
import { useCurrency } from '../context/CurrencyContext'

// Admin FX Settings page: allows viewing/updating FX configuration via /api/settings/fx
// Requires ROLE_ADMIN (backend protected). Assumes token with roles stored in localStorage.
export default function AdminFxSettingsPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [settings, setSettings] = useState(null)
  const [quotes, setQuotes] = useState([])
  const [saving, setSaving] = useState(false)
  const { displayCurrency } = useCurrency() // possibly used for preview

  useEffect(() => {
    const token = localStorage.getItem('token')
    async function fetchSettings() {
      try {
        const res = await fetch('/api/settings/fx', { headers: { Authorization: `Bearer ${token}` } })
        if (!res.ok) throw new Error('Failed to load FX settings: ' + res.status)
        const data = await res.json()
        setSettings(data)
        setQuotes(data.quotes || [])
      } catch(e) {
        setError(e.message)
      } finally {
        setLoading(false)
      }
    }
    fetchSettings()
  }, [])

  const handleQuoteChange = (_, vals) => setQuotes(vals)
  const handleField = (field) => (e) => setSettings({ ...settings, [field]: e.target.value })
  const handleToggleEnabled = (e) => setSettings({ ...settings, enabled: e.target.checked })

  const save = async () => {
    if (!settings) return
    setSaving(true)
    setError(null)
    try {
      const token = localStorage.getItem('token')
      const payload = { ...settings, quotes }
      const res = await fetch('/api/settings/fx', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(payload),
      })
      if (!res.ok) throw new Error('Update failed: ' + res.status)
      const updated = await res.json()
      setSettings(updated)
      setQuotes(updated.quotes || [])
    } catch(e) {
      setError(e.message)
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <Typography>Loading FX settings...</Typography>
  if (error) return <Typography color="error">{error}</Typography>
  if (!settings) return <Typography>No settings available</Typography>

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto', p: 2 }}>
      <Paper sx={{ p: 3 }} elevation={3}>
        <Typography variant="h5" gutterBottom>FX Settings</Typography>
        <Typography variant="body2" sx={{ mb: 2 }}>
          Manage foreign exchange configuration (base currency, quotes, provider & refresh schedule).
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <Box sx={{ flex: '1 1 260px' }}>
            <Typography variant="subtitle2">Base Currency</Typography>
            <CurrencySelect value={settings.baseCurrency} onChange={(val) => setSettings({ ...settings, baseCurrency: val })} />
          </Box>
          <Box sx={{ flex: '1 1 260px' }}>
            <Typography variant="subtitle2">Quotes</Typography>
            <Autocomplete
              multiple
              size="small"
              options={[settings.baseCurrency, 'HUF', 'EUR', 'USD'].filter((v,i,a) => a.indexOf(v)===i)}
              value={quotes}
              onChange={handleQuoteChange}
              renderInput={(params) => <TextField {...params} label="Quote Currencies" placeholder="Select quotes" />}
            />
          </Box>
          <Box sx={{ flex: '1 1 260px' }}>
            <TextField label="API Base URL" fullWidth size="small" value={settings.apiBaseUrl || ''} onChange={handleField('apiBaseUrl')} />
          </Box>
          <Box sx={{ flex: '1 1 260px' }}>
            <TextField label="Refresh Cron" fullWidth size="small" value={settings.refreshCron || ''} onChange={handleField('refreshCron')} />
          </Box>
          <Box sx={{ flex: '1 1 260px' }}>
            <TextField label="Provider" fullWidth size="small" value={settings.provider || ''} onChange={handleField('provider')} />
          </Box>
          <Box sx={{ flex: '1 1 200px', display: 'flex', alignItems: 'center' }}>
            <FormControlLabel control={<Switch checked={!!settings.enabled} onChange={handleToggleEnabled} />} label="Enabled" />
          </Box>
        </Box>
        <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
          <Button variant="contained" color="primary" disabled={saving} onClick={save}>Save</Button>
          {saving && <Typography variant="body2">Saving...</Typography>}
          {error && <Typography color="error">{error}</Typography>}
        </Box>
        <Box sx={{ mt: 3 }}>
          <Typography variant="caption" color="text.secondary">
            Display currency currently: {displayCurrency}. Preview conversions dependent on quotes availability.
          </Typography>
        </Box>
      </Paper>
    </Box>
  )
}
