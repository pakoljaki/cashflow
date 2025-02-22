// frontend/src/components/MyButton.jsx
import React from 'react';
import './MyButton.css'; // optional additional styling

const MyButton = ({ variant = 'primary', type = 'button', onClick, children }) => {
  const btnClass = `my-button ${variant}`;
  return (
    <button className={btnClass} type={type} onClick={onClick}>
      {children}
    </button>
  );
};

export default MyButton;
