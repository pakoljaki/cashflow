import React from 'react'
import { render } from '@testing-library/react'
import DualAmount from '../DualAmount'

describe('DualAmount', () => {
  it('renders single amount without tooltip when single=true', () => {
    const { container, queryByRole } = render(<DualAmount dual={{ single: true, nativeFormatted: '1,000 HUF' }} />)
    expect(container.textContent).toContain('1,000 HUF')
    expect(queryByRole('tooltip')).toBeNull()
  })

  it('renders dual amount with converted value and tooltip', () => {
    const dual = {
      single: false,
      nativeFormatted: '1,000 HUF',
      convertedFormatted: '2.50 EUR',
      tooltip: 'Rate date: 2025-11-01 (ECB)',
      displayCurrency: 'EUR',
      currency: 'HUF'
    }
    const { container } = render(<DualAmount dual={dual} />)
    expect(container.textContent).toContain('1,000 HUF')
    expect(container.textContent).toContain('2.50 EUR')
  })
})
