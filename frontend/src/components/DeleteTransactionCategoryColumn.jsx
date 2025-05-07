// src/components/DeleteTransactionCategoryColumn.jsx
import React from 'react'
import { Droppable, Draggable } from '@hello-pangea/dnd'
import { Box, Typography, Paper } from '@mui/material'

export default function DeleteTransactionCategoryColumn({ transactionCategories }) {
  return (
    <Box sx={{ mt: 2 }}>
      <Typography variant="subtitle1" gutterBottom>
        Delete Transaction Categories
      </Typography>
      <Droppable droppableId="delete">
        {(provided, snapshot) => (
          <Box
            ref={provided.innerRef}
            {...provided.droppableProps}
            sx={{
              minHeight: 100,
              p: 1,
              border: '1px dashed #ccc',
              borderRadius: 1,
              backgroundColor: snapshot.isDraggingOver ? 'action.hover' : '#fff',
            }}
          >
            {transactionCategories.map((tc, idx) => (
              <Draggable key={tc.id} draggableId={String(tc.id)} index={idx}>
                {(prov, snap) => {
                  const bg = snap.isDragging
                    ? tc.direction === 'POSITIVE' ? '#a5d6a7' : '#ef9a9a'
                    : tc.direction === 'POSITIVE' ? '#c8e6c9' : '#ffcdd2'
                  return (
                    <Paper
                      ref={prov.innerRef}
                      {...prov.draggableProps}
                      {...prov.dragHandleProps}
                      elevation={0}
                      sx={{
                        p: 1,
                        mb: 1,
                        bgcolor: bg,
                        cursor: 'grab',
                      }}
                    >
                      {tc.name}
                    </Paper>
                  )
                }}
              </Draggable>
            ))}
            {provided.placeholder}
          </Box>
        )}
      </Droppable>
    </Box>
  )
}
