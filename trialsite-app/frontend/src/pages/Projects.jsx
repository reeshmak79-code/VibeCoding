import { useState, useEffect } from 'react'
import {
  Layout,
  Card,
  Table,
  Button,
  Space,
  Input,
  Tag,
  Modal,
  Form,
  Select,
  DatePicker,
  InputNumber,
  message,
  Popconfirm,
  Row,
  Col,
  Statistic
} from 'antd'
import {
  PlusOutlined,
  SearchOutlined,
  EditOutlined,
  DeleteOutlined,
  ProjectOutlined
} from '@ant-design/icons'
import { projectService } from '../services/projectService'
import { clientService } from '../services/clientService'
import dayjs from 'dayjs'

const { Content } = Layout
const { TextArea } = Input

const Projects = () => {
  const [projects, setProjects] = useState([])
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [editingProject, setEditingProject] = useState(null)
  const [stats, setStats] = useState({ total: 0, active: 0, completed: 0, proposal: 0 })
  const [form] = Form.useForm()

  useEffect(() => {
    loadProjects()
    loadClients()
    loadStats()
  }, [])

  const loadProjects = async () => {
    setLoading(true)
    try {
      const data = await projectService.getAllProjects()
      setProjects(data)
    } catch (error) {
      message.error('Failed to load projects')
    } finally {
      setLoading(false)
    }
  }

  const loadClients = async () => {
    try {
      const data = await clientService.getAllClients()
      setClients(data)
    } catch (error) {
      console.error('Failed to load clients:', error)
    }
  }

  const loadStats = async () => {
    try {
      const data = await projectService.getProjectStats()
      setStats(data)
    } catch (error) {
      console.error('Failed to load stats:', error)
    }
  }

  const handleSearch = async (value) => {
    if (!value) {
      loadProjects()
      return
    }
    
    setLoading(true)
    try {
      const data = await projectService.searchProjects(value)
      setProjects(data)
    } catch (error) {
      message.error('Search failed')
    } finally {
      setLoading(false)
    }
  }

  const showModal = (project = null) => {
    setEditingProject(project)
    if (project) {
      form.setFieldsValue({
        ...project,
        startDate: project.startDate ? dayjs(project.startDate) : null,
        endDate: project.endDate ? dayjs(project.endDate) : null
      })
    } else {
      form.resetFields()
    }
    setIsModalVisible(true)
  }

  const handleCancel = () => {
    setIsModalVisible(false)
    setEditingProject(null)
    form.resetFields()
  }

  const handleSubmit = async (values) => {
    try {
      const projectData = {
        ...values,
        startDate: values.startDate ? values.startDate.format('YYYY-MM-DD') : null,
        endDate: values.endDate ? values.endDate.format('YYYY-MM-DD') : null
      }
      
      if (editingProject) {
        await projectService.updateProject(editingProject.id, projectData)
        message.success('Project updated successfully')
      } else {
        await projectService.createProject(projectData)
        message.success('Project created successfully')
      }
      setIsModalVisible(false)
      form.resetFields()
      loadProjects()
      loadStats()
    } catch (error) {
      message.error(error.response?.data?.message || 'Operation failed')
    }
  }

  const handleDelete = async (id) => {
    try {
      await projectService.deleteProject(id)
      message.success('Project deleted successfully')
      loadProjects()
      loadStats()
    } catch (error) {
      message.error('Failed to delete project')
    }
  }

  const serviceTypeColors = {
    DOCUMENT_PREPARATION: 'blue',
    REGULATORY_COMPLIANCE: 'green',
    EDC_ECRF_SERVICES: 'purple',
    INVESTIGATOR_RECRUITMENT: 'orange',
    PERSONNEL_TRAINING: 'cyan',
    CONTRACT_BUDGET_NEGOTIATION: 'magenta'
  }

  const statusColors = {
    LEAD: 'default',
    PROPOSAL: 'processing',
    ACTIVE: 'success',
    ON_HOLD: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'error'
  }

  const columns = [
    {
      title: 'Project Name',
      dataIndex: 'projectName',
      key: 'projectName',
      sorter: (a, b) => a.projectName.localeCompare(b.projectName),
    },
    {
      title: 'Client',
      dataIndex: 'clientName',
      key: 'clientName',
      sorter: (a, b) => a.clientName.localeCompare(b.clientName),
    },
    {
      title: 'Service Type',
      dataIndex: 'serviceType',
      key: 'serviceType',
      render: (type) => (
        <Tag color={serviceTypeColors[type]}>
          {type.replace(/_/g, ' ')}
        </Tag>
      ),
      filters: [
        { text: 'Document Preparation', value: 'DOCUMENT_PREPARATION' },
        { text: 'Regulatory/Compliance', value: 'REGULATORY_COMPLIANCE' },
        { text: 'EDC/eCRF Services', value: 'EDC_ECRF_SERVICES' },
        { text: 'Investigator Recruitment', value: 'INVESTIGATOR_RECRUITMENT' },
        { text: 'Personnel Training', value: 'PERSONNEL_TRAINING' },
        { text: 'Contract/Budget Negotiation', value: 'CONTRACT_BUDGET_NEGOTIATION' },
      ],
      onFilter: (value, record) => record.serviceType === value,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={statusColors[status]}>
          {status.replace(/_/g, ' ')}
        </Tag>
      ),
      filters: [
        { text: 'Lead', value: 'LEAD' },
        { text: 'Proposal', value: 'PROPOSAL' },
        { text: 'Active', value: 'ACTIVE' },
        { text: 'On Hold', value: 'ON_HOLD' },
        { text: 'Completed', value: 'COMPLETED' },
        { text: 'Cancelled', value: 'CANCELLED' },
      ],
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'Start Date',
      dataIndex: 'startDate',
      key: 'startDate',
      sorter: (a, b) => new Date(a.startDate) - new Date(b.startDate),
    },
    {
      title: 'Budget',
      dataIndex: 'budget',
      key: 'budget',
      render: (budget) => budget ? `$${budget.toLocaleString()}` : '-',
      sorter: (a, b) => (a.budget || 0) - (b.budget || 0),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="middle">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => showModal(record)}
          >
            Edit
          </Button>
          <Popconfirm
            title="Are you sure you want to delete this project?"
            onConfirm={() => handleDelete(record.id)}
            okText="Yes"
            cancelText="No"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Content style={{ margin: '24px 16px 0' }}>
      <div style={{ padding: 24, minHeight: 360 }}>
        <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
          <Col xs={24} sm={8} lg={6}>
            <Card>
              <Statistic
                title="Total Projects"
                value={stats.total}
                prefix={<ProjectOutlined />}
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8} lg={6}>
            <Card>
              <Statistic
                title="Active Projects"
                value={stats.active}
                prefix={<ProjectOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8} lg={6}>
            <Card>
              <Statistic
                title="Completed Projects"
                value={stats.completed}
                prefix={<ProjectOutlined />}
                valueStyle={{ color: '#52c41a' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8} lg={6}>
            <Card>
              <Statistic
                title="Proposals"
                value={stats.proposal}
                prefix={<ProjectOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
        </Row>

        <Card
          title="Projects"
          extra={
            <Space>
              <Input
                placeholder="Search projects..."
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                onPressEnter={(e) => handleSearch(e.target.value)}
                style={{ width: 250 }}
                allowClear
                onClear={() => {
                  setSearchText('')
                  loadProjects()
                }}
              />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => showModal()}
              >
                Add Project
              </Button>
            </Space>
          }
        >
          <Table
            columns={columns}
            dataSource={projects}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} projects`,
            }}
          />
        </Card>

        <Modal
          title={editingProject ? 'Edit Project' : 'Add New Project'}
          open={isModalVisible}
          onCancel={handleCancel}
          footer={null}
          width={700}
        >
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
          >
            <Form.Item
              name="projectName"
              label="Project Name"
              rules={[
                { required: true, message: 'Please enter project name' },
                { min: 2, message: 'Project name must be at least 2 characters' }
              ]}
            >
              <Input placeholder="Enter project name" />
            </Form.Item>

            <Form.Item
              name="clientId"
              label="Client"
              rules={[{ required: true, message: 'Please select a client' }]}
            >
              <Select
                placeholder="Select client"
                showSearch
                optionFilterProp="children"
              >
                {clients.map(client => (
                  <Select.Option key={client.id} value={client.id}>
                    {client.companyName}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="serviceType"
                  label="Service Type"
                  rules={[{ required: true, message: 'Please select service type' }]}
                >
                  <Select placeholder="Select service type">
                    <Select.Option value="DOCUMENT_PREPARATION">Essential Document Preparation</Select.Option>
                    <Select.Option value="REGULATORY_COMPLIANCE">Regulatory/Compliance</Select.Option>
                    <Select.Option value="EDC_ECRF_SERVICES">EDC/eCRF Services</Select.Option>
                    <Select.Option value="INVESTIGATOR_RECRUITMENT">Investigator Recruitment</Select.Option>
                    <Select.Option value="PERSONNEL_TRAINING">Personnel Training</Select.Option>
                    <Select.Option value="CONTRACT_BUDGET_NEGOTIATION">Contract/Budget Negotiation</Select.Option>
                  </Select>
                </Form.Item>
              </Col>

              <Col span={12}>
                <Form.Item
                  name="status"
                  label="Status"
                  rules={[{ required: true, message: 'Please select status' }]}
                >
                  <Select placeholder="Select status">
                    <Select.Option value="LEAD">Lead</Select.Option>
                    <Select.Option value="PROPOSAL">Proposal Sent</Select.Option>
                    <Select.Option value="ACTIVE">Active</Select.Option>
                    <Select.Option value="ON_HOLD">On Hold</Select.Option>
                    <Select.Option value="COMPLETED">Completed</Select.Option>
                    <Select.Option value="CANCELLED">Cancelled</Select.Option>
                  </Select>
                </Form.Item>
              </Col>
            </Row>

            <Row gutter={16}>
              <Col span={12}>
                <Form.Item
                  name="startDate"
                  label="Start Date"
                  rules={[{ required: true, message: 'Please select start date' }]}
                >
                  <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
                </Form.Item>
              </Col>

              <Col span={12}>
                <Form.Item
                  name="endDate"
                  label="End Date"
                >
                  <DatePicker style={{ width: '100%' }} format="YYYY-MM-DD" />
                </Form.Item>
              </Col>
            </Row>

            <Form.Item
              name="budget"
              label="Budget (USD)"
            >
              <InputNumber
                style={{ width: '100%' }}
                prefix="$"
                formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                parser={value => value.replace(/\$\s?|(,*)/g, '')}
                placeholder="Enter budget"
              />
            </Form.Item>

            <Form.Item
              name="description"
              label="Description"
            >
              <TextArea
                rows={3}
                placeholder="Project description"
              />
            </Form.Item>

            <Form.Item
              name="deliverables"
              label="Deliverables"
            >
              <TextArea
                rows={3}
                placeholder="Expected deliverables"
              />
            </Form.Item>

            <Form.Item
              name="notes"
              label="Notes"
            >
              <TextArea
                rows={2}
                placeholder="Additional notes"
              />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
              <Space>
                <Button onClick={handleCancel}>
                  Cancel
                </Button>
                <Button type="primary" htmlType="submit">
                  {editingProject ? 'Update' : 'Create'}
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </Content>
  )
}

export default Projects
