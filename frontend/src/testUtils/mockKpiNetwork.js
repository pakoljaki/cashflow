const accountingCategoriesResponse = [
  { code: 'SALARY', direction: 'POSITIVE', displayName: 'Salary' },
  { code: 'BONUS', direction: 'POSITIVE', displayName: 'Bonus' },
  { code: 'RENT', direction: 'NEGATIVE', displayName: 'Rent' },
  { code: 'GROCERIES', direction: 'NEGATIVE', displayName: 'Groceries' }
]

const transactionCategoriesResponse = [
  { name: 'Paycheck', direction: 'POSITIVE' },
  { name: 'Freelance', direction: 'POSITIVE' },
  { name: 'Utilities', direction: 'NEGATIVE' },
  { name: 'Subscriptions', direction: 'NEGATIVE' }
]

const respondWith = (payload) => Promise.resolve({
  ok: true,
  json: () => Promise.resolve(payload)
})

export const defaultKpiPayload = {
  baseCurrency: 'HUF',
  startBalance: 1000000,
  originalStartBalance: 1000000,
  monthlyData: [
    {
      month: '2024-01',
      totalIncome: 200000,
      totalExpense: -40000,
      netCashflow: 160000,
      accountingCategorySums: { SALARY: 150000, BONUS: 50000, RENT: -30000, GROCERIES: -10000 },
      transactionCategorySums: { Paycheck: 150000, Freelance: 50000, Utilities: -20000, Subscriptions: -20000 }
    }
  ]
}

export const setupKpiFetchMocks = (kpiPayload = defaultKpiPayload) => {
  localStorage.setItem('token', 'test-token')
  const fetchMock = jest.fn((input) => {
    const url = typeof input === 'string' ? input : input?.url ?? ''
    if (url.includes('/api/accounting-categories')) {
      return respondWith(accountingCategoriesResponse)
    }
    if (url.includes('/api/categories')) {
      return respondWith(transactionCategoriesResponse)
    }
    if (url.includes('/api/business-kpi')) {
      return respondWith(kpiPayload)
    }
    return respondWith({})
  })
  globalThis.fetch = fetchMock
  return fetchMock
}

export const resetKpiFetchMocks = () => {
  jest.resetAllMocks()
  localStorage.clear()
}
