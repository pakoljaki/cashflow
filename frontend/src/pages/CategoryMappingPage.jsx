// src/pages/CategoryMappingPage.jsx
import React, { useState, useEffect } from 'react'
import { DragDropContext } from '@hello-pangea/dnd'
import { Box, Typography, Paper, Button } from '@mui/material'
import AccountingCategoryColumn from '../components/AccountingCategoryColumn'
import UnassignedCategoriesColumn from '../components/UnassignedCategoriesColumn'
import AddTransactionCategoryForm from '../components/AddTransactionCategoryForm'
import DeleteTransactionCategoryColumn from '../components/DeleteTransactionCategoryColumn'
import '../styles/CategoryMappingPage.css'

export default function CategoryMappingPage() {
  const [accountingCategories, setAccountingCategories] = useState([])
  const [transactionCategories, setTransactionCategories] = useState([])
  const [mapping, setMapping] = useState({})
  const [message, setMessage] = useState('')

  useEffect(() => {
    const token = localStorage.getItem('token')
    Promise.all([
      fetch('/api/accounting-categories', { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()),
      fetch('/api/categories', { headers: { Authorization: `Bearer ${token}` } }).then(r => r.json()),
    ])
      .then(([acData, tcData]) => {
        setAccountingCategories(acData)
        setTransactionCategories(tcData)
        const initial = {}
        acData.forEach(ac => { initial[ac.id] = [] })
        initial.unassigned = []
        initial.delete = []
        tcData.forEach(tc => {
          const key = tc.accountingCategory ? tc.accountingCategory.id : 'unassigned'
          initial[key].push(tc)
        })
        setMapping(initial)
      })
      .catch(console.error)
  }, [])

  const onDragEnd = result => {
    if (!result.destination) return
    const { source, destination } = result
    if (source.droppableId === destination.droppableId && source.index === destination.index) return
    const srcList = Array.from(mapping[source.droppableId])
    const dstList = Array.from(mapping[destination.droppableId])
    const [moved] = srcList.splice(source.index, 1)
    dstList.splice(destination.index, 0, moved)
    setMapping({
      ...mapping,
      [source.droppableId]: srcList,
      [destination.droppableId]: dstList,
    })
  }

  const handleSaveMapping = async () => {
    const token = localStorage.getItem('token')
    const saveRequests = Object.keys(mapping)
      .filter(key => key !== 'unassigned' && key !== 'delete')
      .map(acId => {
        const ids = mapping[acId].map(tc => tc.id)
        return fetch('/api/accounting-categories/mapping', {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            accountingCategoryId: parseInt(acId, 10),
            transactionCategoryIds: ids,
          }),
        })
      })

    const deleteRequests = mapping.delete.map(tc =>
      fetch(`/api/categories/${tc.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
    )

    try {
      await Promise.all([...saveRequests, ...deleteRequests])
      setMessage('Mapping saved and deletions processed.')
      window.location.reload()
    } catch (err) {
      setMessage('Error: ' + err.message)
    }
  }

  return (
    <Box className="category-mapping-page">
      <Typography variant="h5" align="center" gutterBottom>
        Assign Transaction Categories to Accounting Buckets
      </Typography>
      {message && (
        <Typography variant="body1" color="success.main" className="mapping-message">
          {message}
        </Typography>
      )}
      <DragDropContext onDragEnd={onDragEnd}>
        <Box className="mapping-container">
          <Box className="mapping-columns">
            {accountingCategories.map(ac => (
              <Paper key={ac.id} variant="outlined" className="mapping-column-paper">
                <AccountingCategoryColumn
                  accountingCategory={ac}
                  transactionCategories={mapping[ac.id] || []}
                />
              </Paper>
            ))}
            <Paper variant="outlined" className="mapping-column-paper">
              <UnassignedCategoriesColumn transactionCategories={mapping.unassigned || []} />
            </Paper>
          </Box>

          <Box className="mapping-add-section">
            <AddTransactionCategoryForm
              onNewCategory={newTc => {
                setTransactionCategories(prev => [...prev, newTc])
                setMapping({
                  ...mapping,
                  unassigned: [...mapping.unassigned, newTc],
                })
              }}
            />
            <DeleteTransactionCategoryColumn transactionCategories={mapping.delete || []} />
          </Box>
        </Box>
      </DragDropContext>

      <Box textAlign="center" mt={2}>
        <Button variant="contained" color="primary" onClick={handleSaveMapping}>
          Save Mapping
        </Button>
      </Box>
    </Box>
  )
}
