import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import KpiPage from '../KpiPage'
import { CurrencyProvider } from '../../context/CurrencyContext'
import { defaultKpiPayload, setupKpiFetchMocks, resetKpiFetchMocks } from '../../testUtils/mockKpiNetwork'

const mockKpiPayload = {
  ...defaultKpiPayload,
  monthlyData: [
    {
      month: '2024-01',
      totalIncome: 200000,
      totalExpense: -40000,
      netCashflow: 160000,
      accountingCategorySums: { SALARY: 120000, BONUS: 80000, RENT: -25000, GROCERIES: -15000 },
      transactionCategorySums: { Paycheck: 120000, Freelance: 80000, Utilities: -20000, Subscriptions: -20000 }
    }
  ]
}

let fetchMock

beforeEach(() => {
  fetchMock = setupKpiFetchMocks(mockKpiPayload)
})

afterEach(() => {
  resetKpiFetchMocks()
})

describe('KpiPage currency conversion integration', () => {
  test('submitting the form loads the KPI dashboard and shows fetched data', async () => {
    render(<CurrencyProvider><KpiPage /></CurrencyProvider>)

    const startBalanceInput = screen.getByLabelText(/starting balance/i)
    fireEvent.change(startBalanceInput, { target: { value: '1000000' } })
    const submitBtn = screen.getByRole('button', { name: /see kpis/i })
    fireEvent.click(submitBtn)

    await waitFor(() => expect(screen.getByRole('heading', { name: /kpi dashboard/i })).toBeInTheDocument())

    expect(screen.getByText(/Starting Balance:/i)).toHaveTextContent('HUF')
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/api/business-kpi'), expect.any(Object))
    expect(screen.getByText(/Accounting Income/i)).toBeInTheDocument()
  })

  test('change settings button returns to the year/balance form', async () => {
    render(<CurrencyProvider><KpiPage /></CurrencyProvider>)

    fireEvent.change(screen.getByLabelText(/starting balance/i), { target: { value: '500000' } })
    fireEvent.click(screen.getByRole('button', { name: /see kpis/i }))

    await waitFor(() => expect(screen.getByRole('heading', { name: /kpi dashboard/i })).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /change settings/i }))
    expect(screen.getByText(/Select Year, Starting Balance/i)).toBeInTheDocument()
  })
})
