// src/components/lineitems/RecurringForm.jsx
import React, { useState } from 'react';

export default function RecurringForm({ plans, onSuccess }) {
  const [worstAmount, setWorstAmount] = useState('');
  const [realAmount, setRealAmount] = useState('');
  const [bestAmount, setBestAmount] = useState('');

  const [worstStart, setWorstStart] = useState('');
  const [realStart, setRealStart] = useState('');
  const [bestStart, setBestStart] = useState('');

  const [worstEnd, setWorstEnd] = useState('');
  const [realEnd, setRealEnd] = useState('');
  const [bestEnd, setBestEnd] = useState('');

  const [frequency, setFrequency] = useState('MONTHLY');
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');

  async function handleSubmit() {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }

    for (let plan of plans) {
      let amountVal = '';
      let startVal = '';
      let endVal = '';
      if (plan.scenario === 'WORST') {
        amountVal = worstAmount;
        startVal = worstStart;
        endVal = worstEnd;
      } else if (plan.scenario === 'REALISTIC') {
        amountVal = realAmount;
        startVal = realStart;
        endVal = realEnd;
      } else {
        amountVal = bestAmount;
        startVal = bestStart;
        endVal = bestEnd;
      }

      const body = {
        title,
        type: 'RECURRING',
        amount: amountVal,
        frequency,
        // If your backend expects localDates, you might rename them or store them in transactionDate, etc.
        transactionDate: null,
        // Or if you store them in startDate/endDate, adapt to your PlanLineItem entity
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

    setMessage('Recurring assumption added for all 3 scenarios!');
    if (onSuccess) onSuccess();
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <h5>Recurring Transaction</h5>
      {message && <p style={{ color: 'red' }}>{message}</p>}

      <div>
        <label>Title: </label>
        <input value={title} onChange={e => setTitle(e.target.value)} />
      </div>

      <div>
        <label>Frequency:</label>
        <select value={frequency} onChange={e => setFrequency(e.target.value)}>
          <option value="WEEKLY">WEEKLY</option>
          <option value="BI_WEEKLY">BI_WEEKLY</option>
          <option value="MONTHLY">MONTHLY</option>
          <option value="QUARTERLY">QUARTERLY</option>
          <option value="SEMI_ANNUAL">SEMI_ANNUAL</option>
          <option value="ANNUAL">ANNUAL</option>
        </select>
      </div>

      <h6>Worst Case</h6>
      <div>
        <label>Amount: </label>
        <input type="number" value={worstAmount} onChange={e => setWorstAmount(e.target.value)} />
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
        <label>Amount: </label>
        <input type="number" value={realAmount} onChange={e => setRealAmount(e.target.value)} />
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
        <label>Amount: </label>
        <input type="number" value={bestAmount} onChange={e => setBestAmount(e.target.value)} />
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
