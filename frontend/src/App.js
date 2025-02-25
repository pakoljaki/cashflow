import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import MyNavbar from './components/MyNavBar';
import HomePage from './pages/HomePage';
import TransactionsPage from './pages/TransactionsPage';
import AdminCsvUpload from './pages/AdminCsvUpload';
// ... import more pages as needed

function App() {
  return (
    <Router>
      <MyNavbar />
      <div style={{ padding: '20px' }}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/admin/csv-upload" element={<AdminCsvUpload />} />
          <Route path="/transactions" element={<TransactionsPage />} />
          {/* Add other routes */}
        </Routes>
      </div>
    </Router>
  );
}

export default App;
