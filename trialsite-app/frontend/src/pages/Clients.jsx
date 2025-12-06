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
  UserOutlined
} from '@ant-design/icons'
import { clientService } from '../services/clientService'

const { Content } = Layout
const { TextArea } = Input

const Clients = () => {
  const [clients, setClients] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [isModalVisible, setIsModalVisible] = useState(false)
  const [editingClient, setEditingClient] = useState(null)
  const [stats, setStats] = useState({ total: 0, active: 0, potential: 0 })
  const [form] = Form.useForm()

  useEffect(() => {
    loadClients()
    loadStats()
  }, [])

  const loadClients = async () => {
    setLoading(true)
    try {
      const data = await clientService.getAllClients()
      setClients(data)
    } catch (error) {
      message.error('Failed to load clients')
    } finally {
      setLoading(false)
    }
  }

  const loadStats = async () => {
    try {
      const data = await clientService.getClientStats()
      setStats(data)
    } catch (error) {
      console.error('Failed to load stats:', error)
    }
  }

  const handleSearch = async (value) => {
    if (!value) {
      loadClients()
      return
    }
    
    setLoading(true)
    try {
      const data = await clientService.searchClients(value)
      setClients(data)
    } catch (error) {
      message.error('Search failed')
    } finally {
      setLoading(false)
    }
  }

  const showModal = (client = null) => {
    setEditingClient(client)
    if (client) {
      form.setFieldsValue(client)
    } else {
      form.resetFields()
    }
    setIsModalVisible(true)
  }

  const handleCancel = () => {
    setIsModalVisible(false)
    setEditingClient(null)
    form.resetFields()
  }

  const handleSubmit = async (values) => {
    try {
      if (editingClient) {
        await clientService.updateClient(editingClient.id, values)
        message.success('Client updated successfully')
      } else {
        await clientService.createClient(values)
        message.success('Client created successfully')
      }
      setIsModalVisible(false)
      form.resetFields()
      loadClients()
      loadStats()
    } catch (error) {
      message.error(error.response?.data?.message || 'Operation failed')
    }
  }

  const handleDelete = async (id) => {
    try {
      await clientService.deleteClient(id)
      message.success('Client deleted successfully')
      loadClients()
      loadStats()
    } catch (error) {
      message.error('Failed to delete client')
    }
  }

  const columns = [
    {
      title: 'Company Name',
      dataIndex: 'companyName',
      key: 'companyName',
      sorter: (a, b) => a.companyName.localeCompare(b.companyName),
    },
    {
      title: 'Contact Person',
      dataIndex: 'contactPerson',
      key: 'contactPerson',
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Phone',
      dataIndex: 'phone',
      key: 'phone',
    },
    {
      title: 'Type',
      dataIndex: 'type',
      key: 'type',
      render: (type) => {
        const colors = {
          RESEARCH_SITE: 'blue',
          SITE_NETWORK: 'green',
          AMC: 'purple',
          CRO: 'orange',
          SPONSOR: 'cyan'
        }
        return <Tag color={colors[type]}>{type.replace(/_/g, ' ')}</Tag>
      },
      filters: [
        { text: 'Research Site', value: 'RESEARCH_SITE' },
        { text: 'Site Network', value: 'SITE_NETWORK' },
        { text: 'AMC', value: 'AMC' },
        { text: 'CRO', value: 'CRO' },
        { text: 'Sponsor', value: 'SPONSOR' },
      ],
      onFilter: (value, record) => record.type === value,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const colors = {
          ACTIVE: 'success',
          POTENTIAL: 'warning',
          COMPLETED: 'default',
          ON_HOLD: 'error'
        }
        return <Tag color={colors[status]}>{status.replace(/_/g, ' ')}</Tag>
      },
      filters: [
        { text: 'Active', value: 'ACTIVE' },
        { text: 'Potential', value: 'POTENTIAL' },
        { text: 'Completed', value: 'COMPLETED' },
        { text: 'On Hold', value: 'ON_HOLD' },
      ],
      onFilter: (value, record) => record.status === value,
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
            title="Are you sure you want to delete this client?"
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
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title="Total Clients"
                value={stats.total}
                prefix={<UserOutlined />}
                valueStyle={{ color: '#3f8600' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title="Active Clients"
                value={stats.active}
                prefix={<UserOutlined />}
                valueStyle={{ color: '#1890ff' }}
              />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card>
              <Statistic
                title="Potential Clients"
                value={stats.potential}
                prefix={<UserOutlined />}
                valueStyle={{ color: '#faad14' }}
              />
            </Card>
          </Col>
        </Row>

        <Card
          title="Clients"
          extra={
            <Space>
              <Input
                placeholder="Search clients..."
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                onPressEnter={(e) => handleSearch(e.target.value)}
                style={{ width: 250 }}
                allowClear
                onClear={() => {
                  setSearchText('')
                  loadClients()
                }}
              />
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={() => showModal()}
              >
                Add Client
              </Button>
            </Space>
          }
        >
          <Table
            columns={columns}
            dataSource={clients}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} clients`,
            }}
          />
        </Card>

        <Modal
          title={editingClient ? 'Edit Client' : 'Add New Client'}
          open={isModalVisible}
          onCancel={handleCancel}
          footer={null}
          width={600}
        >
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
          >
            <Form.Item
              name="companyName"
              label="Company Name"
              rules={[
                { required: true, message: 'Please enter company name' },
                {
                  min: 2,
                  message: 'Company name should be at least 2 characters'
                },
                {
                  max: 100,
                  message: 'Company name should not exceed 100 characters'
                }
              ]}
            >
              <Input placeholder="Enter company name" />
            </Form.Item>

            <Form.Item
              name="contactPerson"
              label="Contact Person"
              rules={[{ required: true, message: 'Please enter contact person' }]}
            >
              <Input placeholder="Enter contact person name" />
            </Form.Item>

            <Form.Item
              name="email"
              label="Email"
              rules={[
                { required: true, message: 'Please enter email' },
                { type: 'email', message: 'Please enter a valid email' }
              ]}
            >
              <Input placeholder="Enter email address" />
            </Form.Item>

            <Form.Item
              name="phone"
              label="Phone"
              rules={[
                { required: true, message: 'Please enter phone number' },
                { 
                  pattern: /^[0-9]{10}$/, 
                  message: 'Phone number must be exactly 10 digits (e.g., 5551234567)' 
                }
              ]}
            >
              <Input placeholder="Enter 10-digit phone number (e.g., 5551234567)" maxLength={10} />
            </Form.Item>

            <Form.Item
              name="type"
              label="Client Type"
              rules={[{ required: true, message: 'Please select client type' }]}
            >
              <Select placeholder="Select client type">
                <Select.Option value="RESEARCH_SITE">Research Site</Select.Option>
                <Select.Option value="SITE_NETWORK">Site Network</Select.Option>
                <Select.Option value="AMC">AMC</Select.Option>
                <Select.Option value="CRO">CRO</Select.Option>
                <Select.Option value="SPONSOR">Sponsor</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="status"
              label="Status"
              rules={[{ required: true, message: 'Please select status' }]}
            >
              <Select placeholder="Select status">
                <Select.Option value="ACTIVE">Active</Select.Option>
                <Select.Option value="POTENTIAL">Potential</Select.Option>
                <Select.Option value="COMPLETED">Completed</Select.Option>
                <Select.Option value="ON_HOLD">On Hold</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="specialtyAreas"
              label="Specialty Areas"
            >
              <Input placeholder="e.g., Hypertension, Diabetes, CKD" />
            </Form.Item>

            <Form.Item
              name="notes"
              label="Notes"
            >
              <TextArea
                rows={4}
                placeholder="Additional notes about the client"
              />
            </Form.Item>

            <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
              <Space>
                <Button onClick={handleCancel}>
                  Cancel
                </Button>
                <Button type="primary" htmlType="submit">
                  {editingClient ? 'Update' : 'Create'}
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </Content>
  )
}

export default Clients
