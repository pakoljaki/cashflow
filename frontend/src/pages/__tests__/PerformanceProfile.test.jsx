import React, { Profiler } from 'react'
import { render, screen, act } from '@testing-library/react'
import MonthlyDataTable from '../../components/MonthlyDataTable'
import MonthlyBarChart from '../../components/MonthlyBarChart'
import DualAmount from '../../components/DualAmount'
import { CurrencyProvider } from '../../context/CurrencyContext'

// Polyfill ResizeObserver for Recharts ResponsiveContainer in test environment
class RO {
  observe() { /* noop for test perf */ }
  unobserve() { /* noop */ }
  disconnect() { /* noop */ }
}
// Assign using globalThis to satisfy lint preference
globalThis.ResizeObserver = globalThis.ResizeObserver || RO

// Synthetic KPI monthly dataset to approximate real rendering complexity
const makeData = (months = 12) => Array.from({ length: months }, (_, i) => ({
  month: i + 1,
  totalIncome: 10000 + i * 123,
  totalExpense: 4000 + i * 77,
  originalTotalIncome: 9500 + i * 120,
  originalTotalExpense: 3800 + i * 75,
  rateDate: '2025-10-15',
  rateSource: 'ECB',
  accountingCategorySums: {
    SALES: 8000 + i * 111,
    SERVICES: 2000 + i * 22,
    OPEX: 1500 + i * 33,
    TAX: 900 + i * 11,
  },
}))

const monthlyData = makeData(12)

let measures = []
const onRender = (id, phase, actualDuration, baseDuration, startTime, commitTime) => {
  measures.push({ id, phase, actualDuration, baseDuration, startTime, commitTime })
}

function PerfScenario() {
  return (
    <CurrencyProvider>
      <div>
        <Profiler id="MonthlyDataTable" onRender={onRender}>
          <MonthlyDataTable
            startBalance={50000}
            originalStartBalance={48000}
            baseCurrency="HUF"
            monthlyData={monthlyData}
          />
        </Profiler>
        <Profiler id="MonthlyBarChart" onRender={onRender}>
          <MonthlyBarChart data={monthlyData} displayCurrency="EUR" />
        </Profiler>
        <Profiler id="DualAmount" onRender={onRender}>
          <DualAmount dual={{
            single: false,
            nativeFormatted: '48,000 HUF',
            convertedFormatted: '50,000 EUR',
            tooltip: 'Rate date: 2025-10-15 (ECB)',
            displayCurrency: 'EUR',
            currency: 'HUF',
          }} />
        </Profiler>
      </div>
    </CurrencyProvider>
  )
}

describe('Performance profiling (approximate)', () => {
  test('collects render durations for key FX components', () => {
    measures = []
    act(() => {
      render(<PerfScenario />)
    })

  // Basic assertions to ensure components rendered
  expect(screen.getByRole('table')).toBeInTheDocument()
  expect(screen.getByText(/Open Balance/)).toBeInTheDocument()
  expect(screen.getByText(/48,000 HUF/)).toBeInTheDocument()
  // Ensure profiler captured all target component ids
  const ids = measures.map(m => m.id)
  expect(ids).toEqual(expect.arrayContaining(['MonthlyDataTable','MonthlyBarChart','DualAmount']))

    // Emit summary to console for developer review
    const summary = measures.reduce((acc, m) => {
      acc[m.id] = acc[m.id] || { renders: 0, total: 0, base: 0 }
      acc[m.id].renders += 1
      acc[m.id].total += m.actualDuration
      acc[m.id].base += m.baseDuration
      return acc
    }, {})
    // Sort by total duration
    const ordered = Object.entries(summary).sort((a,b) => b[1].total - a[1].total)
    // Log structured summary (can be copied into README or further tooling)
    console.log('FX Performance Summary:', ordered.map(([id, stats]) => ({
      id,
      renders: stats.renders,
      totalMs: Number(stats.total.toFixed(2)),
      avgMs: Number((stats.total / stats.renders).toFixed(2)),
      baseMs: Number(stats.base.toFixed(2)),
    })))
  })
})
