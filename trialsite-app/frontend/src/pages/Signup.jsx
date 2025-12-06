import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, Typography, message } from 'antd'
import { UserOutlined, LockOutlined, MailOutlined, IdcardOutlined } from '@ant-design/icons'
import { useAuth } from '../context/AuthContext'

const { Title, Text } = Typography

const Signup = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { signup } = useAuth()

  const onFinish = async (values) => {
    setLoading(true)
    const result = await signup(
      values.username,
      values.email,
      values.password,
      values.fullName
    )
    setLoading(false)

    if (result.success) {
      message.success('Account created successfully! Please login.')
      navigate('/login')
    } else {
      message.error(result.message || 'Signup failed')
    }
  }

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    }}>
      <Card style={{ width: 400, boxShadow: '0 4px 6px rgba(0,0,0,0.1)' }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <Title level={2} style={{ marginBottom: 8 }}>Create Account</Title>
          <Text type="secondary">Sign up for TrialSite Solutions</Text>
        </div>

        <Form
          name="signup"
          onFinish={onFinish}
          autoComplete="off"
          layout="vertical"
        >
          <Form.Item
            name="fullName"
            rules={[{ required: true, message: 'Please input your full name!' }]}
          >
            <Input
              prefix={<IdcardOutlined />}
              placeholder="Full Name"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="username"
            rules={[
              { required: true, message: 'Please input your username!' },
              { min: 3, message: 'Username must be at least 3 characters!' }
            ]}
          >
            <Input
              prefix={<UserOutlined />}
              placeholder="Username"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="email"
            rules={[
              { required: true, message: 'Please input your email!' },
              { type: 'email', message: 'Please enter a valid email!' }
            ]}
          >
            <Input
              prefix={<MailOutlined />}
              placeholder="Email"
              size="large"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: 'Please input your password!' },
              { min: 6, message: 'Password must be at least 6 characters!' }
            ]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="Password"
              size="large"
            />
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              size="large"
              block
              loading={loading}
            >
              Sign Up
            </Button>
          </Form.Item>

          <div style={{ textAlign: 'center' }}>
            <Text>Already have an account? </Text>
            <Link to="/login">Login</Link>
          </div>
        </Form>
      </Card>
    </div>
  )
}

export default Signup
