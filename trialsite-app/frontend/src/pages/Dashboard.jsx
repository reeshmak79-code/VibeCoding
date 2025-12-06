import { useState, useEffect } from 'react'
import { Layout, Card, Typography, Button, Row, Col, Statistic, List, Tag, Space } from 'antd'
import {
  UserOutlined,
  ProjectOutlined,
  DollarOutlined,
  PlusOutlined,
  RiseOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { dashboardService } from '../services/dashboardService'
import { PieChart, Pie, Cell, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'

const { Content } = Layout
const { Title, Text } = Typography

const Dashboard = () => {
  const navigate = useNavigate()
  const [overview, setOverview] = useState({ clients: {}, projects: {} })
  const [serviceData, setServiceData] = useState([])
  const [statusData, setStatusData] = useState([])
  const [revenueStats, setRevenueStats] = useState({ total: 0, active: 0, completed: 0 })
  const [recentProjects, setRecentProjects] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadDashboardData()
  }, [])

  const loadDashboardData = async () => {
    setLoading(true)
    try {
      // Load all dashboard data
      const [overviewData, serviceData, statusData, revenueData, recentData] = await Promise.all([
        dashboardService.getOverview(),
        dashboardService.getProjectsByService(),
        dashboardService.getProjectsByStatus(),
        dashboardService.getRevenueStats(),
        dashboardService.getRecentProjects()
      ])
      
      setOverview(overviewData)
      
      // Transform service data for pie chart
      const serviceChartData = Object.entries(serviceData)
        .filter(([_, count]) => count > 0)
        .map(([type, count]) => ({
          name: type.replace(/_/g, ' '),
          value: count
        }))
      setServiceData(serviceChartData)
      
      // Transform status data for bar chart
      const statusChartData = Object.entries(statusData)
        .filter(([_, count]) => count > 0)
        .map(([status, count]) => ({
          name: status.replace(/_/g, ' '),
          count: count
        }))
      setStatusData(statusChartData)
      
      setRevenueStats(revenueData)
      setRecentProjects(recentData)
    } catch (error) {
      console.error('Failed to load dashboard data:', error)
    } finally {
      setLoading(false)
    }
  }

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8', '#82CA9D']
  
  const serviceTypeColors = {
    'DOCUMENT PREPARATION': '#0088FE',
    'REGULATORY COMPLIANCE': '#00C49F',
    'EDC ECRF SERVICES': '#FFBB28',
    'INVESTIGATOR RECRUITMENT': '#FF8042',
    'PERSONNEL TRAINING': '#8884D8',
    'CONTRACT BUDGET NEGOTIATION': '#82CA9D'
  }

  const statusColors = {
    LEAD: 'default',
    PROPOSAL: 'processing',
    ACTIVE: 'success',
    'ON HOLD': 'warning',
    COMPLETED: 'success',
    CANCELLED: 'error'
  }

  return (
    <Content style={{ margin: '24px 16px 0' }}>
      <div style={{ padding: 24, minHeight: 360 }}>
        <Title level={3} style={{ marginBottom: 24 }}>Dashboard Overview</Title>
        
        {/* Main Statistics */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic
                title="Total Clients"
                value={overview.clients?.total || 0}
                prefix={<UserOutlined />}
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic
                title="Active Projects"
                value={overview.projects?.active || 0}
                prefix={<ProjectOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic
                title="Completed Projects"
                value={overview.projects?.completed || 0}
                prefix={<ProjectOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={12} lg={6}>
            <Card loading={loading}>
              <Statistic
                title="Total Revenue"
                value={revenueStats.total || 0}
                prefix={<DollarOutlined />}
                precision={0}
                valueStyle={{ color: '#cf1322' }}
              />
            </Card>
          </Col>
        </Row>

        {/* Charts Row */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {/* Projects by Service Type - Pie Chart */}
          <Col xs={24} lg={12}>
            <Card title="Projects by Service Type" loading={loading}>
              {serviceData.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <PieChart>
                    <Pie
                      data={serviceData}
                      cx="50%"
                      cy="50%"
                      labelLine={false}
                      label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                      outerRadius={80}
                      fill="#8884d8"
                      dataKey="value"
                    >
                      {serviceData.map((entry, index) => (
                        <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                      ))}
                    </Pie>
                    <Tooltip />
                  </PieChart>
                </ResponsiveContainer>
              ) : (
                <div style={{ textAlign: 'center', padding: '80px 0', color: '#999' }}>
                  No projects yet. Create your first project to see analytics.
                </div>
              )}
            </Card>
          </Col>

          {/* Projects by Status - Bar Chart */}
          <Col xs={24} lg={12}>
            <Card title="Projects by Status" loading={loading}>
              {statusData.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart data={statusData}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="name" />
                    <YAxis />
                    <Tooltip />
                    <Legend />
                    <Bar dataKey="count" fill="#8884d8" name="Projects" />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <div style={{ textAlign: 'center', padding: '80px 0', color: '#999' }}>
                  No projects yet. Create your first project to see analytics.
                </div>
              )}
            </Card>
          </Col>
        </Row>

        {/* Revenue & Recent Activity Row */}
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          {/* Revenue Breakdown */}
          <Col xs={24} lg={12}>
            <Card title="Revenue Overview" loading={loading}>
              <Row gutter={16}>
                <Col span={8}>
                  <Statistic
                    title="Total Budget"
                    value={revenueStats.total || 0}
                    prefix="$"
                    precision={0}
                    valueStyle={{ fontSize: 20 }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="Active Projects"
                    value={revenueStats.active || 0}
                    prefix="$"
                    precision={0}
                    valueStyle={{ fontSize: 20, color: '#1890ff' }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="Completed"
                    value={revenueStats.completed || 0}
                    prefix="$"
                    precision={0}
                    valueStyle={{ fontSize: 20, color: '#52c41a' }}
                  />
                </Col>
              </Row>
              <div style={{ marginTop: 16, padding: 16, background: '#f0f2f5', borderRadius: 4 }}>
                <Text type="secondary">
                  <RiseOutlined style={{ marginRight: 8 }} />
                  Revenue from all projects including active and completed.
                </Text>
              </div>
            </Card>
          </Col>

          {/* Recent Projects */}
          <Col xs={24} lg={12}>
            <Card title="Recent Projects" loading={loading}>
              {recentProjects.length > 0 ? (
                <List
                  dataSource={recentProjects}
                  renderItem={(project) => (
                    <List.Item>
                      <List.Item.Meta
                        title={project.projectName}
                        description={
                          <Space>
                            <Tag color={statusColors[project.status]}>
                              {project.status.replace(/_/g, ' ')}
                            </Tag>
                            <Text type="secondary">{project.clientName}</Text>
                          </Space>
                        }
                      />
                      {project.budget && (
                        <Text strong>${project.budget.toLocaleString()}</Text>
                      )}
                    </List.Item>
                  )}
                />
              ) : (
                <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
                  No projects yet.
                </div>
              )}
            </Card>
          </Col>
        </Row>

        {/* Quick Actions */}
        <Card title="Quick Actions">
          <Row gutter={16}>
            <Col xs={24} sm={8}>
              <Button
                type="primary"
                block
                icon={<PlusOutlined />}
                onClick={() => navigate('/clients')}
                size="large"
              >
                Add New Client
              </Button>
            </Col>
            <Col xs={24} sm={8}>
              <Button
                type="primary"
                block
                icon={<PlusOutlined />}
                onClick={() => navigate('/projects')}
                size="large"
              >
                Create Project
              </Button>
            </Col>
            <Col xs={24} sm={8}>
              <Button
                type="default"
                block
                icon={<ProjectOutlined />}
                onClick={() => navigate('/projects')}
                size="large"
              >
                View All Projects
              </Button>
            </Col>
          </Row>
        </Card>
      </div>
    </Content>
  )
}

export default Dashboard
