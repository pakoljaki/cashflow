import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';

// Components in the root folder (or in /src/components/ if that’s your structure)
import MyNavBar from './components/MyNavBar';
import HomePage from './pages/HomePage';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AdminCsvUpload from './pages/AdminCsvUpload';
import TransactionsPage from './pages/TransactionsPage';
import AdminDashboard from './pages/AdminDashboard';

// New pages (inside /src/pages/)
import CashflowPlansPage from './pages/CashflowPlansPage';
import PlanLineItemsPage from './pages/PlanLineItemsPage';
import KpiPage from './pages/KpiPage';
import AboutPage from './pages/AboutPage';  // Create this file if not already present

function App() {
  return (
    <Router>
      <MyNavBar />
      <div className="app-container">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/admin/csv-upload" element={<AdminCsvUpload />} />
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          
          {/* NEW ROUTES */}
          <Route path="/cashflow-plans" element={<CashflowPlansPage />} />
          <Route path="/plans/:planId" element={<PlanLineItemsPage />} />
          <Route path="/kpis" element={<KpiPage />} />
          <Route path="/about" element={<AboutPage />} />

          {/* 404 fallback */}
          <Route path="*" element={<h2>404: Not Found</h2>} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
