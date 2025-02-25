import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/login.css';


const Login = () => {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({ ...prevData, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    try {
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem("token", data.token); // Store JWT token
        localStorage.setItem("userRole", data.role); // Store user role

        console.log("Login successful. Role:", data.role);
        navigate("/dashboard"); // Redirect to Dashboard
      } else {
        alert("Invalid credentials");
      }
    } catch (error) {
      console.error("Error logging in:", error);
      alert("Something went wrong. Please try again.");
    }
  };

  return (
    <div className="login-container">
      <h2>Login</h2>
      <form className="login-form" onSubmit={handleSubmit}>
        <input type="email" name="email" placeholder="Email" value={formData.email} onChange={handleChange} required />
        <input type="password" name="password" placeholder="Password" value={formData.password} onChange={handleChange} required />
        <button type="submit">Login</button>
      </form>
      <p>
        Don't have an account?{" "}
        <span onClick={() => navigate('/register')} style={{ color: 'blue', cursor: 'pointer' }}>
          Register
        </span>
      </p>
    </div>
  );
};

export default Login;
