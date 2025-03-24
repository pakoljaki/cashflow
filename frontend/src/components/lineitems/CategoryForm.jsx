// src/components/lineitems/CategoryForm.jsx
import React, { useState } from 'react';

export default function CategoryForm({ plans, onSuccess }) {
  const [title, setTitle] = useState('');

  // For category adjustments, we might do a percent change for worst/real/best
  const [worstPercent, setWorstPercent] = useState('');
  const [realPercent, setRealPercent] = useState('');
  const [bestPercent, setBestPercent] = useState('');

  // Optional start/end date for each scenario
  const [worstStart, setWorstStart] = useState('');
  const [realStart, setRealStart] = useState('');
  const [bestStart, setBestStart] = useState('');
  const [worstEnd, setWorstEnd] = useState('');
  const [realEnd, setRealEnd] = useState('');
  const [bestEnd, setBestEnd] = useState('');

  const [categoryId, setCategoryId] = useState('');
  const [message, setMessage] = useState('');

  async function handleSubmit() {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }

    for (let plan of plans) {
      let percentVal = '';
      let startVal = '';
      let endVal = '';
      if (plan.scenario === 'WORST') {
        percentVal = worstPercent;
        startVal = worstStart;
        endVal = worstEnd;
      } else if (plan.scenario === 'REALISTIC') {
        percentVal = realPercent;
        startVal = realStart;
        endVal = realEnd;
      } else {
        percentVal = bestPercent;
        startVal = bestStart;
        endVal = bestEnd;
      }

      const body = {
        title,
        type: 'CATEGORY_ADJUSTMENT',
        percentChange: percentVal ? parseFloat(percentVal) : 0,
        categoryId: categoryId || null,
        startDate: startVal || null,
        endDate: endVal || null
      };

      const resp = await fetch(`/api/cashflow-plans/${plan.id}/line-items`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify(body)
      });
      if (!resp.ok) {
        setMessage(`Failed for ${plan.scenario}: ${await resp.text()}`);
        return;
      }
    }

    setMessage('Category Adjust assumption added for all 3 scenarios!');
    if (onSuccess) onSuccess();
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <h5>Category Adjustment</h5>
      {message && <p style={{ color: 'red' }}>{message}</p>}

      <div>
        <label>Title: </label>
        <input value={title} onChange={e => setTitle(e.target.value)} />
      </div>

      <div>
        <label>Category ID (optional): </label>
        <input 
          type="text" 
          placeholder="Category ID or blank"
          value={categoryId}
          onChange={e => setCategoryId(e.target.value)}
        />
      </div>

      <h6>Worst Case</h6>
      <div>
        <label>Percent Change: </label>
        <input 
          type="number" 
          step="0.01"
          value={worstPercent}
          onChange={e => setWorstPercent(e.target.value)}
        />
      </div>
      <div>
        <label>Start Date: </label>
        <input type="date" value={worstStart} onChange={e => setWorstStart(e.target.value)} />
      </div>
      <div>
        <label>End Date: </label>
        <input type="date" value={worstEnd} onChange={e => setWorstEnd(e.target.value)} />
      </div>

      <h6>Realistic</h6>
      <div>
        <label>Percent Change: </label>
        <input 
          type="number" 
          step="0.01"
          value={realPercent}
          onChange={e => setRealPercent(e.target.value)}
        />
      </div>
      <div>
        <label>Start Date: </label>
        <input type="date" value={realStart} onChange={e => setRealStart(e.target.value)} />
      </div>
      <div>
        <label>End Date: </label>
        <input type="date" value={realEnd} onChange={e => setRealEnd(e.target.value)} />
      </div>

      <h6>Best Case</h6>
      <div>
        <label>Percent Change: </label>
        <input 
          type="number"
          step="0.01"
          value={bestPercent}
          onChange={e => setBestPercent(e.target.value)}
        />
      </div>
      <div>
        <label>Start Date: </label>
        <input type="date" value={bestStart} onChange={e => setBestStart(e.target.value)} />
      </div>
      <div>
        <label>End Date: </label>
        <input type="date" value={bestEnd} onChange={e => setBestEnd(e.target.value)} />
      </div>

      <button onClick={handleSubmit}>Save</button>
    </div>
  );
}
