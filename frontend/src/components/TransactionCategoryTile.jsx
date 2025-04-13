import React from 'react';

export default function TransactionCategoryTile({ transactionCategory }) {
  return (
    <div className="transaction-category-tile">
      {transactionCategory.name}
    </div>
  );
}
