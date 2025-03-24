import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

// Navbar
import MyNavBar from './components/MyNavBar';

// Pages
import HomePage from './pages/HomePage';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AdminCsvUpload from './pages/AdminCsvUpload';
import TransactionsPage from './pages/TransactionsPage';
import AdminDashboard from './pages/AdminDashboard';
import PlanLineItemsPage from './pages/PlanLineItemsPage';
import CashflowPlansPage from './pages/CashflowPlansPage';
import ScenarioGroupLineItemsPage from './pages/ScenarioGroupLineItemsPage';
import KpiPage from './pages/KpiPage';
import AboutPage from './pages/AboutPage';

function App() {
  return (
    <Router>
      <MyNavBar />
      <div className="app-container" style={{ padding: '1rem' }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/admin/csv-upload" element={<AdminCsvUpload />} />
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/transactions" element={<TransactionsPage />} />

          {/* Single-Plan approach */}
          <Route path="/plans/:planId" element={<PlanLineItemsPage />} />

          {/* Scenario-based approach */}
          <Route path="/cashflow-plans" element={<CashflowPlansPage />} />
          <Route path="/scenario-group/:groupKey" element={<ScenarioGroupLineItemsPage />} />

          {/* Additional routes (KPI, About, etc.) */}
          <Route path="/kpis" element={<KpiPage />} />
          <Route path="/about" element={<AboutPage />} />

          {/* 404 fallback, optional */}
          <Route path="*" element={<h2>404: Not Found</h2>} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
