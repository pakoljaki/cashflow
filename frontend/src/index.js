import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import { ThemeProvider, createTheme } from '@mui/material/styles'
import CssBaseline from '@mui/material/CssBaseline'
import { amountFormatter } from './utils/numberFormatter'

const theme = createTheme({
  palette: {
    primary: { main: '#007bff' },
    secondary: { main: '#ffdd57' },
    success: { main: '#28a745' },
    error: { main: '#dc3545' },
  },
  typography: {
    fontFamily: '"Segoe UI", Tahoma, Geneva, Verdana, sans-serif',
  },
})

window.formatAmount = (n) => amountFormatter.format(Number(n) || 0)

const root = ReactDOM.createRoot(document.getElementById('root'))
root.render(
  <ThemeProvider theme={theme}>
    <CssBaseline />
    <App />
  </ThemeProvider>
)
