import React, { useState } from 'react';

//TODO: ADD CATEGORY HANDLING similar to one-time

export default function RecurringForm({ plans, onSuccess }) {
  const scenarios = ["WORST", "REALISTIC", "BEST"];
  const [scenarioData, setScenarioData] = useState(() => {
    const init = {};
    scenarios.forEach(s => {
      init[s] = { active: false, amount: '' };
    });
    return init;
  });

  const [title, setTitle] = useState('');
  const [frequency, setFrequency] = useState('MONTHLY');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [message, setMessage] = useState('');

  async function handleSubmit() {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }
    if (!title.trim()) {
      alert('Title is required');
      return;
    }

    let createdCount = 0;
    let sharedAssumptionId = null;

    for (let plan of plans) {
      const sc = plan.scenario;
      if (scenarioData[sc].active) {
        const amountVal = scenarioData[sc].amount || '0';

        const body = {
          title: title.trim(),
          type: 'RECURRING',
          amount: parseFloat(amountVal),
          frequency,
          transactionDate: null,
          startDate: startDate || null,
          endDate: endDate || null
        };

        if (sharedAssumptionId) {
          body.assumptionId = sharedAssumptionId;
        }

        try {
          const resp = await fetch(`/api/cashflow-plans/${plan.id}/line-items`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              Authorization: `Bearer ${token}`
            },
            body: JSON.stringify(body)
          });
          if (!resp.ok) {
            const txt = await resp.text();
            throw new Error(`Failed for ${sc}: ` + txt);
          }

          const savedItem = await resp.json();
          if (!sharedAssumptionId) {
            sharedAssumptionId = savedItem.assumptionId;
          }

          createdCount++;
        } catch (err) {
          setMessage(err.message);
          return;
        }
      }
    }

    setMessage(`Recurring assumption added for ${createdCount} scenario(s)!`);
    if (onSuccess) onSuccess();
  }

  function toggleScenarioActive(s) {
    setScenarioData(prev => ({
      ...prev,
      [s]: {
        ...prev[s],
        active: !prev[s].active,
      },
    }));
  }

  function handleAmountChange(s, val) {
    setScenarioData(prev => ({
      ...prev,
      [s]: {
        ...prev[s],
        amount: val,
      },
    }));
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <h5>Recurring Transaction (Multi-Scenario)</h5>
      {message && <p style={{ color: 'red' }}>{message}</p>}

      <div style={{ marginBottom: '0.5rem' }}>
        <label>Title: </label>
        <input
          style={{ marginLeft: '0.5rem' }}
          value={title}
          onChange={e => setTitle(e.target.value)}
        />
      </div>

      <div style={{ marginBottom: '0.5rem' }}>
        <label>Frequency: </label>
        <select
          style={{ marginLeft: '0.5rem' }}
          value={frequency}
          onChange={e => setFrequency(e.target.value)}
        >
          <option value="WEEKLY">WEEKLY</option>
          <option value="BI_WEEKLY">BI_WEEKLY</option>
          <option value="MONTHLY">MONTHLY</option>
          <option value="QUARTERLY">QUARTERLY</option>
          <option value="SEMI_ANNUAL">SEMI_ANNUAL</option>
          <option value="ANNUAL">ANNUAL</option>
        </select>
      </div>

      <div style={{ marginBottom: '0.5rem' }}>
        <label>Start Date (all scenarios): </label>
        <input
          type="date"
          style={{ marginLeft: '0.5rem' }}
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
        />
      </div>
      <div style={{ marginBottom: '1rem' }}>
        <label>End Date (all scenarios): </label>
        <input
          type="date"
          style={{ marginLeft: '0.5rem' }}
          value={endDate}
          onChange={e => setEndDate(e.target.value)}
        />
      </div>

      <table border="1" cellPadding="5" style={{ marginBottom: '1rem' }}>
        <thead>
          <tr>
            <th>Scenario</th>
            <th>Active?</th>
            <th>Amount</th>
          </tr>
        </thead>
        <tbody>
          {scenarios.map(s => (
            <tr key={s}>
              <td>{s}</td>
              <td>
                <input
                  type="checkbox"
                  checked={scenarioData[s].active}
                  onChange={() => toggleScenarioActive(s)}
                />
              </td>
              <td>
                <input
                  type="number"
                  step="0.01"
                  disabled={!scenarioData[s].active}
                  value={scenarioData[s].amount}
                  onChange={e => handleAmountChange(s, e.target.value)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <button onClick={handleSubmit}>Save</button>
    </div>
  );
}
