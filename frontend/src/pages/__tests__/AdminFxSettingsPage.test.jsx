import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import AdminFxSettingsPage from '../AdminFxSettingsPage'
import { CurrencyProvider } from '../../context/CurrencyContext'

// Capture fetch calls for assertions
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
  // Mock fetch BEFORE render so both context refresh and page GET share same handler
  globalThis.fetch = jest.fn((url, options = {}) => {
    calls.push({ url, options })
    // All GETs for fx settings
    if (url === '/api/settings/fx' && (!options.method || options.method === 'GET')) {
      return Promise.resolve({ ok: true, json: () => Promise.resolve(initialSettings) })
    }
    if (url === '/api/settings/fx' && options.method === 'PUT') {
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
    // Provide fake JWT payload with roles to suppress warnings (simple base64 section with roles)
    const payload = btoa(JSON.stringify({ roles: ['ROLE_ADMIN'] }))
    localStorage.setItem('token', `x.${payload}.y`)

    render(<CurrencyProvider><AdminFxSettingsPage /></CurrencyProvider>)

    // Wait for settings to load (API Base URL field present)
    const apiBaseField = await screen.findByLabelText(/API Base URL/i)
    expect(apiBaseField.value).toBe('https://fx.example')

    // Change Provider
    const providerField = screen.getByLabelText(/Provider/i)
    fireEvent.change(providerField, { target: { value: 'Custom' } })

    // Toggle Enabled off
    const enabledSwitch = screen.getByLabelText(/Enabled/i)
    fireEvent.click(enabledSwitch)

  // Base Currency select: use role combobox tied to currency-select id
  const baseCurrencySelect = screen.getByRole('combobox', { name: /Currency/i })
  fireEvent.mouseDown(baseCurrencySelect)
  const eurOption = await screen.findByRole('option', { name: 'EUR' })
  fireEvent.click(eurOption)

    // Save
    const saveBtn = screen.getByRole('button', { name: /Save/i })
    fireEvent.click(saveBtn)

    await waitFor(() => {
      const putCall = calls.find(c => c.options.method === 'PUT')
      expect(putCall).toBeTruthy()
      const bodyObj = JSON.parse(putCall.options.body)
      expect(updatedSettingsMatcher(bodyObj)).toBe(true)
    })

    // Provider field updated
    expect(providerField.value).toBe('Custom')
  })
})
