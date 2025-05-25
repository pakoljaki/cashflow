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
import '../styles/csvupload.css'

export default function CsvUpload() {
  const [role, setRole] = useState(null)
  const [selectedFile, setSelectedFile] = useState(null)
  const [imports, setImports] = useState([])
  const [messageInfo, setMessageInfo] = useState({ text: '', severity: 'info', open: false })

  useEffect(() => {
    setRole(localStorage.getItem('userRole'))
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

  if (role !== 'ROLE_ADMIN') {
    return (
      <Box className="csvupload-page">
        <Typography variant="h6" color="error">
          Access Denied: Admins only
        </Typography>
      </Box>
    )
  }

  return (
    <Box className="csvupload-page">
      <Typography variant="h5" align="center" gutterBottom>
        Admin CSV Upload & Import
      </Typography>

      <Paper className="upload-section" elevation={3}>
        <Typography variant="h6" gutterBottom>
          Upload New CSV
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button variant="contained" component="label">
            Choose File
            <input type="file" accept=".csv" hidden onChange={handleFileChange} />
          </Button>
          <TextField
            size="small"
            variant="outlined"
            placeholder="No file selected"
            value={selectedFile?.name || ''}
            InputProps={{ readOnly: true }}
            sx={{ flex: 1 }}
          />
          <Button variant="contained" color="primary" onClick={handleUpload}>
            Upload & Parse
          </Button>
        </Box>
      </Paper>

      <Paper className="imports-section" elevation={3}>
        <Typography variant="h6" gutterBottom>
          Available Imports
        </Typography>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Filename</TableCell>
                <TableCell align="right">Action</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {imports.map(fn => (
                <TableRow key={fn} hover>
                  <TableCell>{fn}</TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      variant="outlined"
                      onClick={() => handleImportExisting(fn)}
                    >
                      Import
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
              {imports.length === 0 && (
                <TableRow>
                  <TableCell colSpan={2} align="center">
                    No files available
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </Paper>

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
