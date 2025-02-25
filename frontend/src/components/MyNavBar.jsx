import React from 'react';
import { Link } from 'react-router-dom';
import { Navbar, Nav, Container } from 'react-bootstrap';
import '../styles/navbar.css'; // Optional custom styles

const MyNavbar = () => {
  return (
    <Navbar bg="light" expand="lg" className="my-navbar">
      <Container>
        {/* Brand / Logo */}
        <Navbar.Brand as={Link} to="/" className="navbar-brand-custom">
          Cashflow Planner
        </Navbar.Brand>

        <Navbar.Toggle aria-controls="navbar-nav" />

        <Navbar.Collapse id="navbar-nav">
          <Nav className="ms-auto">
            {/* Example links: adjust as needed */}
            <Nav.Link as={Link} to="/dashboard">Dashboard</Nav.Link>
            <Nav.Link as={Link} to="/admin/csv-upload">CSV Upload</Nav.Link>
            <Nav.Link as={Link} to="/transactions">Transactions</Nav.Link>
            <Nav.Link as={Link} to="/about">About</Nav.Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default MyNavbar;
