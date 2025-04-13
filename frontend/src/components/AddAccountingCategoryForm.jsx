import React, { useState } from 'react';
import MyButton from './MyButton';

export default function AddAccountingCategoryForm({ onNewCategory }) {
  const [code, setCode] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [description, setDescription] = useState('');
  const [message, setMessage] = useState('');

  const handleSubmit = async e => {
    e.preventDefault();
    const token = localStorage.getItem('token');
    const payload = { code, displayName, description };
    try {
      const resp = await fetch('/api/accounting-categories', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
        body: JSON.stringify(payload)
      });
      if (!resp.ok) throw new Error(await resp.text());
      const newCategory = await resp.json();
      onNewCategory(newCategory);
      setCode('');
      setDisplayName('');
      setDescription('');
      setMessage('New accounting category added.');
    } catch (err) {
      setMessage('Error: ' + err.message);
    }
  };

  return (
    <div className="add-accounting-form">
      <h3>Add Accounting Category</h3>
      {message && <div className="form-message">{message}</div>}
      <form onSubmit={handleSubmit}>
        <div>
          <label>Code: </label>
          <input type="text" value={code} onChange={e => setCode(e.target.value)} required />
        </div>
        <div>
          <label>Display Name: </label>
          <input type="text" value={displayName} onChange={e => setDisplayName(e.target.value)} required />
        </div>
        <div>
          <label>Description: </label>
          <input type="text" value={description} onChange={e => setDescription(e.target.value)} />
        </div>
        <div style={{ marginTop: '0.5rem' }}>
          <MyButton type="submit">Add Category</MyButton>
        </div>
      </form>
    </div>
  );
}
