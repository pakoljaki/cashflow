import React, { useEffect, useState } from "react";

const AdminDashboard = () => {
  const [users, setUsers] = useState([]);
  const role = localStorage.getItem("userRole");

  useEffect(() => {
    if (role !== "ADMIN") {
      alert("Access denied!");
      window.location.href = "/dashboard";
      return;
    }

    fetch("http://localhost:8080/api/admin/users")
      .then((res) => res.json())
      .then((data) => setUsers(data))
      .catch((err) => console.error("Error fetching users", err));
  }, [role]);

  return (
    <div>
      <h2>Admin Dashboard - User List</h2>
      <table>
        <thead>
          <tr>
            <th>Email</th>
            <th>Role</th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>{user.email}</td>
              <td>{user.role}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default AdminDashboard;
