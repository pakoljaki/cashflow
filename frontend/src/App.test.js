import { render, screen } from '@testing-library/react'
import App from './App'

test('renders home splash and primary nav links', () => {
  render(<App />)

  expect(screen.getByRole('link', { name: /cashflow planner/i })).toBeInTheDocument()
  expect(screen.getByText(/welcome to cashflow planner/i)).toBeInTheDocument()
  expect(screen.getByRole('button', { name: /transactions/i })).toBeInTheDocument()
})
