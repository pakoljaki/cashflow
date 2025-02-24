import React, { useState } from 'react';

function AdminCsvUpload() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [message, setMessage] = useState('');

  const handleFileChange = (e) => {
    setSelectedFile(e.target.files[0]);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setMessage('No file selected');
      return;
    }
    const formData = new FormData();
    formData.append('file', selectedFile);

    try {
      const response = await fetch('/api/admin/csv/upload', {
        method: 'POST',
        body: formData,
      });
      if (response.ok) {
        const text = await response.text();
        setMessage('Upload success: ' + text);
      } else {
        const errorText = await response.text();
        setMessage('Upload failed: ' + errorText);
      }
    } catch (error) {
      console.error('Error uploading file', error);
      setMessage('Error: ' + error.message);
    }
  };

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
