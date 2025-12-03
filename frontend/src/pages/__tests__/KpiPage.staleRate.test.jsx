import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import KpiPage from '../KpiPage'
import { CurrencyProvider } from '../../context/CurrencyContext'
import { defaultKpiPayload, setupKpiFetchMocks, resetKpiFetchMocks } from '../../testUtils/mockKpiNetwork'

beforeEach(() => {
  setupKpiFetchMocks(defaultKpiPayload)
})

afterEach(() => {
  resetKpiFetchMocks()
})

describe('KpiPage dashboard info popover', () => {
  test('shows staleness guidance when info button is opened', async () => {
    render(<CurrencyProvider><KpiPage /></CurrencyProvider>)

    fireEvent.change(screen.getByLabelText(/Starting Balance/i), { target: { value: '1000000' } })
    fireEvent.click(screen.getByRole('button', { name: /See KPIs/i }))

    await waitFor(() => expect(screen.getByRole('heading', { name: /KPI Dashboard/i })).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: /Information about this dashboard/i }))
    await waitFor(() => expect(screen.getByText(/Rates may become stale after 2 days/i)).toBeInTheDocument())
    expect(screen.getByText(/About This Dashboard/i)).toBeInTheDocument()
  })
})
