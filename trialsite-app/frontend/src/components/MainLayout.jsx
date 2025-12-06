import { useState } from 'react'
import { Layout, Menu, Typography, Button } from 'antd'
import {
  UserOutlined,
  ProjectOutlined,
  CalendarOutlined,
  FileTextOutlined,
  LogoutOutlined,
  DashboardOutlined,
  TeamOutlined,
  EditOutlined
} from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'
import { useNavigate, useLocation } from 'react-router-dom'

const { Header, Sider } = Layout
const { Text } = Typography

const MainLayout = ({ children }) => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [collapsed, setCollapsed] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  // Role-based menu items
  const isAdminOrDoctor = user?.role === 'ADMIN' || user?.role === 'DOCTOR'
  const hasRestrictedAccess = !isAdminOrDoctor // USER, AUDITOR, COORDINATOR, etc.
  
  const menuItems = hasRestrictedAccess ? [
    // USER, AUDITOR, COORDINATOR: Only Documents (permission-filtered) and Signatures
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: 'Documents',
      onClick: () => navigate('/documents')
    },
    {
      key: '/signatures',
      icon: <EditOutlined />,
      label: 'Signatures',
      onClick: () => navigate('/signatures')
    }
  ] : [
    // ADMIN and DOCTOR: Full access
    {
      key: '/dashboard',
      icon: <DashboardOutlined />,
      label: 'Dashboard',
      onClick: () => navigate('/dashboard')
    },
    {
      key: '/clients',
      icon: <UserOutlined />,
      label: 'Clients',
      onClick: () => navigate('/clients')
    },
    {
      key: '/projects',
      icon: <ProjectOutlined />,
      label: 'Projects',
      onClick: () => navigate('/projects')
    },
    {
      key: '/calendar',
      icon: <CalendarOutlined />,
      label: 'Calendar',
      disabled: true
    },
    {
      key: '/documents',
      icon: <FileTextOutlined />,
      label: 'Documents',
      onClick: () => navigate('/documents')
    },
    {
      key: '/signatures',
      icon: <EditOutlined />,
      label: 'Signatures',
      onClick: () => navigate('/signatures')
    },
    // Only show Users menu for ADMIN
    ...(user?.role === 'ADMIN' ? [{
      key: '/users',
      icon: <TeamOutlined />,
      label: 'Users',
      onClick: () => navigate('/users')
    }] : [])
  ]

  const selectedKey = menuItems.find(item => location.pathname.startsWith(item.key))?.key || '/dashboard'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        breakpoint="lg"
        style={{
          background: '#001529'
        }}
      >
        <div style={{
          height: 64,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: 'white',
          fontSize: collapsed ? 16 : 18,
          fontWeight: 'bold'
        }}>
          {collapsed ? 'TS' : 'TrialSite'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          items={menuItems}
        />
      </Sider>

      <Layout>
        <Header style={{
          padding: '0 24px',
          background: '#fff',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          boxShadow: '0 1px 4px rgba(0,21,41,.08)'
        }}>
          <div />
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Text>Welcome, <strong>{user?.fullName}</strong></Text>
            <Button
              type="primary"
              danger
              icon={<LogoutOutlined />}
              onClick={handleLogout}
            >
              Logout
            </Button>
          </div>
        </Header>

        {children}
      </Layout>
    </Layout>
  )
}

export default MainLayout
