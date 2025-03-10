import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function CashflowPlansPage() {
  const navigate = useNavigate();

  const [plans, setPlans] = useState([]);
  const [basePlanName, setBasePlanName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [startBalance, setStartBalance] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetchAllPlans();
  }, []);

  const fetchAllPlans = async () => {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }

    try {
      const resp = await fetch('/api/cashflow-plans', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt || 'Failed to fetch plans');
      }
      const data = await resp.json();
      setPlans(data);
    } catch (error) {
      setMessage('Error: ' + error.message);
    }
  };

  const handleCreateScenarios = async () => {
    if (!basePlanName || !startDate || !endDate) {
      alert('Please fill in base plan name, start date, and end date');
      return;
    }

    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }

    const bodyData = {
      basePlanName,
      startDate,
      endDate,
      startBalance: startBalance || '0'
    };

    try {
      const resp = await fetch('/api/cashflow-plans/scenarios', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(bodyData)
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt);
      }
      const data = await resp.json(); // Should be an array of 3 created plans
      setMessage(`Created 3 scenario plans successfully!`);
      // Refresh or append them to the list
      setPlans((old) => [...old, ...data]);
      // Clear form
      setBasePlanName('');
      setStartDate('');
      setEndDate('');
      setStartBalance('');
    } catch (error) {
      setMessage('Error creating scenario plans: ' + error.message);
    }
  };

  return (
    <div style={{ padding: '1rem' }}>
      <h2>Cashflow Plans</h2>
      <p>Create a new 3-scenario plan set or view existing plans.</p>

      {/* Form to create 3-scenario group */}
      <div style={{ border: '1px solid #ccc', padding: '1rem', marginBottom: '1rem' }}>
        <h4>Create 3-Scenario Plan Group</h4>
        <div>
          <label>Base Plan Name: </label>
          <input 
            type="text" 
            value={basePlanName} 
            onChange={(e) => setBasePlanName(e.target.value)} 
          />
        </div>
        <div>
          <label>Start Date: </label>
          <input 
            type="date" 
            value={startDate} 
            onChange={(e) => setStartDate(e.target.value)} 
          />
        </div>
        <div>
          <label>End Date: </label>
          <input 
            type="date" 
            value={endDate} 
            onChange={(e) => setEndDate(e.target.value)} 
          />
        </div>
        <div>
          <label>Starting Balance: </label>
          <input 
            type="number" 
            step="0.01"
            value={startBalance}
            onChange={(e) => setStartBalance(e.target.value)} 
          />
        </div>
        <button onClick={handleCreateScenarios}>
          Create 3 Plans (Worst/Realistic/Best)
        </button>
      </div>

      {message && <div style={{ color: 'red' }}>{message}</div>}

      {/* Existing Plans Table */}
      <h4>Existing Plans</h4>
      {plans.length === 0 ? (
        <p>No plans found.</p>
      ) : (
        <table border="1" cellPadding="8">
          <thead>
            <tr>
              <th>ID</th>
              <th>Plan Name</th>
              <th>Scenario</th>
              <th>Start Date</th>
              <th>End Date</th>
              <th>Start Balance</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {plans.map(plan => (
              <tr key={plan.id}>
                <td>{plan.id}</td>
                <td>{plan.planName}</td>
                <td>{plan.scenario || '—'}</td>
                <td>{plan.startDate}</td>
                <td>{plan.endDate}</td>
                <td>{plan.startBalance}</td>
                <td>
                  {/* Link to line items page */}
                  <button onClick={() => navigate(`/plans/${plan.id}`)}>
                    View Line Items
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
