import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

export default function CashflowPlansPage() {
  const navigate = useNavigate();
  const [allPlans, setAllPlans] = useState([]);
  const [groupedPlans, setGroupedPlans] = useState([]); 
  const [basePlanName, setBasePlanName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [startBalance, setStartBalance] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetchAllPlans();
  }, []);

  async function fetchAllPlans() {
    const token = localStorage.getItem("token");
    if (!token) {
      setMessage('Not logged in');
      return;
    }
    try {
      const resp = await fetch("http://localhost:8080/api/cashflow-plans", {
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${token}`
        },
      });
      if (!resp.ok) {
        throw new Error('Failed to fetch plans');
      }
      const data = await resp.json();
      setAllPlans(data);
      groupByScenario(data);
    } catch (error) {
      console.error("Error fetching plans:", error);
      setMessage(error.message);
    }
  }

    function groupByScenario(plansArray) {
    const map = new Map();
    for (let plan of plansArray) {
      if (!map.has(plan.groupKey)) {
        map.set(plan.groupKey, []);
      }
      map.get(plan.groupKey).push(plan);
    }

       const result = [];
    for (let [groupKey, plansInGroup] of map.entries()) {
      let displayedPlan = plansInGroup.find(p => p.scenario === 'REALISTIC');
      if (!displayedPlan) {
        displayedPlan = plansInGroup[0]; 
      }
      result.push(displayedPlan);
    }
    setGroupedPlans(result);
  }

  async function handleCreateScenarios() {
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
      startBalance: startBalance || '0',
    };

    try {
      const resp = await fetch('/api/cashflow-plans/scenarios', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(bodyData),
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt);
      }

      const newGroupOfPlans = await resp.json();
      setMessage('Created 3 scenario plans successfully!');

      const updated = [...allPlans, ...newGroupOfPlans];
      setAllPlans(updated);
      groupByScenario(updated);

      setBasePlanName('');
      setStartDate('');
      setEndDate('');
      setStartBalance('');
    } catch (error) {
      setMessage('Error creating scenario plans: ' + error.message);
    }
  }

  return (
    <div style={{ display: 'flex', padding: '1rem' }}>
      {/* LEFT SIDE: Table of scenario groups */}
      <div style={{ flex: 2, marginRight: '1rem' }}>
        <h2>Cashflow Plans (Grouped by Scenario)</h2>
        {message && <div style={{ color: 'red' }}>{message}</div>}

        {groupedPlans.length === 0 ? (
          <p>No plans found.</p>
        ) : (
          <table border="1" cellPadding="8" style={{ width: '100%' }}>
            <thead>
              <tr>
                <th>ID</th>
                <th>Plan Name</th>
                {/* We intentionally skip showing scenario */}
                <th>Start Date</th>
                <th>End Date</th>
                <th>Start Balance</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {groupedPlans.map(plan => (
                <tr key={plan.id}>
                  <td>{plan.id}</td>
                  <td>{plan.planName}</td>
                  <td>{plan.startDate}</td>
                  <td>{plan.endDate}</td>
                  <td>{plan.startBalance}</td>
                  <td>
                    {/* 
                       If you still want to see line items for that 
                       specific scenario plan, you can keep a button: 
                       <button onClick={() => navigate(`/plans/${plan.id}`)}>View Items</button>
                    */}
                    <button onClick={() => navigate(`/scenario-group/${plan.groupKey}`)}>
                      Manage Group
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* RIGHT SIDE: Create new 3-scenario plan group */}
      <div style={{ flex: 1, border: '1px solid #ccc', padding: '1rem' }}>
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
        <button onClick={handleCreateScenarios}>Create Plans (Worst/Realistic/Best)</button>
      </div>
    </div>
  );
}
