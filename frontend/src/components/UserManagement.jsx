import { useState, useEffect } from 'react'
import {
  Box,
  Paper,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemButton,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Divider,
  Alert,
} from '@mui/material'
import PersonIcon from '@mui/icons-material/Person'
import AdminPanelSettingsIcon from '@mui/icons-material/AdminPanelSettings'

export default function UserManagement({ onUserChange }) {
  const [users, setUsers] = useState([])
  const [selectedUser, setSelectedUser] = useState(null)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [promoteDialogOpen, setPromoteDialogOpen] = useState(false)
  const [message, setMessage] = useState({ text: '', severity: 'info' })

  useEffect(() => {
    fetchUsers()
  }, [])

  const fetchUsers = async () => {
    const token = localStorage.getItem('token')
    try {
      const res = await fetch('/api/admin/users', {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error('Failed to fetch users')
      const data = await res.json()
      setUsers(data)
    } catch (err) {
      setMessage({ text: 'Error loading users: ' + err.message, severity: 'error' })
    }
  }

  const handleDeleteUser = async () => {
    if (!selectedUser) return
    const token = localStorage.getItem('token')
    try {
      const res = await fetch(`/api/admin/users/${selectedUser.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error('Failed to delete user')
      setMessage({ text: `User ${selectedUser.email} deleted successfully`, severity: 'success' })
      setDeleteDialogOpen(false)
      setSelectedUser(null)
      fetchUsers()
      if (onUserChange) onUserChange()
    } catch (err) {
      setMessage({ text: 'Error deleting user: ' + err.message, severity: 'error' })
    }
  }

  const handlePromoteUser = async () => {
    if (!selectedUser) return
    const token = localStorage.getItem('token')
    try {
      const res = await fetch(`/api/admin/users/${selectedUser.id}/promote`, {
        method: 'PATCH',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error('Failed to promote user')
      setMessage({ text: `User ${selectedUser.email} promoted to ADMIN`, severity: 'success' })
      setPromoteDialogOpen(false)
      setSelectedUser(null)
      fetchUsers()
      if (onUserChange) onUserChange()
    } catch (err) {
      setMessage({ text: 'Error promoting user: ' + err.message, severity: 'error' })
    }
  }

  const isAdmin = (user) => user.role === 'ADMIN'

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Typography variant="h5" gutterBottom>
        User Management
      </Typography>
      
      {message.text && (
        <Alert severity={message.severity} sx={{ mb: 2 }}>
          {message.text}
        </Alert>
      )}

      <Paper variant="outlined" sx={{ mb: 3, flexGrow: 1, overflow: 'auto', minHeight: 500, maxHeight: 600 }}>
        <List dense sx={{ pt: 0 }}>
          {users.map((user, index) => (
            <Box key={user.id}>
              <ListItem
                disablePadding
                secondaryAction={
                  isAdmin(user) ? (
                    <Chip 
                      icon={<AdminPanelSettingsIcon />} 
                      label="ADMIN" 
                      color="primary" 
                      size="small"
                      sx={{ mr: 1 }}
                    />
                  ) : (
                    <Chip 
                      icon={<PersonIcon />} 
                      label="USER" 
                      size="small"
                      sx={{ mr: 1 }}
                    />
                  )
                }
              >
                <ListItemButton
                  selected={selectedUser?.id === user.id}
                  onClick={() => setSelectedUser(user)}
                  sx={{ py: 1.5 }}
                >
                  <ListItemText
                    primary={user.email}
                    secondary={`ID: ${user.id}`}
                    primaryTypographyProps={{ fontWeight: selectedUser?.id === user.id ? 600 : 400 }}
                  />
                </ListItemButton>
              </ListItem>
              {index < users.length - 1 && <Divider />}
            </Box>
          ))}
          {users.length === 0 && (
            <ListItem sx={{ py: 4 }}>
              <ListItemText 
                primary="No users found"
                sx={{ textAlign: 'center', color: 'text.secondary' }}
              />
            </ListItem>
          )}
        </List>
      </Paper>

      {selectedUser && (
        <Box sx={{ 
          display: 'flex', 
          gap: 2, 
          mt: 'auto',
          p: 2,
          bgcolor: '#f5f7fa',
          borderRadius: 2
        }}>
          {!isAdmin(selectedUser) && (
            <Button
              variant="contained"
              color="primary"
              onClick={() => setPromoteDialogOpen(true)}
              fullWidth
              size="large"
              sx={{ py: 1.5 }}
            >
              ⬆️ Promote to Admin
            </Button>
          )}
          <Button
            variant="outlined"
            color="error"
            onClick={() => setDeleteDialogOpen(true)}
            fullWidth
            size="large"
            sx={{ py: 1.5 }}
          >
            🗑️ Delete User
          </Button>
        </Box>
      )}

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialogOpen} onClose={() => setDeleteDialogOpen(false)}>
        <DialogTitle>Confirm Delete</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to delete user <strong>{selectedUser?.email}</strong>? This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleDeleteUser} color="error" variant="contained">
            Delete
          </Button>
        </DialogActions>
      </Dialog>

      {/* Promote Confirmation Dialog */}
      <Dialog open={promoteDialogOpen} onClose={() => setPromoteDialogOpen(false)}>
        <DialogTitle>Confirm Promotion</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Are you sure you want to promote <strong>{selectedUser?.email}</strong> to ADMIN role?
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPromoteDialogOpen(false)}>Cancel</Button>
          <Button onClick={handlePromoteUser} color="primary" variant="contained">
            Promote
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
