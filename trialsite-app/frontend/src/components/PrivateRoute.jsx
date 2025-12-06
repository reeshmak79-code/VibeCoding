import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Spin, message } from 'antd'

const PrivateRoute = ({ children, allowedRoles }) => {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <div style={{ 
        display: 'flex', 
        justifyContent: 'center', 
        alignItems: 'center', 
        height: '100vh' 
      }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!user) {
    return <Navigate to="/login" />
  }

  // If allowedRoles is specified, check if user's role is allowed
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    message.error('Access denied. You do not have permission to access this page.')
    // Redirect non-admin/doctor users to documents
    const isAdminOrDoctor = user.role === 'ADMIN' || user.role === 'DOCTOR'
    return <Navigate to={isAdminOrDoctor ? '/dashboard' : '/documents'} replace />
  }

  return children
}

export default PrivateRoute
