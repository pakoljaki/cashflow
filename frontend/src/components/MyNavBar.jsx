import React from 'react';
import { Link } from 'react-router-dom';
import { Navbar, Nav, Container } from 'react-bootstrap';

const MyNavBar = () => {
  return (
    <Navbar bg="light" expand="lg" className="my-navbar">
      <Container>
        <Navbar.Brand as={Link} to="/">Cashflow Planner</Navbar.Brand>
        <Navbar.Toggle aria-controls="navbar-nav" />
        <Navbar.Collapse id="navbar-nav">
          <Nav className="ms-auto">
            <Nav.Link as={Link} to="/dashboard">Dashboard</Nav.Link>
            <Nav.Link as={Link} to="/admin/csv-upload">CSV Upload</Nav.Link>
            <Nav.Link as={Link} to="/transactions">Transactions</Nav.Link>
            <Nav.Link as={Link} to="/cashflow-plans">Cashflow Plans</Nav.Link>
            <Nav.Link as={Link} to="/kpis">KPI Page</Nav.Link>
            <Nav.Link as={Link} to="/accounting-mapping">Accounting Mapping</Nav.Link>
            <Nav.Link as={Link} to="/about">About</Nav.Link>
            <Nav.Link as={Link} to="/login">Login</Nav.Link>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
};

export default MyNavBar;
