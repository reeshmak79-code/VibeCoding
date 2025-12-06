import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Spin } from 'antd'

const PrivateRoute = ({ children }) => {
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

  return user ? children : <Navigate to="/login" />
}

export default PrivateRoute
