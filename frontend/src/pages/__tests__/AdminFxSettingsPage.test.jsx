import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import AdminFxSettingsPage from '../AdminFxSettingsPage'
import { CurrencyProvider } from '../../context/CurrencyContext'

const calls = []

const initialSettings = {
  baseCurrency: 'HUF',
  quotes: ['HUF','EUR'],
  refreshCron: '0 0 * * *',
  provider: 'ECB',
  enabled: true,
  apiBaseUrl: 'https://fx.example'
}

const updatedSettingsMatcher = (bodyObj) => (
  bodyObj.provider === 'Custom' && bodyObj.enabled === false && bodyObj.baseCurrency === 'EUR'
)

beforeEach(() => {
  calls.length = 0
  globalThis.fetch = jest.fn((url, options = {}) => {
    calls.push({ url, options })
    if (url === '/api/fx/settings' && (!options.method || options.method === 'GET')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(initialSettings) })
    }
    if (url === '/api/fx/settings' && options.method === 'PUT') {
      const parsed = JSON.parse(options.body)
      return Promise.resolve({ ok: true, json: () => Promise.resolve(parsed) })
    }
    return Promise.resolve({ ok: true, json: () => Promise.resolve({}) })
  })
})

afterEach(() => {
  jest.resetAllMocks()
  localStorage.clear()
})

describe('AdminFxSettingsPage integration', () => {
  test('loads settings, edits fields, submits PUT', async () => {
    const payload = btoa(JSON.stringify({ roles: ['ROLE_ADMIN'] }))
    localStorage.setItem('token', `x.${payload}.y`)

    render(<CurrencyProvider><AdminFxSettingsPage /></CurrencyProvider>)

    const apiBaseField = await screen.findByLabelText(/API Base URL/i)
    expect(apiBaseField.value).toBe('https://fx.example')

    // Change Provider
    const providerField = screen.getByLabelText(/Provider/i)
    fireEvent.change(providerField, { target: { value: 'Custom' } })

    // Toggle Enabled off
    const enabledSwitch = screen.getByLabelText(/Enabled/i)
    fireEvent.click(enabledSwitch)

  const baseCurrencySelect = screen.getByRole('combobox', { name: /Currency/i })
  fireEvent.mouseDown(baseCurrencySelect)
  const eurOption = await screen.findByRole('option', { name: 'EUR' })
  fireEvent.click(eurOption)

    const saveBtn = screen.getByRole('button', { name: /Save/i })
    fireEvent.click(saveBtn)

    await waitFor(() => {
      const putCall = calls.find(c => c.options.method === 'PUT')
      expect(putCall).toBeTruthy()
      const bodyObj = JSON.parse(putCall.options.body)
      expect(updatedSettingsMatcher(bodyObj)).toBe(true)
    })

    expect(providerField.value).toBe('Custom')
  })
})
