import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function AdminCsvUpload() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [message, setMessage] = useState('');
  const navigate = useNavigate();
  const [role, setRole] = useState(null);

  useEffect(() => {
    const storedRole = localStorage.getItem('userRole');
    setRole(storedRole);
  }, []);

  const handleFileChange = (e) => {
    setSelectedFile(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('No file selected');
      return;
    }
    
    const token = localStorage.getItem('token');
    if (!token) {
      setMessage('Unauthorized: Please log in again.');
      return;
    }

    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await fetch('/api/admin/csv/upload', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: formData,
      });

      if (response.ok) {
        setMessage('Upload successful');
      } else {
        setMessage('Upload failed: ' + (await response.text()));
      }
    } catch (error) {
      setMessage('Error: ' + error.message);
    }
  };

  if (role !== 'ROLE_ADMIN') {
    return <div><p>Access Denied: You must be an admin to upload files.</p></div>;
  }

  return (
    <div>
      <h2>Admin CSV Upload (In-Memory Parsing)</h2>
      <input type="file" accept=".csv" onChange={handleFileChange} />
      <button onClick={handleUpload}>Upload & Parse</button>
      <p>{message}</p>
    </div>
  );
}

export default AdminCsvUpload;
