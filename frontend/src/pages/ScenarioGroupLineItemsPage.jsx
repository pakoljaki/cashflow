import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import OneTimeForm from '../components/lineitems/OneTimeForm';
import RecurringForm from '../components/lineitems/RecurringForm';
import CategoryForm from '../components/lineitems/CategoryForm';
import CashflowChart from '../components/charts/CashflowChart';

export default function ScenarioGroupLineItemsPage() {
  const { groupKey } = useParams();
  const [plans, setPlans] = useState([]);
  const [message, setMessage] = useState('');
  const [selectedForm, setSelectedForm] = useState(null);
  const [monthlyData, setMonthlyData] = useState([]);

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

  // After plans load, fetch monthly KPI for the REALISTIC plan
  useEffect(() => {
    if (plans.length === 0) return;
    const realistic = plans.find(p => p.scenario === 'REALISTIC');
    if (!realistic) return;
    const token = localStorage.getItem('token');
    fetch(`/api/cashflow-plans/${realistic.id}/monthly-kpi`, {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(r => r.json())
      .then(data => setMonthlyData(data))
      .catch(err => console.error(err));
  }, [plans]);

  function getAllLineItems() {
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

  function groupByAssumption(items) {
    const map = new Map();
    let fallbackCounter = 100000;
    for (let it of items) {
      let key = it.assumptionId || `missing-${fallbackCounter++}`;
      if (!map.has(key)) {
        map.set(key, { assumptionId: it.assumptionId, worst: null, realistic: null, best: null });
      }
      const grp = map.get(key);
      if (it.scenario === 'WORST') grp.worst = it;
      if (it.scenario === 'REALISTIC') grp.realistic = it;
      if (it.scenario === 'BEST') grp.best = it;
    }
    return Array.from(map.values());
  }

  const allItems = getAllLineItems();
  const groupedAssumptions = groupByAssumption(allItems);

  async function handleDeleteItem(item) {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const resp = await fetch(
        `/api/cashflow-plans/${item.planId}/line-items/${item.id}`,
        { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } }
      );
      if (!resp.ok) throw new Error(await resp.text());
      fetchPlans();
    } catch (err) {
      setMessage('Error deleting item: ' + err.message);
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', padding: '1rem', height: '100%' }}>
      {/* Top: line‐items table & forms */}
      <div style={{ display: 'flex', flex: 1, marginBottom: '1rem' }}>
        <div style={{ flex: 3, marginRight: '1rem' }}>
          <h2>Scenario Group: {groupKey}</h2>
          {message && <div style={{ color: 'red' }}>{message}</div>}
          <p>We have {plans.length} plans in this group.</p>
          <table border="1" cellPadding="6" width="100%" style={{ marginBottom: '1rem' }}>
            <thead>
              <tr>
                <th>Assumption ID</th>
                <th>Worst</th>
                <th>Realistic</th>
                <th>Best</th>
              </tr>
            </thead>
            <tbody>
              {groupedAssumptions.map(grp => (
                <tr key={grp.assumptionId || Math.random()}>
                  <td>{grp.assumptionId || 'N/A'}</td>
                  <td>
                    {grp.worst
                      ? <ItemCell item={grp.worst} onDelete={() => handleDeleteItem(grp.worst)} />
                      : <em style={{ color: '#aaa' }}>— none —</em>}
                  </td>
                  <td>
                    {grp.realistic
                      ? <ItemCell item={grp.realistic} onDelete={() => handleDeleteItem(grp.realistic)} />
                      : <em style={{ color: '#aaa' }}>— none —</em>}
                  </td>
                  <td>
                    {grp.best
                      ? <ItemCell item={grp.best} onDelete={() => handleDeleteItem(grp.best)} />
                      : <em style={{ color: '#aaa' }}>— none —</em>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div style={{ flex: 2, border: '1px solid #ccc', padding: '1rem' }}>
          <h3>Add Assumption</h3>
          <p>Select assumption type:</p>
          <button onClick={() => setSelectedForm('ONE_TIME')} style={{ marginRight: '0.5rem' }}>One-Time</button>
          <button onClick={() => setSelectedForm('RECURRING')} style={{ marginRight: '0.5rem' }}>Recurring</button>
          <button onClick={() => setSelectedForm('CATEGORY')}>Category Adjust</button>
          <div style={{ marginTop: '1rem' }}>
            {selectedForm === 'ONE_TIME' && (
              <OneTimeForm plans={plans} onSuccess={() => { fetchPlans(); setSelectedForm(null); }} />
            )}
            {selectedForm === 'RECURRING' && (
              <RecurringForm plans={plans} onSuccess={() => { fetchPlans(); setSelectedForm(null); }} />
            )}
            {selectedForm === 'CATEGORY' && (
              <CategoryForm plans={plans} onSuccess={() => { fetchPlans(); setSelectedForm(null); }} />
            )}
          </div>
        </div>
      </div>

      {/* Bottom: interactive cashflow chart */}
      <div style={{ flex: 1, borderTop: '1px solid #ddd', paddingTop: '1rem' }}>
        <h3>Cashflow Forecast</h3>
        <CashflowChart monthlyData={monthlyData} />
      </div>
    </div>
  );
}

function ItemCell({ item, onDelete }) {
  return (
    <div style={{ border: '1px solid #ccc', padding: '4px', marginBottom: '4px' }}>
      <strong>{item.title}</strong> [{item.type}]
      <br />
      {item.amount != null && <span>Amt: {item.amount}</span>}
      {item.percentChange != null && (
        <div>± {(item.percentChange * 100).toFixed(2)}%</div>
      )}
      <button onClick={onDelete} style={{ marginTop: '4px' }}>Delete</button>
    </div>
  );
}
