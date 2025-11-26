import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import KpiPage from '../KpiPage'
import { CurrencyProvider } from '../../context/CurrencyContext'

const FIXED_NOW = new Date('2025-11-02T12:00:00Z').getTime()
const realDateNow = Date.now
beforeAll(() => { globalThis.Date.now = () => FIXED_NOW })
afterAll(() => { globalThis.Date.now = realDateNow })

const STALE_RATE_DATE = '2025-10-20'

globalThis.fetch = jest.fn((url) => {
  if (url.startsWith('/api/business-kpi')) {
    if (url.includes('displayCurrency=EUR')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve({
        displayCurrency: 'EUR', baseCurrency: 'HUF', year: 2025,
        startBalance: 2500, originalStartBalance: 1000000,
        monthlyData: [{ month: 1, totalIncome: 500, totalExpense: 100, originalTotalIncome: 200000, originalTotalExpense: 40000, rateDate: STALE_RATE_DATE, rateSource: 'ECB' }]
      }) })
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({
      displayCurrency: 'HUF', baseCurrency: 'HUF', year: 2025,
      startBalance: 1000000, monthlyData: [{ month: 1, totalIncome: 200000, totalExpense: 40000 }]
    }) })
  }
  return Promise.resolve({ ok: true, json: () => Promise.resolve({}) })
})

describe('Rate staleness UI integration', () => {
  test('shows stale badge and banner when rateDate exceeds stale threshold', async () => {
    render(<CurrencyProvider><KpiPage /></CurrencyProvider>)

    const startBalanceInput = screen.getByLabelText(/Starting Balance/i)
    fireEvent.change(startBalanceInput, { target: { value: '1000000' } })
    const submitBtn = screen.getByRole('button', { name: /See KPIs/i })
    fireEvent.click(submitBtn)

    await waitFor(() => expect(screen.getByText(/KPI Dashboard/i)).toBeInTheDocument())

    const currencySelect = screen.getByLabelText(/Display Currency/i)
    fireEvent.mouseDown(currencySelect)
    fireEvent.change(currencySelect, { target: { value: 'EUR' } })

    await waitFor(() => expect(screen.getByText(/Rate STALE/i)).toBeInTheDocument())
    await waitFor(() => expect(screen.getByText(/FX rate is/i)).toBeInTheDocument())
    expect(screen.getByText(/stale threshold/i)).toBeInTheDocument()
  })
})
