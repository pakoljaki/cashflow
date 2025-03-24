
import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

import OneTimeForm from '../components/lineitems/OneTimeForm';
import RecurringForm from '../components/lineitems/RecurringForm';
import CategoryForm from '../components/lineitems/CategoryForm';


export default function ScenarioGroupLineItemsPage() {
  const { groupKey } = useParams();
  const [plans, setPlans] = useState([]);       // The 3 plans
  const [selectedForm, setSelectedForm] = useState(null);
  const [message, setMessage] = useState('');

  useEffect(() => {
    fetchPlans();
  }, [groupKey]);

  async function fetchPlans() {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in.');
      return;
    }
    try {
      const resp = await fetch(`/api/cashflow-plans/group/${groupKey}/plans`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt);
      }
      const data = await resp.json();
      setPlans(data);
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  }

  function getMergedLineItems() {
    const items = [];
    for (let plan of plans) {
      for (let item of plan.lineItems) {
        items.push({
          planId: plan.id,
          scenario: plan.scenario,
          ...item
        });
      }
    }
    return items;
  }

  const mergedItems = getMergedLineItems();

  return (
    <div style={{ display: 'flex', padding: '1rem' }}>
      <div style={{ flex: 3, marginRight: '1rem' }}>
        <h2>Scenario Group: {groupKey}</h2>
        {message && <div style={{ color: 'red' }}>{message}</div>}

        <p>We have {plans.length} plans in this group. (Should be 3)</p>

        <table border="1" cellPadding="6">
          <thead>
            <tr>
              <th>Scenario</th>
              <th>Title</th>
              <th>Type</th>
              <th>Category</th>
              <th>Amount</th>
              <th>TransactionDate</th>
              <th>PercentChange</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {mergedItems.map((item) => (
              <tr key={`${item.planId}-${item.id}`}>
                <td>{item.scenario}</td>
                <td>{item.title}</td>
                <td>{item.type}</td>
                <td>{item.category ? item.category.name : '—'}</td>
                <td>{item.amount}</td>
                <td>{item.transactionDate || '—'}</td>
                <td>{item.percentChange || '—'}</td>
                <td>
                  <button onClick={() => handleDeleteItem(item.planId, item.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Right side: add assumption forms */}
      <div style={{ flex: 2, border: '1px solid #ccc', padding: '1rem' }}>
        <h3>Add Assumption</h3>
        <p>Select assumption type:</p>
        <button onClick={() => setSelectedForm('ONE_TIME')}>One-Time</button>
        <button onClick={() => setSelectedForm('RECURRING')}>Recurring</button>
        <button onClick={() => setSelectedForm('CATEGORY')}>Category Adjust</button>

        {selectedForm === 'ONE_TIME' && (
          <OneTimeForm 
            plans={plans} 
            onSuccess={() => {
              fetchPlans();
              setSelectedForm(null);
            }} 
          />
        )}
        {selectedForm === 'RECURRING' && (
          <RecurringForm 
            plans={plans} 
            onSuccess={() => {
              fetchPlans();
              setSelectedForm(null);
            }} 
          />
        )}
        {selectedForm === 'CATEGORY' && (
          <CategoryForm 
            plans={plans} 
            onSuccess={() => {
              fetchPlans();
              setSelectedForm(null);
            }} 
          />
        )}
      </div>
    </div>
  );

  async function handleDeleteItem(planId, itemId) {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const resp = await fetch(`/api/cashflow-plans/${planId}/line-items/${itemId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!resp.ok) {
        throw new Error(await resp.text());
      }
      fetchPlans();
    } catch (err) {
      setMessage('Error deleting item: ' + err.message);
    }
  }
}
