import React from 'react'
import { Droppable, Draggable } from '@hello-pangea/dnd'
import TransactionCategoryTile from './TransactionCategoryTile'

export default function UnassignedCategoriesColumn({ transactionCategories }) {
  return (
    <div className="unassigned-column">
      <h3>Unassigned</h3>
      <Droppable droppableId="unassigned">
        {(provided, snapshot) => (
          <div
            ref={provided.innerRef}
            {...provided.droppableProps}
            className="droppable-column"
            style={{
              background: snapshot.isDraggingOver ? '#f0f0f0' : '#fff'
            }}
          >
            {transactionCategories.map((tc, index) => (
              <Draggable key={tc.id} draggableId={String(tc.id)} index={index}>
                {(provided, snapshot) => {
                  const bg = snapshot.isDragging
                    ? tc.direction === 'POSITIVE' ? '#a5d6a7' : '#ef9a9a'
                    : tc.direction === 'POSITIVE' ? '#c8e6c9' : '#ffcdd2'
                  return (
                    <div
                      ref={provided.innerRef}
                      {...provided.draggableProps}
                      {...provided.dragHandleProps}
                      className="draggable-tile"
                      style={{
                        background: bg,
                        ...provided.draggableProps.style
                      }}
                    >
                      <TransactionCategoryTile transactionCategory={tc} />
                    </div>
                  )
                }}
              </Draggable>
            ))}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </div>
  )
}
