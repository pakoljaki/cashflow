import React, { useState, useEffect } from 'react';

export default function OneTimeForm({ plans, onSuccess }) {
  const scenarios = ["WORST", "REALISTIC", "BEST"];
  const [scenarioData, setScenarioData] = useState(() => {
    const init = {};
    scenarios.forEach(s => {
      init[s] = { active: false, amount: '' };
    });
    return init;
  });

  const [title, setTitle] = useState('');
  const [transactionDate, setTransactionDate] = useState('');
  const [message, setMessage] = useState('');

  const [categories, setCategories] = useState([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [showNewCategoryForm, setShowNewCategoryForm] = useState(false);
  const [newCatName, setNewCatName] = useState('');
  const [newCatDirection, setNewCatDirection] = useState('POSITIVE');

  useEffect(() => {
    fetchCategories();
  }, []);

  async function fetchCategories() {
    const token = localStorage.getItem('token');
    if (!token) return;
    try {
      const resp = await fetch('/api/categories', {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!resp.ok) throw new Error('Failed to load categories');
      const data = await resp.json();
      setCategories(data);
    } catch (err) {
      console.error('Error fetching categories:', err);
    }
  }

  async function handleCreateCategory() {
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Not logged in');
      return;
    }
    if (!newCatName.trim()) {
      alert('Category name cannot be empty');
      return;
    }
    try {
      const resp = await fetch('/api/categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          name: newCatName.trim(),
          direction: newCatDirection
        })
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt);
      }
      const createdCat = await resp.json();
      setMessage(`Created new category: ${createdCat.name}`);
      setShowNewCategoryForm(false);
      fetchCategories();
      setSelectedCategoryId(createdCat.id);
      setNewCatName('');
      setNewCatDirection('POSITIVE');
    } catch (err) {
      setMessage('Error creating category: ' + err.message);
    }
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
    if (!transactionDate) {
      alert('Please select a date for this one-time assumption');
      return;
    }

    let createdCount = 0;
    let sharedAssumptionId = null;

    for (let plan of plans) {
      const sc = plan.scenario;
      if (scenarioData[sc].active) {
        const body = {
          title: title.trim(),
          type: 'ONE_TIME',
          amount: scenarioData[sc].amount ? parseFloat(scenarioData[sc].amount) : 0,
          transactionDate,
          categoryId: selectedCategoryId ? parseInt(selectedCategoryId) : null
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
            const errTxt = await resp.text();
            throw new Error(`Scenario ${sc} failed: ${errTxt}`);
          }
          const savedItem = await resp.json();
          if (!sharedAssumptionId) {
            sharedAssumptionId = savedItem.assumptionId;
          }
          createdCount++;
        } catch (err) {
          setMessage(`Error creating item for ${sc}: ` + err.message);
          return;
        }
      }
    }

    setMessage(`Created items for ${createdCount} scenario(s).`);
    if (onSuccess) onSuccess();
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <h4>One-Time Assumption (Multi-Scenario)</h4>
      {message && <div style={{ color: 'red', marginBottom: '0.5rem' }}>{message}</div>}

      <div style={{ marginBottom: '0.5rem' }}>
        <label>Title: </label>
        <input
          style={{ marginLeft: '0.5rem' }}
          value={title}
          onChange={e => setTitle(e.target.value)}
        />
      </div>

      <div style={{ marginBottom: '0.5rem' }}>
        <label>Date (applies to all scenarios): </label>
        <input
          type="date"
          style={{ marginLeft: '0.5rem' }}
          value={transactionDate}
          onChange={e => setTransactionDate(e.target.value)}
        />
      </div>

      <div style={{ marginBottom: '0.5rem' }}>
        <label>Category: </label>
        <select
          style={{ marginLeft: '0.5rem' }}
          value={selectedCategoryId}
          onChange={e => setSelectedCategoryId(e.target.value)}
        >
          <option value="">-- None --</option>
          {categories.map(cat => (
            <option key={cat.id} value={cat.id}>
              {cat.name} ({cat.direction})
            </option>
          ))}
        </select>
        <button
          style={{ marginLeft: '0.5rem' }}
          onClick={() => setShowNewCategoryForm(!showNewCategoryForm)}
        >
          {showNewCategoryForm ? 'Cancel' : 'New Category'}
        </button>
      </div>

      {showNewCategoryForm && (
        <div style={{ border: '1px dashed #999', padding: '0.5rem', marginBottom: '0.5rem' }}>
          <h5>Create a New Category</h5>
          <div>
            <label>Name: </label>
            <input
              style={{ marginLeft: '0.5rem' }}
              value={newCatName}
              onChange={e => setNewCatName(e.target.value)}
            />
          </div>
          <div style={{ marginTop: '0.5rem' }}>
            <label>Direction: </label>
            <select
              style={{ marginLeft: '0.5rem' }}
              value={newCatDirection}
              onChange={e => setNewCatDirection(e.target.value)}
            >
              <option value="POSITIVE">POSITIVE (Income)</option>
              <option value="NEGATIVE">NEGATIVE (Expense)</option>
            </select>
          </div>
          <button style={{ marginTop: '0.5rem' }} onClick={handleCreateCategory}>
            Save Category
          </button>
        </div>
      )}

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
