import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import MyNavBar from './components/MyNavBar';
import { CurrencyProvider, useCurrency } from './context/AppContext';
import HomePage from './pages/HomePage';
import Login from './pages/Login';
import Register from './pages/Register';
import AdminPage from './pages/Admin';
import TransactionsPage from './pages/TransactionsPage';
import AdminDashboard from './pages/AdminDashboard';
import AdminFxSettingsPage from './pages/AdminFxSettingsPage';
import CashflowPlansPage from './pages/CashflowPlansPage';
import ScenarioGroupLineItemsPage from './pages/ScenarioGroupLineItemsPage';
import KpiPage from './pages/KpiPage';
import CategoryMappingPage from './pages/CategoryMappingPage';

function AppInner() {
  const { roles } = useCurrency()
  const isAdmin = roles.includes('ROLE_ADMIN')
  return (
    <Router>
      <MyNavBar />
      <div className="app-container" style={{ padding: '1rem' }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/admin" element={isAdmin ? <AdminPage /> : <h2>403: Forbidden</h2>} />
          <Route path="/admin/csv-upload" element={isAdmin ? <AdminPage /> : <h2>403: Forbidden</h2>} />
          <Route path="/admin/dashboard" element={isAdmin ? <AdminDashboard /> : <h2>403: Forbidden</h2>} />
          <Route path="/admin/fx-settings" element={isAdmin ? <AdminFxSettingsPage /> : <h2>403: Forbidden</h2>} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/cashflow-plans" element={<CashflowPlansPage />} />
          <Route path="/scenario-group/:groupKey" element={<ScenarioGroupLineItemsPage />} />
          <Route path="/kpis" element={<KpiPage />} />
          <Route path="/accounting-mapping" element={<CategoryMappingPage />} />
          <Route path="*" element={<h2>404: Not Found</h2>} />
        </Routes>
      </div>
    </Router>
  )
}

function App() {
  return (
    <CurrencyProvider>
      <AppInner />
    </CurrencyProvider>
  )
}

export default App;
