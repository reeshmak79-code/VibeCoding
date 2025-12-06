import { useState, useEffect } from 'react'
import {
  Layout,
  Card,
  Table,
  Button,
  Modal,
  Form,
  Select,
  Input,
  message,
  Tag,
  Space,
  Popconfirm,
  Typography
} from 'antd'
import {
  UserOutlined,
  PlusOutlined,
  EditOutlined,
  StopOutlined,
  CheckCircleOutlined,
  DeleteOutlined
} from '@ant-design/icons'
import { userService } from '../services/userService'
import { useAuth } from '../context/AuthContext'
import dayjs from 'dayjs'

const { Content } = Layout
const { Title } = Typography

const Users = () => {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [form] = Form.useForm()

  useEffect(() => {
    loadUsers()
  }, [])

  const loadUsers = async () => {
    setLoading(true)
    try {
      const data = await userService.getAllUsers()
      setUsers(data)
    } catch (error) {
      if (error.response?.status === 403) {
        message.error('Access denied. Admin only.')
      } else {
        message.error('Failed to load users')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleCreate = () => {
    setEditingUser(null)
    form.resetFields()
    form.setFieldsValue({ active: true })
    setModalVisible(true)
  }

  const handleEdit = (user) => {
    setEditingUser(user)
    form.setFieldsValue({
      fullName: user.fullName,
      email: user.email,
      username: user.username,
      role: user.role,
      active: user.active,
      password: '' // Don't pre-fill password
    })
    setModalVisible(true)
  }

  const handleSubmit = async (values) => {
    try {
      // Prepare data - convert empty password to null/undefined for backend
      const userData = { ...values }
      if (!userData.password || userData.password.trim() === '') {
        // Remove password field if empty (backend will auto-generate)
        delete userData.password
      }
      
      if (editingUser) {
        // Update existing user - only send password if provided
        if (!userData.password) {
          delete userData.password
        }
        await userService.updateUser(editingUser.id, userData)
        message.success('User updated successfully')
      } else {
        // Create new user
        const response = await userService.createUser(userData)
        if (response.message) {
          message.success(response.message, 10) // Show for 10 seconds
        } else {
          message.success('User created successfully')
        }
      }
      setModalVisible(false)
      form.resetFields()
      setEditingUser(null)
      loadUsers()
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Failed to save user'
      message.error(errorMsg)
    }
  }

  const handleDeactivate = async (id) => {
    try {
      await userService.deactivateUser(id)
      message.success('User deactivated successfully')
      loadUsers()
    } catch (error) {
      message.error('Failed to deactivate user')
    }
  }

  const handleActivate = async (id) => {
    try {
      await userService.activateUser(id)
      message.success('User activated successfully')
      loadUsers()
    } catch (error) {
      message.error('Failed to activate user')
    }
  }

  const handleDelete = async (id) => {
    try {
      await userService.deleteUser(id)
      message.success('User deleted successfully')
      loadUsers()
    } catch (error) {
      const errorMsg = error.response?.data?.message || 'Failed to delete user'
      message.error(errorMsg)
    }
  }

  const generatePassword = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
    let password = ''
    for (let i = 0; i < 12; i++) {
      password += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    // Set the password value in the form
    form.setFieldsValue({ password: password })
    // Force form to update the UI
    setTimeout(() => {
      form.validateFields(['password']).catch(() => {})
    }, 0)
    message.success('Password generated successfully!')
  }

  const roleColors = {
    ADMIN: 'red',
    USER: 'blue',
    DOCTOR: 'green',
    AUDITOR: 'orange',
    COORDINATOR: 'purple'
  }

  const columns = [
    {
      title: 'Name',
      dataIndex: 'fullName',
      key: 'fullName',
      render: (text, record) => (
        <Space>
          <UserOutlined />
          <strong>{text}</strong>
        </Space>
      )
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email'
    },
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username'
    },
    {
      title: 'Role',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={roleColors[role] || 'default'}>
          {role}
        </Tag>
      ),
      filters: [
        { text: 'Admin', value: 'ADMIN' },
        { text: 'User', value: 'USER' },
        { text: 'Doctor', value: 'DOCTOR' },
        { text: 'Auditor', value: 'AUDITOR' },
        { text: 'Coordinator', value: 'COORDINATOR' }
      ],
      onFilter: (value, record) => record.role === value
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      render: (active) => (
        <Tag color={active ? 'success' : 'error'}>
          {active ? 'Active' : 'Inactive'}
        </Tag>
      ),
      filters: [
        { text: 'Active', value: true },
        { text: 'Inactive', value: false }
      ],
      onFilter: (value, record) => record.active === value
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => dayjs(date).format('MMM D, YYYY'),
      sorter: (a, b) => new Date(a.createdAt) - new Date(b.createdAt)
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => {
        const isCurrentUser = currentUser?.id === record.id
        return (
          <Space>
            <Button
              type="link"
              icon={<EditOutlined />}
              onClick={() => handleEdit(record)}
            >
              Edit
            </Button>
            {record.active ? (
              <Popconfirm
                title="Deactivate User"
                description="Are you sure you want to deactivate this user?"
                onConfirm={() => handleDeactivate(record.id)}
                okText="Yes"
                cancelText="No"
              >
                <Button
                  type="link"
                  danger
                  icon={<StopOutlined />}
                >
                  Deactivate
                </Button>
              </Popconfirm>
            ) : (
              <Popconfirm
                title="Activate User"
                description="Are you sure you want to activate this user?"
                onConfirm={() => handleActivate(record.id)}
                okText="Yes"
                cancelText="No"
              >
                <Button
                  type="link"
                  icon={<CheckCircleOutlined />}
                >
                  Activate
                </Button>
              </Popconfirm>
            )}
            {!isCurrentUser && (
              <Popconfirm
                title="Delete User"
                description="Are you sure you want to delete this user? This action cannot be undone."
                onConfirm={() => handleDelete(record.id)}
                okText="Yes, Delete"
                cancelText="Cancel"
                okButtonProps={{ danger: true }}
              >
                <Button
                  type="link"
                  danger
                  icon={<DeleteOutlined />}
                >
                  Delete
                </Button>
              </Popconfirm>
            )}
          </Space>
        )
      }
    }
  ]

  return (
    <Content style={{ margin: '24px 16px 0' }}>
      <div style={{ padding: 24, minHeight: 360 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ margin: 0 }}>User Management</Title>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            Add User
          </Button>
        </div>

        <Card>
          <Table
            columns={columns}
            dataSource={users}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} users`
            }}
          />
        </Card>

        <Modal
          title={editingUser ? 'Edit User' : 'Add User'}
          open={modalVisible}
          onCancel={() => {
            setModalVisible(false)
            form.resetFields()
            setEditingUser(null)
          }}
          footer={null}
          width={600}
        >
          <Form
            form={form}
            layout="vertical"
            onFinish={handleSubmit}
          >
            <Form.Item
              name="fullName"
              label="Full Name"
              rules={[
                { required: true, message: 'Please enter full name' },
                { min: 2, message: 'Full name must be at least 2 characters' }
              ]}
            >
              <Input placeholder="Enter full name" />
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
              name="username"
              label="Username"
              rules={[
                { required: true, message: 'Please enter username' },
                { min: 3, message: 'Username must be at least 3 characters' }
              ]}
            >
              <Input placeholder="Enter username" />
            </Form.Item>

            <Form.Item
              name="password"
              label={editingUser ? 'New Password (leave empty to keep current)' : 'Temporary Password'}
              rules={editingUser ? [] : []}
              extra={!editingUser && "Leave empty to auto-generate, or enter a custom password"}
            >
              <Space.Compact style={{ width: '100%' }}>
                <Input.Password
                  placeholder={editingUser ? "Enter new password" : "Enter password or leave empty"}
                  style={{ flex: 1 }}
                />
                {!editingUser && (
                  <Button 
                    type="default"
                    onClick={(e) => {
                      e.preventDefault()
                      e.stopPropagation()
                      generatePassword()
                    }}
                  >
                    Generate
                  </Button>
                )}
              </Space.Compact>
            </Form.Item>

            <Form.Item
              name="role"
              label="Role"
              rules={[{ required: true, message: 'Please select a role' }]}
            >
              <Select placeholder="Select role">
                <Select.Option value="ADMIN">Admin</Select.Option>
                <Select.Option value="USER">User</Select.Option>
                <Select.Option value="DOCTOR">Doctor</Select.Option>
                <Select.Option value="AUDITOR">Auditor</Select.Option>
                <Select.Option value="COORDINATOR">Coordinator</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              name="active"
              label="Status"
            >
              <Select>
                <Select.Option value={true}>Active</Select.Option>
                <Select.Option value={false}>Inactive</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  {editingUser ? 'Update' : 'Create'}
                </Button>
                <Button
                  onClick={() => {
                    setModalVisible(false)
                    form.resetFields()
                    setEditingUser(null)
                  }}
                >
                  Cancel
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>
      </div>
    </Content>
  )
}

export default Users
