import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Login from './pages/Login'
import Signup from './pages/Signup'
import Dashboard from './pages/Dashboard'
import Clients from './pages/Clients'
import Projects from './pages/Projects'
import Documents from './pages/Documents'
import Users from './pages/Users'
import Signatures from './pages/Signatures'
import PrivateRoute from './components/PrivateRoute'
import MainLayout from './components/MainLayout'

// Component to handle root redirect based on user role
const RootRedirect = () => {
  const { user } = useAuth()
  const isAdminOrDoctor = user?.role === 'ADMIN' || user?.role === 'DOCTOR'
  return <Navigate to={isAdminOrDoctor ? '/dashboard' : '/documents'} replace />
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route
            path="/dashboard"
            element={
              <PrivateRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <MainLayout>
                  <Dashboard />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route
            path="/clients"
            element={
              <PrivateRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <MainLayout>
                  <Clients />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route
            path="/projects"
            element={
              <PrivateRoute allowedRoles={['ADMIN', 'DOCTOR']}>
                <MainLayout>
                  <Projects />
                </MainLayout>
              </PrivateRoute>
            }
          />
          <Route
            path="/documents"
            element={
              <PrivateRoute>
                <MainLayout>
                  <Documents />
                </MainLayout>
              </PrivateRoute>
            }
          />
                <Route
                  path="/users"
                  element={
                    <PrivateRoute allowedRoles={['ADMIN']}>
                      <MainLayout>
                        <Users />
                      </MainLayout>
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/signatures"
                  element={
                    <PrivateRoute>
                      <MainLayout>
                        <Signatures />
                      </MainLayout>
                    </PrivateRoute>
                  }
                />
                <Route
                  path="/"
                  element={
                    <PrivateRoute>
                      <RootRedirect />
                    </PrivateRoute>
                  }
                />
        </Routes>
      </Router>
    </AuthProvider>
  )
}

export default App
