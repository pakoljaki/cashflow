import React, { useState, useEffect } from 'react';

export default function CategoryForm({ plans, onSuccess }) {
  const scenarios = ["WORST", "REALISTIC", "BEST"];
  const [scenarioData, setScenarioData] = useState(() => {
    const init = {};
    scenarios.forEach(s => {
      init[s] = { active: false, percent: '' };
    });
    return init;
  });

  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  const [categories, setCategories] = useState([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [showNewCategoryForm, setShowNewCategoryForm] = useState(false);
  const [newCatName, setNewCatName] = useState('');
  const [newCatDirection, setNewCatDirection] = useState('NEGATIVE');

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
      setNewCatDirection('NEGATIVE');
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

  function handlePercentChange(s, val) {
    setScenarioData(prev => ({
      ...prev,
      [s]: {
        ...prev[s],
        percent: val,
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

    let createdCount = 0;
    let sharedAssumptionId = null;

    for (let plan of plans) {
      const sc = plan.scenario;
      if (scenarioData[sc].active) {
        const pc = scenarioData[sc].percent || '0';
        const body = {
          title: title.trim(),
          type: 'CATEGORY_ADJUSTMENT',
          percentChange: parseFloat(pc),
          categoryId: selectedCategoryId ? parseInt(selectedCategoryId) : null,
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
            throw new Error(`Failed for ${plan.scenario}: ` + txt);
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

    setMessage(`Category adjustment added for ${createdCount} scenario(s)!`);
    if (onSuccess) onSuccess();
  }

  return (
    <div style={{ marginTop: '1rem' }}>
      <h5>Category Adjustment (Multi-Scenario)</h5>
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
        <label>Start Date (all scenarios): </label>
        <input
          type="date"
          style={{ marginLeft: '0.5rem' }}
          value={startDate}
          onChange={e => setStartDate(e.target.value)}
        />
      </div>

      <div style={{ marginBottom: '0.5rem' }}>
        <label>End Date (all scenarios): </label>
        <input
          type="date"
          style={{ marginLeft: '0.5rem' }}
          value={endDate}
          onChange={e => setEndDate(e.target.value)}
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
              <option value="POSITIVE">POSITIVE</option>
              <option value="NEGATIVE">NEGATIVE</option>
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
            <th>% Change</th>
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
                  value={scenarioData[s].percent}
                  onChange={e => handlePercentChange(s, e.target.value)}
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
