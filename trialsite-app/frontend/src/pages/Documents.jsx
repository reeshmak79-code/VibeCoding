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
  Upload,
  message,
  Tag,
  Space,
  Popconfirm,
  Statistic,
  Row,
  Col,
  Typography,
  Tree,
  Divider
} from 'antd'
import {
  UploadOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FileOutlined,
  FolderOutlined,
  FolderAddOutlined,
  LockOutlined,
  PlusOutlined
} from '@ant-design/icons'
import { documentService } from '../services/documentService'
import { projectService } from '../services/projectService'
import { folderService } from '../services/folderService'
import { permissionService } from '../services/permissionService'
import dayjs from 'dayjs'

const { Content } = Layout
const { Title } = Typography
const { TextArea } = Input

const Documents = () => {
  const [documents, setDocuments] = useState([])
  const [projects, setProjects] = useState([])
  const [folders, setFolders] = useState([])
  const [stats, setStats] = useState({ total: 0, totalSize: 0, byType: {} })
  const [loading, setLoading] = useState(false)
  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [folderModalVisible, setFolderModalVisible] = useState(false)
  const [permissionModalVisible, setPermissionModalVisible] = useState(false)
  const [selectedProject, setSelectedProject] = useState(null)
  const [selectedFolder, setSelectedFolder] = useState(null)
  const [uploadForm] = Form.useForm()
  const [folderForm] = Form.useForm()
  const [permissionForm] = Form.useForm()
  const [fileList, setFileList] = useState([])
  const [permissionTarget, setPermissionTarget] = useState({ type: null, id: null }) // 'document' or 'folder'

  useEffect(() => {
    loadProjects()
  }, [])

  useEffect(() => {
    if (selectedProject) {
      loadFolders()
      loadDocuments(selectedProject, selectedFolder)
    }
  }, [selectedProject, selectedFolder])

  const loadProjects = async () => {
    try {
      const data = await projectService.getAllProjects()
      setProjects(data)
    } catch (error) {
      message.error('Failed to load projects')
    }
  }

  const loadFolders = async () => {
    if (!selectedProject) return
    try {
      const data = await folderService.getProjectFolders(selectedProject)
      setFolders(data)
    } catch (error) {
      message.error('Failed to load folders')
    }
  }

  const loadDocuments = async (projectId, folderId = null) => {
    if (!projectId) return
    
    setLoading(true)
    try {
      let docs
      if (folderId) {
        docs = await documentService.getFolderDocuments(folderId)
      } else {
        docs = await documentService.getProjectDocuments(projectId)
      }
      
      const statsData = await documentService.getProjectDocumentStats(projectId)
      setDocuments(docs)
      setStats(statsData)
    } catch (error) {
      message.error('Failed to load documents')
    } finally {
      setLoading(false)
    }
  }

  const handleProjectChange = (projectId) => {
    setSelectedProject(projectId)
    setSelectedFolder(null)
  }

  const handleFolderSelect = (folderId) => {
    setSelectedFolder(folderId)
  }

  const handleCreateFolder = async (values) => {
    try {
      await folderService.createFolder({
        ...values,
        projectId: selectedProject
      })
      message.success('Folder created successfully')
      setFolderModalVisible(false)
      folderForm.resetFields()
      loadFolders()
    } catch (error) {
      message.error('Failed to create folder')
    }
  }

  const handleDeleteFolder = async (id) => {
    try {
      await folderService.deleteFolder(id)
      message.success('Folder deleted successfully')
      if (selectedFolder === id) {
        setSelectedFolder(null)
      }
      loadFolders()
    } catch (error) {
      message.error(error.response?.data?.message || 'Failed to delete folder')
    }
  }

  const handleUpload = async (values) => {
    if (fileList.length === 0) {
      message.error('Please select a file')
      return
    }

    try {
      const file = fileList[0].originFileObj
      await documentService.uploadDocument(
        file,
        values.projectId,
        values.documentType,
        values.description,
        values.folderId || null
      )
      
      message.success('Document uploaded successfully')
      setUploadModalVisible(false)
      uploadForm.resetFields()
      setFileList([])
      loadDocuments(selectedProject, selectedFolder)
    } catch (error) {
      message.error('Failed to upload document')
    }
  }

  const handleDownload = async (record) => {
    try {
      await documentService.downloadDocument(record.id, record.originalFileName)
      message.success('Download started')
    } catch (error) {
      message.error('Failed to download document')
    }
  }

  const handleDelete = async (id) => {
    try {
      await documentService.deleteDocument(id)
      message.success('Document deleted successfully')
      loadDocuments(selectedProject, selectedFolder)
    } catch (error) {
      message.error('Failed to delete document')
    }
  }

  const handleGrantPermission = async (values) => {
    try {
      const permissionData = {
        permissionType: values.permissionType,
        [permissionTarget.type === 'document' ? 'documentId' : 'folderId']: permissionTarget.id,
        [values.grantType === 'user' ? 'userId' : 'role']: values.grantType === 'user' ? values.userId : values.role
      }
      
      await permissionService.grantPermission(permissionData)
      message.success('Permission granted successfully')
      setPermissionModalVisible(false)
      permissionForm.resetFields()
      setPermissionTarget({ type: null, id: null })
    } catch (error) {
      message.error('Failed to grant permission')
    }
  }

  const openPermissionModal = (type, id) => {
    setPermissionTarget({ type, id })
    setPermissionModalVisible(true)
  }

  // Convert folders to tree data
  const buildTreeData = (folders, parentId = null) => {
    return folders
      .filter(f => (parentId === null ? !f.parentFolderId : f.parentFolderId === parentId))
      .map(folder => ({
        title: (
          <Space>
            <FolderOutlined />
            <span>{folder.folderName}</span>
            <span style={{ color: '#999', fontSize: 12 }}>
              ({folder.documentCount} docs)
            </span>
            <Button
              type="link"
              size="small"
              icon={<LockOutlined />}
              onClick={(e) => {
                e.stopPropagation()
                openPermissionModal('folder', folder.id)
              }}
            >
              Permissions
            </Button>
            <Popconfirm
              title="Delete Folder"
              description="Are you sure? This will delete the folder and all its contents."
              onConfirm={(e) => {
                e.stopPropagation()
                handleDeleteFolder(folder.id)
              }}
              okText="Yes"
              cancelText="No"
            >
              <Button
                type="link"
                danger
                size="small"
                icon={<DeleteOutlined />}
                onClick={(e) => e.stopPropagation()}
              >
                Delete
              </Button>
            </Popconfirm>
          </Space>
        ),
        key: folder.id,
        isLeaf: folder.subFolderCount === 0,
        children: buildTreeData(folders, folder.id)
      }))
  }

  const documentTypeColors = {
    CONTRACT: 'blue',
    PROPOSAL: 'green',
    DELIVERABLE: 'purple',
    REPORT: 'orange',
    TRAINING_MATERIAL: 'cyan',
    OTHER: 'default'
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  const columns = [
    {
      title: 'File Name',
      dataIndex: 'originalFileName',
      key: 'originalFileName',
      render: (text, record) => (
        <Space>
          <FileOutlined />
          {text}
          {record.folderName && (
            <Tag color="default" icon={<FolderOutlined />}>
              {record.folderName}
            </Tag>
          )}
        </Space>
      )
    },
    {
      title: 'Document Type',
      dataIndex: 'documentType',
      key: 'documentType',
      render: (type) => (
        <Tag color={documentTypeColors[type]}>
          {type.replace(/_/g, ' ')}
        </Tag>
      ),
      filters: [
        { text: 'Contract', value: 'CONTRACT' },
        { text: 'Proposal', value: 'PROPOSAL' },
        { text: 'Deliverable', value: 'DELIVERABLE' },
        { text: 'Report', value: 'REPORT' },
        { text: 'Training Material', value: 'TRAINING_MATERIAL' },
        { text: 'Other', value: 'OTHER' }
      ],
      onFilter: (value, record) => record.documentType === value
    },
    {
      title: 'Size',
      dataIndex: 'fileSize',
      key: 'fileSize',
      render: (size) => formatFileSize(size),
      sorter: (a, b) => a.fileSize - b.fileSize
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true
    },
    {
      title: 'Uploaded By',
      dataIndex: 'uploadedBy',
      key: 'uploadedBy'
    },
    {
      title: 'Uploaded Date',
      dataIndex: 'uploadedAt',
      key: 'uploadedAt',
      render: (date) => dayjs(date).format('MMM D, YYYY h:mm A'),
      sorter: (a, b) => new Date(a.uploadedAt) - new Date(b.uploadedAt)
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<DownloadOutlined />}
            onClick={() => handleDownload(record)}
          >
            Download
          </Button>
          <Button
            type="link"
            icon={<LockOutlined />}
            onClick={() => openPermissionModal('document', record.id)}
          >
            Permissions
          </Button>
          <Popconfirm
            title="Delete Document"
            description="Are you sure you want to delete this document?"
            onConfirm={() => handleDelete(record.id)}
            okText="Yes"
            cancelText="No"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              Delete
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <Content style={{ margin: '24px 16px 0' }}>
      <div style={{ padding: 24, minHeight: 360 }}>
        <Title level={3} style={{ marginBottom: 24 }}>Document Management</Title>

        <Card style={{ marginBottom: 16 }}>
          <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div>
              <label style={{ marginRight: 8 }}>Select Project:</label>
              <Select
                style={{ width: 300 }}
                placeholder="Choose a project"
                onChange={handleProjectChange}
                value={selectedProject}
                showSearch
                optionFilterProp="children"
              >
                {projects.map(project => (
                  <Select.Option key={project.id} value={project.id}>
                    {project.projectName}
                  </Select.Option>
                ))}
              </Select>
              {selectedProject && (
                <>
                  <Button
                    type="primary"
                    icon={<UploadOutlined />}
                    onClick={() => setUploadModalVisible(true)}
                    style={{ marginLeft: 16 }}
                  >
                    Upload Document
                  </Button>
                  <Button
                    icon={<FolderAddOutlined />}
                    onClick={() => setFolderModalVisible(true)}
                    style={{ marginLeft: 8 }}
                  >
                    Create Folder
                  </Button>
                </>
              )}
            </div>

            {selectedProject && (
              <Row gutter={16}>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Total Documents"
                      value={stats.total}
                      prefix={<FolderOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Total Size"
                      value={formatFileSize(stats.totalSize)}
                      prefix={<FileOutlined />}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic
                      title="Contracts"
                      value={stats.byType?.CONTRACT || 0}
                      valueStyle={{ color: '#1890ff' }}
                    />
                  </Card>
                </Col>
              </Row>
            )}
          </Space>
        </Card>

        {selectedProject ? (
          <Row gutter={16}>
            <Col span={6}>
              <Card title="Folders" size="small">
                <Button
                  type="dashed"
                  block
                  icon={<PlusOutlined />}
                  onClick={() => setFolderModalVisible(true)}
                  style={{ marginBottom: 16 }}
                >
                  New Folder
                </Button>
                <Tree
                  treeData={buildTreeData(folders)}
                  onSelect={(selectedKeys) => {
                    if (selectedKeys.length > 0) {
                      handleFolderSelect(selectedKeys[0])
                    } else {
                      setSelectedFolder(null)
                    }
                  }}
                  selectedKeys={selectedFolder ? [selectedFolder] : []}
                  defaultExpandAll
                />
                {!selectedFolder && (
                  <Button
                    type="link"
                    block
                    onClick={() => setSelectedFolder(null)}
                    style={{ marginTop: 8 }}
                  >
                    Show All Documents
                  </Button>
                )}
              </Card>
            </Col>
            <Col span={18}>
              <Card>
                <div style={{ marginBottom: 16 }}>
                  {selectedFolder ? (
                    <Space>
                      <FolderOutlined />
                      <strong>Viewing folder contents</strong>
                      <Button
                        type="link"
                        onClick={() => setSelectedFolder(null)}
                      >
                        Show All Documents
                      </Button>
                    </Space>
                  ) : (
                    <Space>
                      <FileOutlined />
                      <strong>All Documents</strong>
                    </Space>
                  )}
                </div>
                <Table
                  columns={columns}
                  dataSource={documents}
                  rowKey="id"
                  loading={loading}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showTotal: (total) => `Total ${total} documents`
                  }}
                />
              </Card>
            </Col>
          </Row>
        ) : (
          <Card>
            <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
              <FolderOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <p>Select a project to view and manage documents</p>
            </div>
          </Card>
        )}

        {/* Upload Document Modal */}
        <Modal
          title="Upload Document"
          open={uploadModalVisible}
          onCancel={() => {
            setUploadModalVisible(false)
            uploadForm.resetFields()
            setFileList([])
          }}
          footer={null}
        >
          <Form form={uploadForm} layout="vertical" onFinish={handleUpload}>
            <Form.Item
              name="projectId"
              label="Project"
              initialValue={selectedProject}
              rules={[{ required: true, message: 'Please select a project' }]}
            >
              <Select placeholder="Select project">
                {projects.map(project => (
                  <Select.Option key={project.id} value={project.id}>
                    {project.projectName}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item name="folderId" label="Folder (Optional)">
              <Select placeholder="Select folder (or leave empty for root)">
                <Select.Option value={null}>Root (No folder)</Select.Option>
                {folders.map(folder => (
                  <Select.Option key={folder.id} value={folder.id}>
                    {folder.folderName}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item
              name="documentType"
              label="Document Type"
              rules={[{ required: true, message: 'Please select document type' }]}
            >
              <Select placeholder="Select document type">
                <Select.Option value="CONTRACT">Contract</Select.Option>
                <Select.Option value="PROPOSAL">Proposal</Select.Option>
                <Select.Option value="DELIVERABLE">Deliverable</Select.Option>
                <Select.Option value="REPORT">Report</Select.Option>
                <Select.Option value="TRAINING_MATERIAL">Training Material</Select.Option>
                <Select.Option value="OTHER">Other</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item name="description" label="Description">
              <TextArea rows={3} placeholder="Enter document description" />
            </Form.Item>

            <Form.Item
              label="File"
              rules={[{ required: true, message: 'Please select a file' }]}
            >
              <Upload
                maxCount={1}
                fileList={fileList}
                onChange={({ fileList }) => setFileList(fileList)}
                beforeUpload={() => false}
              >
                <Button icon={<UploadOutlined />}>Select File</Button>
              </Upload>
              <p style={{ marginTop: 8, color: '#999', fontSize: 12 }}>
                Max file size: 10MB
              </p>
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  Upload
                </Button>
                <Button
                  onClick={() => {
                    setUploadModalVisible(false)
                    uploadForm.resetFields()
                    setFileList([])
                  }}
                >
                  Cancel
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>

        {/* Create Folder Modal */}
        <Modal
          title="Create Folder"
          open={folderModalVisible}
          onCancel={() => {
            setFolderModalVisible(false)
            folderForm.resetFields()
          }}
          footer={null}
        >
          <Form form={folderForm} layout="vertical" onFinish={handleCreateFolder}>
            <Form.Item
              name="folderName"
              label="Folder Name"
              rules={[{ required: true, message: 'Please enter folder name' }]}
            >
              <Input placeholder="Enter folder name" />
            </Form.Item>

            <Form.Item name="description" label="Description">
              <TextArea rows={3} placeholder="Enter folder description" />
            </Form.Item>

            <Form.Item name="parentFolderId" label="Parent Folder (Optional)">
              <Select placeholder="Select parent folder (or leave empty for root)">
                <Select.Option value={null}>Root (No parent)</Select.Option>
                {folders.map(folder => (
                  <Select.Option key={folder.id} value={folder.id}>
                    {folder.folderName}
                  </Select.Option>
                ))}
              </Select>
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  Create
                </Button>
                <Button
                  onClick={() => {
                    setFolderModalVisible(false)
                    folderForm.resetFields()
                  }}
                >
                  Cancel
                </Button>
              </Space>
            </Form.Item>
          </Form>
        </Modal>

        {/* Permission Management Modal */}
        <Modal
          title={`Manage Permissions - ${permissionTarget.type === 'document' ? 'Document' : 'Folder'}`}
          open={permissionModalVisible}
          onCancel={() => {
            setPermissionModalVisible(false)
            permissionForm.resetFields()
            setPermissionTarget({ type: null, id: null })
          }}
          footer={null}
        >
          <Form form={permissionForm} layout="vertical" onFinish={handleGrantPermission}>
            <Form.Item
              name="grantType"
              label="Grant Permission To"
              rules={[{ required: true, message: 'Please select grant type' }]}
            >
              <Select placeholder="Select type">
                <Select.Option value="role">Role (e.g., AUDITOR)</Select.Option>
                <Select.Option value="user">Specific User</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item
              noStyle
              shouldUpdate={(prevValues, currentValues) => prevValues.grantType !== currentValues.grantType}
            >
              {({ getFieldValue }) => {
                const grantType = getFieldValue('grantType')
                if (grantType === 'role') {
                  return (
                    <Form.Item
                      name="role"
                      label="Role"
                      rules={[{ required: true, message: 'Please select a role' }]}
                    >
                      <Select placeholder="Select role">
                        <Select.Option value="ADMIN">Admin</Select.Option>
                        <Select.Option value="USER">User</Select.Option>
                        <Select.Option value="AUDITOR">Auditor</Select.Option>
                      </Select>
                    </Form.Item>
                  )
                } else if (grantType === 'user') {
                  return (
                    <Form.Item
                      name="userId"
                      label="User"
                      rules={[{ required: true, message: 'Please select a user' }]}
                    >
                      <Select placeholder="Select user" showSearch>
                        {/* You'll need to load users here */}
                        <Select.Option value={1}>User 1</Select.Option>
                      </Select>
                    </Form.Item>
                  )
                }
                return null
              }}
            </Form.Item>

            <Form.Item
              name="permissionType"
              label="Permission Type"
              rules={[{ required: true, message: 'Please select permission type' }]}
            >
              <Select placeholder="Select permission">
                <Select.Option value="READ">Read Only</Select.Option>
                <Select.Option value="WRITE">Read & Write</Select.Option>
                <Select.Option value="DELETE">Read, Write & Delete</Select.Option>
              </Select>
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit">
                  Grant Permission
                </Button>
                <Button
                  onClick={() => {
                    setPermissionModalVisible(false)
                    permissionForm.resetFields()
                    setPermissionTarget({ type: null, id: null })
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

export default Documents
