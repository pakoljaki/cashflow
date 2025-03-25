import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

export default function PlanLineItemsPage() {
  const { planId } = useParams();
  const [lineItems, setLineItems] = useState([]);
  const [message, setMessage] = useState('');
  
  const [title, setTitle] = useState('');
  const [type, setType] = useState('ONE_TIME'); 
  const [amount, setAmount] = useState('');
  const [frequency, setFrequency] = useState('ONE_TIME'); 
  const [startWeek, setStartWeek] = useState('');
  const [endWeek, setEndWeek] = useState('');
  const [percentChange, setPercentChange] = useState('');
  const [transactionDate, setTransactionDate] = useState('');
  const [categoryId, setCategoryId] = useState('');

  const [categories, setCategories] = useState([]);

  useEffect(() => {
    fetchLineItems();
    fetchCategories();
  }, [planId]);

  const fetchLineItems = async () => {
    const token = localStorage.getItem('token');
    try {
      const resp = await fetch(`/api/cashflow-plans/${planId}/line-items`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt);
      }
      const data = await resp.json();
      setLineItems(data);
    } catch (error) {
      setMessage('Error fetching line items: ' + error.message);
    }
  };

  const fetchCategories = async () => {
    const token = localStorage.getItem('token');
    try {
      const resp = await fetch('/api/categories', {
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!resp.ok) throw new Error('Failed to load categories');
      const data = await resp.json();
      setCategories(data);
    } catch (err) {
      console.error('Error loading categories', err);
    }
  };

  const handleAddLineItem = async () => {
    const token = localStorage.getItem('token');
    if (!token) return;

    const requestBody = {
      title,
      type,
      amount,
      frequency,
      startWeek: startWeek ? parseInt(startWeek) : 0,
      endWeek: endWeek ? parseInt(endWeek) : 0,
      percentChange: percentChange ? parseFloat(percentChange) : 0,
      transactionDate: transactionDate || null,
      categoryId: categoryId ? parseInt(categoryId) : null
    };

    try {
      const resp = await fetch(`/api/cashflow-plans/${planId}/line-items`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
      });

      if (!resp.ok) {
        const txt = await resp.text();
        throw new Error(txt);
      }
      const newItem = await resp.json();
      setLineItems((old) => [...old, newItem]);
      setMessage('Line item added successfully!');
      setTitle('');
      setAmount('');
      setStartWeek('');
      setEndWeek('');
      setFrequency('ONE_TIME');
      setPercentChange('');
      setTransactionDate('');
      setCategoryId('');
      setType('ONE_TIME');
    } catch (error) {
      setMessage('Error adding line item: ' + error.message);
    }
  };

  const handleDeleteItem = async (itemId) => {
    const token = localStorage.getItem('token');
    if (!token) return;

    try {
      const resp = await fetch(`/api/cashflow-plans/${planId}/line-items/${itemId}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` }
      });
      if (resp.ok) {
        setLineItems((old) => old.filter(i => i.id !== itemId));
      } else {
        const txt = await resp.text();
        throw new Error(txt);
      }
    } catch (error) {
      setMessage('Error deleting line item: ' + error.message);
    }
  };

  return (
    <div style={{ padding: '1rem' }}>
      <h2>Plan #{planId} Line Items</h2>
      {message && <div style={{ color: 'red' }}>{message}</div>}

      {/* Existing Items Table */}
      <table border="1" cellPadding="6" style={{ marginTop: '1rem' }}>
        <thead>
          <tr>
            <th>ID</th>
            <th>Type</th>
            <th>Title</th>
            <th>Amount</th>
            <th>Frequency</th>
            <th>StartWeek</th>
            <th>EndWeek</th>
            <th>PercentChange</th>
            <th>TransactionDate</th>
            <th>Category</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {lineItems.map(item => (
            <tr key={item.id}>
              <td>{item.id}</td>
              <td>{item.type}</td>
              <td>{item.title}</td>
              <td>{item.amount}</td>
              <td>{item.frequency}</td>
              <td>{item.startWeek}</td>
              <td>{item.endWeek}</td>
              <td>{item.percentChange}</td>
              <td>{item.transactionDate || '—'}</td>
              <td>{item.category ? item.category.name : '—'}</td>
              <td>
                <button onClick={() => handleDeleteItem(item.id)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Form to Add New Line Item */}
      <div style={{ marginTop: '2rem', border: '1px solid #ccc', padding: '1rem' }}>
        <h4>Add New Line Item</h4>
        <div>
          <label>Type:</label>
          <select value={type} onChange={e => setType(e.target.value)}>
            <option value="ONE_TIME">ONE_TIME</option>
            <option value="RECURRING">RECURRING</option>
            <option value="CATEGORY_ADJUSTMENT">CATEGORY_ADJUSTMENT</option>
          </select>
        </div>
        <div>
          <label>Title:</label>
          <input 
            type="text" 
            value={title}
            onChange={e => setTitle(e.target.value)} 
          />
        </div>
        <div>
          <label>Amount:</label>
          <input 
            type="number" 
            step="0.01"
            value={amount}
            onChange={e => setAmount(e.target.value)} 
          />
        </div>
        {/* If type = RECURRING or other freq-based, show Frequency */}
        <div>
          <label>Frequency:</label>
          <select value={frequency} onChange={e => setFrequency(e.target.value)}>
            <option value="ONE_TIME">ONE_TIME</option>
            <option value="WEEKLY">WEEKLY</option>
            <option value="BI_WEEKLY">BI_WEEKLY</option>
            <option value="MONTHLY">MONTHLY</option>
            <option value="QUARTERLY">QUARTERLY</option>
            <option value="SEMI_ANNUAL">SEMI_ANNUAL</option>
            <option value="ANNUAL">ANNUAL</option>
          </select>
        </div>
        <div>
          <label>Start Week:</label>
          <input 
            type="number"
            value={startWeek}
            onChange={e => setStartWeek(e.target.value)}
          />
        </div>
        <div>
          <label>End Week:</label>
          <input 
            type="number"
            value={endWeek}
            onChange={e => setEndWeek(e.target.value)}
          />
        </div>
        {/* For category adjustments */}
        <div>
          <label>Category (Optional):</label>
          <select value={categoryId} onChange={e => setCategoryId(e.target.value)}>
            <option value="">No Category</option>
            {categories.map(cat => (
              <option key={cat.id} value={cat.id}>
                {cat.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label>Percent Change (for CATEGORY_ADJUSTMENT):</label>
          <input 
            type="number"
            step="0.01"
            value={percentChange}
            onChange={e => setPercentChange(e.target.value)}
          />
        </div>
        {/* For ONE_TIME transactions: */}
        <div>
          <label>Transaction Date:</label>
          <input 
            type="date"
            value={transactionDate}
            onChange={e => setTransactionDate(e.target.value)} 
          />
        </div>

        <button onClick={handleAddLineItem}>Add Line Item</button>
      </div>
    </div>
  );
}
