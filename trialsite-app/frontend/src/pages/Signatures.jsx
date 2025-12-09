import { useState, useEffect } from 'react'
import {
  Layout,
  Card,
  Table,
  Button,
  Tag,
  Space,
  message,
  Typography
} from 'antd'
import {
  FileTextOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons'
import { signatureService } from '../services/signatureService'
import { useAuth } from '../context/AuthContext'
import dayjs from 'dayjs'

const { Content } = Layout
const { Title } = Typography

const Signatures = () => {
  const { user } = useAuth()
  const [signatures, setSignatures] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadPendingSignatures()
  }, [])

  const loadPendingSignatures = async () => {
    setLoading(true)
    try {
      const data = await signatureService.getPendingSignatures()
      setSignatures(data || [])
    } catch (error) {
      console.error('Error loading pending signatures:', error)
      message.error('Failed to load pending signatures')
    } finally {
      setLoading(false)
    }
  }

  const handleRefreshStatus = async () => {
    message.info('Refreshing signature statuses...')
    await loadPendingSignatures()
    message.success('Status refreshed')
  }

  const handleSignNow = async (signature) => {
    try {
      message.loading('Getting signing URL...', 0) // Show loading message
      const response = await signatureService.getSigningUrl(signature.id)
      message.destroy() // Remove loading message
      
      if (response.signingUrl) {
        // Open PandaDoc signing URL in new window
        window.open(response.signingUrl, '_blank')
        message.success('Opening signing page...')
        // Refresh after a delay to check if status changed
        setTimeout(() => {
          loadPendingSignatures()
        }, 3000)
      } else {
        message.error('Signing URL not available. Please contact administrator.')
      }
    } catch (error) {
      message.destroy() // Remove loading message
      const errorMessage = error.response?.data?.message || error.message || 'Failed to get signing URL'
      if (errorMessage.includes('still being processed') || errorMessage.includes('wait')) {
        message.warning({
          content: errorMessage + ' You can try again in a few moments.',
          duration: 5,
        })
      } else {
        message.error(errorMessage)
      }
    }
  }

  const getStatusTag = (status) => {
    const statusConfig = {
      PENDING: { color: 'default', icon: <ClockCircleOutlined />, text: 'Pending' },
      SENT: { color: 'processing', icon: <ClockCircleOutlined />, text: 'Sent' },
      VIEWED: { color: 'warning', icon: <ClockCircleOutlined />, text: 'Viewed' },
      SIGNED: { color: 'success', icon: <CheckCircleOutlined />, text: 'Signed' },
      DECLINED: { color: 'error', icon: <CloseCircleOutlined />, text: 'Declined' },
      EXPIRED: { color: 'default', icon: <ClockCircleOutlined />, text: 'Expired' },
      CANCELLED: { color: 'default', icon: <CloseCircleOutlined />, text: 'Cancelled' }
    }
    
    const config = statusConfig[status] || statusConfig.PENDING
    return (
      <Tag color={config.color} icon={config.icon}>
        {config.text}
      </Tag>
    )
  }

  const columns = [
    {
      title: 'Document Name',
      dataIndex: 'documentName',
      key: 'documentName',
      render: (text) => (
        <Space>
          <FileTextOutlined />
          {text}
        </Space>
      )
    },
    {
      title: 'Assigned By',
      dataIndex: 'assignedByUserName',
      key: 'assignedByUserName'
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => getStatusTag(status)
    },
    {
      title: 'Assigned Date',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => dayjs(date).format('MMM D, YYYY h:mm A')
    },
    {
      title: 'Signed Date',
      dataIndex: 'signedAt',
      key: 'signedAt',
      render: (date) => date ? dayjs(date).format('MMM D, YYYY h:mm A') : '-'
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => {
        const canSign = record.status === 'PENDING' || 
                       record.status === 'SENT' || 
                       record.status === 'VIEWED'
        
        return (
          <Space>
            {canSign && (
              <Button
                type="primary"
                onClick={() => handleSignNow(record)}
              >
                Sign Now
              </Button>
            )}
            {record.status === 'SIGNED' && (
              <Tag color="success">Completed</Tag>
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
          <Title level={3} style={{ margin: 0 }}>Documents Pending Signature</Title>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefreshStatus}
            loading={loading}
          >
            Refresh Status
          </Button>
        </div>
        
        <Card>
          <Table
            columns={columns}
            dataSource={signatures}
            rowKey="id"
            loading={loading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showTotal: (total) => `Total ${total} signature requests`
            }}
            locale={{
              emptyText: 'No documents pending signature'
            }}
          />
        </Card>
      </div>
    </Content>
  )
}

export default Signatures
