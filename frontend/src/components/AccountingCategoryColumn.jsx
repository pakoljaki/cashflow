import React from 'react';
import { Droppable, Draggable } from '@hello-pangea/dnd';
import TransactionCategoryTile from './TransactionCategoryTile';

export default function AccountingCategoryColumn({ accountingCategory, transactionCategories }) {
  return (
    <div className="accounting-column">
      <h3>{accountingCategory.displayName}</h3>
      <Droppable droppableId={String(accountingCategory.id)}>
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
                {(provided, snapshot) => (
                  <div
                    ref={provided.innerRef}
                    {...provided.draggableProps}
                    {...provided.dragHandleProps}
                    className="draggable-tile"
                    style={{
                      background: snapshot.isDragging ? '#ddd' : '#eee',
                      ...provided.draggableProps.style
                    }}
                  >
                    <TransactionCategoryTile transactionCategory={tc} />
                  </div>
                )}
              </Draggable>
            ))}
            {provided.placeholder}
          </div>
        )}
      </Droppable>
    </div>
  );
}
