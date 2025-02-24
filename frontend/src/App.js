
import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AdminCsvUpload from './pages/AdminCsvUpload';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/" element={<Login />} />  {/* Default route */}
        <Route path="/admin/csv-upload" element={<AdminCsvUpload />} />
      </Routes>
    </Router>
  );
}

export default App;
