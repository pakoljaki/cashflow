// src/pages/HomePage.jsx
import React from 'react'
import {
  Container,
  Box,
  Typography,
  Paper,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Divider
} from '@mui/material'
import DescriptionIcon from '@mui/icons-material/Description'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import CategoryIcon from '@mui/icons-material/Category'
import MapIcon from '@mui/icons-material/Map'
import AssessmentIcon from '@mui/icons-material/Assessment'
import EventNoteIcon from '@mui/icons-material/EventNote'
import PlaylistAddIcon from '@mui/icons-material/PlaylistAdd'

export default function HomePage() {
  return (
    <Container maxWidth="md" sx={{ py: 4 }}>
      <Paper elevation={3} sx={{ p: 4, borderRadius: 2 }}>
        <Typography variant="h3" gutterBottom align="center">
          Welcome to Cashflow Planner
        </Typography>
        <Typography variant="body1" paragraph align="center" sx={{ color: 'text.secondary' }}>
          Cashflow Planner helps you import your bank transactions, categorize them, map them to accounting buckets, visualize key performance metrics, 
          and build detailed cashflow plans with customizable assumptions. Use the navigation above to get started. Log in to access features.
        </Typography>
        <Divider sx={{ my: 3 }} />
        <Typography variant="h5" gutterBottom>
          User Manual
        </Typography>
        <List disablePadding>
          <ListItem alignItems="flex-start">
            <ListItemIcon>
              <UploadFileIcon color="primary" />
            </ListItemIcon>
            <ListItemText
              primary="1. Importing transactions"
              secondary={
                <Typography variant="body2" color="textSecondary">
                  Use CSV files to bulk‐import your bank data. Filenames should follow the pattern <code>ACCOUNT_<em>YYYY_MM</em>.csv</code> and the first row must contain headers. 
                  Select the correct currency and bank account during import so amounts and balances are parsed correctly.
                </Typography>
              }
            />
          </ListItem>
          <ListItem alignItems="flex-start">
            <ListItemIcon>
              <CategoryIcon color="primary" />
            </ListItemIcon>
            <ListItemText
              primary="2. Categorizing transactions"
              secondary={
                <Typography variant="body2" color="textSecondary">
                  Create transaction categories to reflect your business activities (e.g. “Office Supplies”, “Sales Revenue”). 
                  On the Transactions page, assign each transaction to one of these categories. You decide how granular your breakdown should be.
                </Typography>
              }
            />
          </ListItem>
          <ListItem alignItems="flex-start">
            <ListItemIcon>
              <MapIcon color="primary" />
            </ListItemIcon>
            <ListItemText
              primary="3. Map transaction categories"
              secondary={
                <Typography variant="body2" color="textSecondary">
                  Link your transaction categories to higher‐level accounting buckets (e.g. “Operating Expenses”, “Core Revenue”). 
                  This mapping drives your KPI visualizations and cashflow reports.
                </Typography>
              }
            />
          </ListItem>
          <ListItem alignItems="flex-start">
            <ListItemIcon>
              <AssessmentIcon color="primary" />
            </ListItemIcon>
            <ListItemText
              primary="4. See KPI data"
              secondary={
                <Typography variant="body2" color="textSecondary">
                  View monthly breakdowns of Income, Expense, Net Cashflow, and Bank Balance for any year you’ve imported transactions for. 
                  Only years with transaction valueDates in your database will appear.
                </Typography>
              }
            />
          </ListItem>
          <ListItem alignItems="flex-start">
            <ListItemIcon>
              <EventNoteIcon color="primary" />
            </ListItemIcon>
            <ListItemText
              primary="5. Make cashflow plan"
              secondary={
                <Typography variant="body2" color="textSecondary">
                  Create forward‐looking cashflow plans by selecting a date range and starting balance. 
                  You can only plan a year if you have data imported for the previous year—plans use last year’s transactions as the baseline.
                </Typography>
              }
            />
          </ListItem>
          <ListItem alignItems="flex-start">
            <ListItemIcon>
              <PlaylistAddIcon color="primary" />
            </ListItemIcon>
            <ListItemText
              primary="6. Add assumptions for the plan"
              secondary={
                <>
                  <Typography variant="body2" color="textSecondary" paragraph>
                    Enhance your plan by adding assumptions. More detail yields more accurate forecasts.
                  </Typography>
                  <Typography variant="body2" component="ul" sx={{ pl: 2 }}>
                    <li><strong>One‐time:</strong> single events (e.g. equipment purchase).</li>
                    <li><strong>Recurring:</strong> regular payments (e.g. salaries, rent).</li>
                    <li><strong>Category adjustment:</strong> scale entire buckets by a percentage (e.g. +10% utilities in winter).</li>
                  </Typography>
                </>
              }
            />
          </ListItem>
        </List>
      </Paper>
    </Container>
  )
}
