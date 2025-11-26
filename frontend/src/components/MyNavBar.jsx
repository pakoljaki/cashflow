import { Navbar, Container } from 'react-bootstrap'
import { NavLink } from 'react-router-dom'
import MyButton from './MyButton'
import '../styles/navbar.css'

export default function MyNavBar() {
  return (
    <Navbar expand="lg" className="my-navbar sticky-top">
      <Container fluid>
        <Navbar.Brand as={NavLink} to="/" className="navbar-brand-custom">
          Cashflow Planner
        </Navbar.Brand>
        <Navbar.Toggle
          aria-controls="navbar-nav"
          className="navbar-toggler-custom d-lg-none"
        />
        <Navbar.Collapse id="navbar-nav">
          <div className="navbar-buttons">
            <NavLink to="/login" className="nav-button-link">
              <MyButton variant="primary">Login</MyButton>
            </NavLink>
            <NavLink to="/admin" className="nav-button-link">
              <MyButton variant="primary">Admin Panel</MyButton>
            </NavLink>
            <NavLink to="/transactions" className="nav-button-link">
              <MyButton variant="primary">Transactions</MyButton>
            </NavLink>
            <NavLink to="/accounting-mapping" className="nav-button-link">
              <MyButton variant="primary">Accounting Mapping</MyButton>
            </NavLink>
            <NavLink to="/kpis" className="nav-button-link">
              <MyButton variant="primary">KPI Page</MyButton>
            </NavLink>
            <NavLink to="/cashflow-plans" className="nav-button-link">
              <MyButton variant="primary">Cashflow Plans</MyButton>
            </NavLink>
          </div>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  )
}
