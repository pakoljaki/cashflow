import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import KpiPage from '../KpiPage'
import { CurrencyProvider } from '../../context/CurrencyContext'

// Mock fetch responses: initial KPI data for HUF, then EUR switch
globalThis.fetch = jest.fn((url) => {
  if (url.startsWith('/api/business-kpi')) {
    // simplistic differentiation by query param
    if (url.includes('displayCurrency=EUR')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({
        displayCurrency: 'EUR', baseCurrency: 'HUF', year: 2025,
        startBalance: 2500, originalStartBalance: 1000000,
        monthlyData: [{ month: 1, totalIncome: 500, totalExpense: 100, originalTotalIncome: 200000, originalTotalExpense: 40000, rateDate: '2025-11-01' }]
      }) })
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({
      displayCurrency: 'HUF', baseCurrency: 'HUF', year: 2025,
      startBalance: 1000000, monthlyData: [{ month: 1, totalIncome: 200000, totalExpense: 40000 }]
    }) })
  }
  return Promise.resolve({ ok: true, json: () => Promise.resolve({}) })
})

describe('KpiPage currency conversion integration', () => {
  test('switching display currency updates amounts', async () => {
    render(<CurrencyProvider><KpiPage /></CurrencyProvider>)

    // Fill form (YearBalanceForm) assuming inputs exist with labels
    const startBalanceInput = screen.getByLabelText(/start balance/i)
    fireEvent.change(startBalanceInput, { target: { value: '1000000' } })
    const submitBtn = screen.getByRole('button', { name: /generate/i })
    fireEvent.click(submitBtn)

    await waitFor(() => expect(screen.getByText(/KPI Dashboard/i)).toBeInTheDocument())

    // Switch currency
    const currencySelect = screen.getByLabelText(/Display Currency/i)
    fireEvent.mouseDown(currencySelect)
    // For simplicity directly fire change
    fireEvent.change(currencySelect, { target: { value: 'EUR' } })

    await waitFor(() => expect(screen.getByText(/Updating conversion/i)).toBeInTheDocument())
    await waitFor(() => expect(screen.getByText(/Income \(EUR\)/i)).toBeInTheDocument())
  })
})
