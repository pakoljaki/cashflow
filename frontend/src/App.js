import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import MyNavBar from './components/MyNavBar';
import HomePage from './pages/HomePage';
import Login from './pages/Login';
import Register from './pages/Register';
import CsvUpload from './pages/CsvUpload';
import TransactionsPage from './pages/TransactionsPage';
import AdminDashboard from './pages/AdminDashboard';
import CashflowPlansPage from './pages/CashflowPlansPage';
import ScenarioGroupLineItemsPage from './pages/ScenarioGroupLineItemsPage';
import KpiPage from './pages/KpiPage';
import CategoryMappingPage from './pages/CategoryMappingPage';

function App() {
  return (
    <Router>
      <MyNavBar />
      <div className="app-container" style={{ padding: '1rem' }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/admin/csv-upload" element={<CsvUpload />} />
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/cashflow-plans" element={<CashflowPlansPage />} />
          <Route path="/scenario-group/:groupKey" element={<ScenarioGroupLineItemsPage />} />
          <Route path="/kpis" element={<KpiPage />} />
          <Route path="/accounting-mapping" element={<CategoryMappingPage />} />
          <Route path="*" element={<h2>404: Not Found</h2>} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
