import { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  Button,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TextField,
  Snackbar,
  Alert,
} from '@mui/material'
import { useCurrency } from '../context/CurrencyContext'
import UserManagement from '../components/UserManagement'

export default function AdminPage() {
  const { roles } = useCurrency()
  const [selectedFile, setSelectedFile] = useState(null)
  const [imports, setImports] = useState([])
  const [messageInfo, setMessageInfo] = useState({ text: '', severity: 'info', open: false })

  useEffect(() => {
    fetchImports()
  }, [])

  const fetchImports = async () => {
    const token = localStorage.getItem('token')
    try {
      const res = await fetch('/api/admin/csv/imports', {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error(await res.text())
      setImports(await res.json())
    } catch {
      notify('Failed to load importable files', 'error')
    }
  }

  const handleFileChange = e => {
    setSelectedFile(e.target.files[0] || null)
  }

  const handleUpload = async () => {
    if (!selectedFile) {
      notify('No file selected', 'warning')
      return
    }
    const token = localStorage.getItem('token')
    const formData = new FormData()
    formData.append('file', selectedFile)
    try {
      const res = await fetch('/api/admin/csv/upload', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      })
      if (!res.ok) throw new Error(await res.text())
      notify('Upload & parse succeeded', 'success')
      setSelectedFile(null)
      fetchImports()
    } catch (err) {
      notify('Upload failed: ' + err.message, 'error')
    }
  }

  const handleImportExisting = async fileName => {
    const token = localStorage.getItem('token')
    try {
      const res = await fetch(`/api/admin/csv/import?file=${encodeURIComponent(fileName)}`, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error(await res.text())
      notify(`Imported "${fileName}"`, 'success')
      fetchImports()
    } catch (err) {
      notify('Import failed: ' + err.message, 'error')
    }
  }

  const notify = (text, severity) => {
    setMessageInfo({ text, severity, open: true })
  }

  const isAdmin = roles.includes('ROLE_ADMIN')

  if (!isAdmin) {
    return (
      <Box sx={{ p: 4, textAlign: 'center' }}>
        <Typography variant="h5" color="error">
          Access Denied: Admins only
        </Typography>
      </Box>
    )
  }

  return (
    <Box sx={{ p: 4, bgcolor: '#f5f7fa', minHeight: 'calc(100vh - 200px)' }}>
      <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
        <Box sx={{ mb: 4, textAlign: 'center' }}>
          <Typography variant="h3" gutterBottom sx={{ fontWeight: 700, color: '#1976d2' }}>
            Admin Panel
          </Typography>
          <Typography variant="subtitle1" color="text.secondary">
            Manage CSV imports and user accounts
          </Typography>
        </Box>

        <Box sx={{ display: 'flex', gap: 3, flexDirection: { xs: 'column', lg: 'row' } }}>
          {/* Left side - CSV Upload */}
          <Box sx={{ flex: 1 }}>
            <Paper elevation={2} sx={{ p: 4, borderRadius: 2, height: '100%' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Box sx={{ 
                  bgcolor: 'primary.main', 
                  color: 'white', 
                  p: 1.5, 
                  borderRadius: 2, 
                  mr: 2,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  📁
                </Box>
                <Typography variant="h5" sx={{ fontWeight: 600 }}>
                  CSV Upload & Import
                </Typography>
              </Box>

              <Box sx={{ mb: 4 }}>
                <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600, color: 'text.secondary' }}>
                  Upload New CSV File
                </Typography>
                <Paper variant="outlined" sx={{ p: 3, bgcolor: '#fafbfc', borderRadius: 2, borderStyle: 'dashed' }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
                    <Button variant="contained" component="label" size="large" sx={{ minWidth: 140 }}>
                      Choose File
                      <input type="file" accept=".csv" hidden onChange={handleFileChange} />
                    </Button>
                    <TextField
                      size="medium"
                      variant="outlined"
                      placeholder="No file selected"
                      value={selectedFile?.name || ''}
                      InputProps={{ readOnly: true }}
                      sx={{ flex: 1, minWidth: 200 }}
                    />
                    <Button 
                      variant="contained" 
                      color="success" 
                      onClick={handleUpload} 
                      size="large"
                      sx={{ minWidth: 140 }}
                    >
                      Upload & Parse
                    </Button>
                  </Box>
                </Paper>
              </Box>

              <Box>
                <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600, color: 'text.secondary' }}>
                  Available Import Files
                </Typography>
                <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
                  <Table>
                    <TableHead>
                      <TableRow sx={{ bgcolor: '#1976d2' }}>
                        <TableCell sx={{ fontWeight: 700, color: 'white', fontSize: '0.95rem' }}>
                          Filename
                        </TableCell>
                        <TableCell align="right" sx={{ fontWeight: 700, color: 'white', fontSize: '0.95rem' }}>
                          Action
                        </TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {imports.map((fn, idx) => (
                        <TableRow 
                          key={fn} 
                          hover 
                          sx={{ 
                            '&:nth-of-type(odd)': { bgcolor: '#f9f9f9' },
                            '&:hover': { bgcolor: '#e3f2fd' }
                          }}
                        >
                          <TableCell sx={{ py: 2 }}>{fn}</TableCell>
                          <TableCell align="right">
                            <Button
                              size="medium"
                              variant="outlined"
                              color="primary"
                              onClick={() => handleImportExisting(fn)}
                            >
                              Import
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                      {imports.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={2} align="center" sx={{ py: 6, color: 'text.secondary' }}>
                            No import files available
                          </TableCell>
                        </TableRow>
                      )}
                    </TableBody>
                  </Table>
                </TableContainer>
              </Box>
            </Paper>
          </Box>

          {/* Right side - User Management */}
          <Box sx={{ flex: 1 }}>
            <Paper elevation={2} sx={{ p: 4, borderRadius: 2, height: '100%', display: 'flex', flexDirection: 'column' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', mb: 3 }}>
                <Box sx={{ 
                  bgcolor: 'secondary.main', 
                  color: 'white', 
                  p: 1.5, 
                  borderRadius: 2, 
                  mr: 2,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}>
                  👥
                </Box>
                <Typography variant="h5" sx={{ fontWeight: 600 }}>
                  User Management
                </Typography>
              </Box>
              <UserManagement />
            </Paper>
          </Box>
        </Box>
      </Box>

      <Snackbar
        open={messageInfo.open}
        autoHideDuration={4000}
        onClose={() => setMessageInfo(prev => ({ ...prev, open: false }))}
      >
        <Alert
          elevation={6}
          variant="filled"
          severity={messageInfo.severity}
          onClose={() => setMessageInfo(prev => ({ ...prev, open: false }))}
        >
          {messageInfo.text}
        </Alert>
      </Snackbar>
    </Box>
  )
}
