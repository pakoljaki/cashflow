import React, { useEffect, useState } from 'react';
import { Table, Button } from 'react-bootstrap';
import '../styles/transactions.css';

export default function TransactionsPage() {
  const [transactions, setTransactions] = useState([]);
  const [selectedTransactionIds, setSelectedTransactionIds] = useState([]);
  const [selectedDirection, setSelectedDirection] = useState(null); 

  const [categories, setCategories] = useState([]);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    fetch('/api/transactions', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => {
        console.log("Transactions received:", data); 
        setTransactions(data);
      })
      .catch(err => console.error('Error loading transactions', err));
  }, []);
  
  useEffect(() => {
    const token = localStorage.getItem('token');
    fetch('/api/categories', {
      headers: { Authorization: `Bearer ${token}` }
    })
      .then(res => res.json())
      .then(data => setCategories(data))
      .catch(err => console.error('Error loading categories', err));
  }, []);

  const handleRowClick = (tx) => {
    if (selectedTransactionIds.includes(tx.id)) {
      const newIds = selectedTransactionIds.filter(id => id !== tx.id);
      setSelectedTransactionIds(newIds);

      if (newIds.length === 0) {
        setSelectedDirection(null);
      }
      return;
    }

    // if nothing selected yet, adopt row direction
    if (!selectedDirection) {
      setSelectedDirection(tx.transactionDirection);
      setSelectedTransactionIds([...selectedTransactionIds, tx.id]);
    } else {
      // if direction matches, add row. otherwise ignore
      if (selectedDirection === tx.transactionDirection) {
        setSelectedTransactionIds([...selectedTransactionIds, tx.id]);
      } else {
        alert("You cannot mix POSITIVE and NEGATIVE transactions for categorization.");
      }
    }
  };

  // return a filtered list of categories for the selected direction
  const positiveCategories = categories.filter(cat => cat.direction === "POSITIVE");
  const negativeCategories = categories.filter(cat => cat.direction === "NEGATIVE");

  // if user clicks a category button -> bulk update
  const handleCategoryClick = async (catId) => {
    if (selectedTransactionIds.length === 0) {
      alert("No transactions selected.");
      return;
    }
    const token = localStorage.getItem('token');
    try {
      const resp = await fetch('/api/transactions/bulk-category', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          transactionIds: selectedTransactionIds,
          categoryId: catId
        })
      });
      if (!resp.ok) {
        const errTxt = await resp.text();
        throw new Error(errTxt);
      }
      setMessage("Category updated successfully!");
      // refresh
      refreshTransactions();
      // clear selection
      setSelectedTransactionIds([]);
      setSelectedDirection(null);
    } catch (err) {
      console.error(err);
      setMessage("Error updating category: " + err.message);
    }
  };

  // create new category for the selected direction
  const handleCreateCategory = async () => {
    if (!selectedDirection) {
      alert("Select at least one transaction first (to define the direction), or pick a direction.");
      return;
    }
    if (!newCategoryName.trim()) {
      alert("Please enter a category name");
      return;
    }
    const token = localStorage.getItem('token');
    try {
      const resp = await fetch('/api/categories', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          name: newCategoryName.trim(),
          direction: selectedDirection
        })
      });
      if (!resp.ok) {
        const errTxt = await resp.text();
        throw new Error(errTxt);
      }
      setMessage("New category created!");
      setNewCategoryName('');
      // reload categories
      const data = await resp.json(); 
      setCategories(old => [...old, data]);
    } catch (err) {
      console.error(err);
      setMessage("Error creating category: " + err.message);
    }
  };

  const refreshTransactions = async () => {
    const token = localStorage.getItem('token');
    try {
        const res = await fetch('/api/transactions', {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (!res.ok) throw new Error('Failed to fetch transactions');
        
        const data = await res.json();
        setTransactions(data);
    } catch (err) {
        console.error('Error refreshing transactions:', err);
    }
};


  return (
    <div className="transactions-page">
      {/* LEFT: table */}
      <div className="transactions-container">
        <h2>All Transactions</h2>
        <Table bordered hover responsive className="transaction-table">
          <thead>
            <tr style={{ backgroundColor: '#007bff', color: 'white' }}>
              <th>ID</th>
              <th>Date</th>
              <th>Amount</th>
              <th>Currency</th>
              <th>Partner</th>
              <th>Account</th>
              <th>Memo</th>
              <th>Category</th>
            </tr>
          </thead>
          <tbody>
            {transactions.map(tx => {
              // highlight row if selected
              const isSelected = selectedTransactionIds.includes(tx.id);
              const rowClass = isSelected ? 'selected-row' : '';

              // show + or - with color
              const isPos = tx.transactionDirection === "POSITIVE";
              const sign = isPos ? "+" : "–";
              const amountClass = isPos ? "positive" : "negative";

              return (
                <tr key={tx.id} className={rowClass} onClick={() => handleRowClick(tx)}>
                  <td>{tx.id}</td>
                  <td>{tx.bookingDate}</td>
                  <td className={amountClass}>{sign}{tx.amount}</td>
                  <td>{tx.currency}</td>
                  <td>{tx.partnerName || "N/A"}</td>
                  <td>{tx.partnerAccountNumber || "N/A"}</td>
                  <td>{tx.memo || "N/A"}</td>
                  <td>{tx.category ? tx.category.name : "Uncategorized"}</td>
                </tr>
              );
            })}
          </tbody>
        </Table>
        {message && <div className="info-message">{message}</div>}
      </div>

      {/* RIGHT: Category Panel */}
      <div className="category-panel">
        <h4>Categories</h4>
        {selectedDirection === "POSITIVE" && (
          <>
            <p style={{ fontStyle: 'italic', color: '#28a745' }}>
              Currently selecting POSITIVE transactions
            </p>
            {positiveCategories.map(cat => (
              <div
                key={cat.id}
                className="category-button"
                onClick={() => handleCategoryClick(cat.id)}
              >
                {cat.name}
              </div>
            ))}
          </>
        )}
        {selectedDirection === "NEGATIVE" && (
          <>
            <p style={{ fontStyle: 'italic', color: '#dc3545' }}>
              Currently selecting NEGATIVE transactions
            </p>
            {negativeCategories.map(cat => (
              <div
                key={cat.id}
                className="category-button"
                onClick={() => handleCategoryClick(cat.id)}
              >
                {cat.name}
              </div>
            ))}
          </>
        )}
        {!selectedDirection && (
          <p style={{ color: '#666', fontSize: '0.9rem' }}>
            Select some transactions to define a direction.
          </p>
        )}

        {/* Add new category for current direction */}
        <div className="new-category">
          <h5>Add New Category</h5>
          <input
            type="text"
            placeholder="Category name..."
            value={newCategoryName}
            onChange={e => setNewCategoryName(e.target.value)}
          />
          <Button variant="outline-primary" onClick={handleCreateCategory}>
            Create Category
          </Button>
        </div>
      </div>
    </div>
  );
}
