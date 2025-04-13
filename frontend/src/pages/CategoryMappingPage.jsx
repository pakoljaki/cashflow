import React, { useState, useEffect } from 'react';
import { DragDropContext} from '@hello-pangea/dnd';
import AccountingCategoryColumn from '../components/AccountingCategoryColumn';
import UnassignedCategoriesColumn from '../components/UnassignedCategoriesColumn';
import AddAccountingCategoryForm from '../components/AddAccountingCategoryForm';
import MyButton from '../components/MyButton';
import '../styles/CategoryMappingPage.css';

export default function CategoryMappingPage() {
  const [accountingCategories, setAccountingCategories] = useState([]);
  const [transactionCategories, setTransactionCategories] = useState([]);
  const [mapping, setMapping] = useState({});
  const [message, setMessage] = useState('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    Promise.all([
      fetch('/api/accounting-categories', { headers: { Authorization: `Bearer ${token}` } }).then(res => res.json()),
      fetch('/api/categories', { headers: { Authorization: `Bearer ${token}` } }).then(res => res.json())
    ])
      .then(([acData, tcData]) => {
        setAccountingCategories(acData);
        setTransactionCategories(tcData);
        const initialMapping = {};
        tcData.forEach(tc => {
          const key = tc.accountingCategory ? tc.accountingCategory.id : 'unassigned';
          if (!initialMapping[key]) initialMapping[key] = [];
          initialMapping[key].push(tc);
        });
        if (!initialMapping['unassigned']) initialMapping['unassigned'] = [];
        acData.forEach(ac => {
          if (!initialMapping[ac.id]) initialMapping[ac.id] = [];
        });
        setMapping(initialMapping);
      })
      .catch(err => console.error(err));
  }, []);

  const onDragEnd = result => {
    if (!result.destination) return;
    const { source, destination } = result;
    if (source.droppableId === destination.droppableId && source.index === destination.index)
      return;
    const sourceList = Array.from(mapping[source.droppableId] || []);
    const destinationList = Array.from(mapping[destination.droppableId] || []);
    const [movedItem] = sourceList.splice(source.index, 1);
    destinationList.splice(destination.index, 0, movedItem);
    setMapping({
      ...mapping,
      [source.droppableId]: sourceList,
      [destination.droppableId]: destinationList
    });
  };

  const handleSaveMapping = () => {
    const token = localStorage.getItem('token');
    const requests = Object.keys(mapping)
      .filter(key => key !== 'unassigned')
      .map(acId => {
        const tcs = mapping[acId];
        const payload = {
          accountingCategoryId: parseInt(acId, 10),
          transactionCategoryIds: tcs.map(tc => tc.id)
        };
        return fetch('/api/accounting-categories/mapping', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify(payload)
        });
      });
    Promise.all(requests)
      .then(() => setMessage('Mapping saved successfully!'))
      .catch(err => setMessage('Error saving mapping: ' + err.message));
  };

  return (
    <div className="category-mapping-page">
      <h2>Assign Transaction Categories to Accounting Buckets</h2>
      {message && <div className="mapping-message">{message}</div>}
      <DragDropContext onDragEnd={onDragEnd}>
        <div className="mapping-container">
          <div className="mapping-columns">
            <UnassignedCategoriesColumn transactionCategories={mapping['unassigned'] || []} />
            {accountingCategories.map(ac => (
              <AccountingCategoryColumn
                key={ac.id}
                accountingCategory={ac}
                transactionCategories={mapping[ac.id] || []}
              />
            ))}
          </div>
          <div className="mapping-add-category">
            <AddAccountingCategoryForm
              onNewCategory={newAc => {
                setAccountingCategories([...accountingCategories, newAc]);
                setMapping({ ...mapping, [newAc.id]: [] });
              }}
            />
          </div>
        </div>
      </DragDropContext>
      <div className="save-button">
        <MyButton onClick={handleSaveMapping}>Save Mapping</MyButton>
      </div>
    </div>
  );
}
