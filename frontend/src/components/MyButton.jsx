import React from 'react';
import './MyButton.css'; 

const MyButton = ({ variant = 'primary', type = 'button', onClick, children }) => {
  const btnClass = `my-button ${variant}`;
  return (
    <button className={btnClass} type={type} onClick={onClick}>
      {children}
    </button>
  );
};

export default MyButton;
