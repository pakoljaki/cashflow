import React, { useState } from 'react';

export default function OneTimeForm({ plans, onSuccess }) {
  // For each scenario, separate fields
  const [worstAmount, setWorstAmount] = useState('');
  const [realAmount, setRealAmount] = useState('');
  const [bestAmount, setBestAmount] = useState('');
  const [worstDate, setWorstDate] = useState('');
  const [realDate, setRealDate] = useState('');
  const [bestDate, setBestDate] = useState('');
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');

  async function handleSubmit() {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }

    // We'll loop over each plan, and if plan.scenario === "WORST", we use worstAmount/worstDate, etc.
    for (let plan of plans) {
      let amountVal = '';
      let dateVal = '';
      if (plan.scenario === 'WORST') {
        amountVal = worstAmount;
        dateVal = worstDate;
      } else if (plan.scenario === 'REALISTIC') {
        amountVal = realAmount;
        dateVal = realDate;
      } else {
        amountVal = bestAmount;
        dateVal = bestDate;
      }

      const body = {
        title,
        type: 'ONE_TIME',
        amount: amountVal,
        transactionDate: dateVal,
        // etc. default freq is ONE_TIME
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
        setMessage(`Failed to add item to ${plan.scenario} plan: ${await resp.text()}`);
        return;
      }
    }
    setMessage('Added One-Time assumption for all 3 scenarios!');
    if (onSuccess) onSuccess();
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <h5>One-Time Transaction</h5>
      {message && <p style={{ color: 'red' }}>{message}</p>}

      <div>
        <label>Title: </label>
        <input value={title} onChange={e => setTitle(e.target.value)} />
      </div>

      <h6>Worst Case</h6>
      <div>
        <label>Amount: </label>
        <input type="number" value={worstAmount} onChange={e => setWorstAmount(e.target.value)} />
      </div>
      <div>
        <label>Date: </label>
        <input type="date" value={worstDate} onChange={e => setWorstDate(e.target.value)} />
      </div>

      <h6>Realistic</h6>
      <div>
        <label>Amount: </label>
        <input type="number" value={realAmount} onChange={e => setRealAmount(e.target.value)} />
      </div>
      <div>
        <label>Date: </label>
        <input type="date" value={realDate} onChange={e => setRealDate(e.target.value)} />
      </div>

      <h6>Best Case</h6>
      <div>
        <label>Amount: </label>
        <input type="number" value={bestAmount} onChange={e => setBestAmount(e.target.value)} />
      </div>
      <div>
        <label>Date: </label>
        <input type="date" value={bestDate} onChange={e => setBestDate(e.target.value)} />
      </div>

      <button onClick={handleSubmit}>Save</button>
    </div>
  );
}
