import { useState, useEffect, useMemo, useCallback } from 'react'
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
  FileTextOutlined,
  FolderOutlined,
  FolderAddOutlined,
  LockOutlined,
  CloseOutlined,
  PlusOutlined
} from '@ant-design/icons'
import { documentService } from '../services/documentService'
import { projectService } from '../services/projectService'
import { folderService } from '../services/folderService'
import { permissionService } from '../services/permissionService'
import { userService } from '../services/userService'
import { signatureService } from '../services/signatureService'
import { useAuth } from '../context/AuthContext'
import dayjs from 'dayjs'

const { Content } = Layout
const { Title } = Typography
const { TextArea } = Input

const Documents = () => {
  console.log('=== FRONTEND DEBUG: Documents component rendering')
  const { user } = useAuth()
  console.log('=== FRONTEND DEBUG: User from auth:', user)
  
  // Safety check - if user is not loaded yet, show loading state
  if (!user) {
    console.log('=== FRONTEND DEBUG: No user, showing loading state')
    return (
      <Content style={{ margin: '24px 16px 0' }}>
        <div style={{ padding: 24, minHeight: 360, textAlign: 'center' }}>
          <Title level={3}>Loading...</Title>
        </div>
      </Content>
    )
  }
  
  const isAdmin = user?.role === 'ADMIN'
  const isAdminOrDoctor = user?.role === 'ADMIN' || user?.role === 'DOCTOR'
  const hasRestrictedAccess = !isAdminOrDoctor // USER, AUDITOR, COORDINATOR, etc. - need permission filtering
  console.log('=== FRONTEND DEBUG: User role:', user?.role, 'isAdmin:', isAdmin, 'isAdminOrDoctor:', isAdminOrDoctor, 'hasRestrictedAccess:', hasRestrictedAccess)
  const [documents, setDocuments] = useState([])
  const [userPermissions, setUserPermissions] = useState([]) // Store user's permissions
  const [documentPermissionsMap, setDocumentPermissionsMap] = useState({}) // Map of documentId -> permissions array
  const [projects, setProjects] = useState([])
  const [folders, setFolders] = useState([])
  const [users, setUsers] = useState([])
  const [loadingUsers, setLoadingUsers] = useState(false)
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
  const [existingPermissions, setExistingPermissions] = useState([]) // Current permissions for the target
  const [signatureModalVisible, setSignatureModalVisible] = useState(false)
  const [selectedDocumentForSignature, setSelectedDocumentForSignature] = useState(null)
  const [signatureForm] = Form.useForm()
  const [assigningSignature, setAssigningSignature] = useState(false)

  useEffect(() => {
    if (hasRestrictedAccess) {
      loadUserPermissions()
    } else {
      loadProjects()
    }
  }, [hasRestrictedAccess])

  useEffect(() => {
    if (selectedProject) {
      loadFolders()
      loadDocuments(selectedProject, selectedFolder)
    } else if (hasRestrictedAccess && userPermissions && userPermissions.length > 0) {
      // For restricted users, if no project selected, show all permitted documents
      loadAllPermittedDocuments()
    }
  }, [selectedProject, selectedFolder])
  
  // Reload permissions when documents change (for ADMIN/DOCTOR)
  useEffect(() => {
    console.log('=== FRONTEND DEBUG: useEffect for permissions triggered', {
      isAdminOrDoctor,
      documentsLength: documents?.length,
      selectedProject
    })
    
    if (isAdminOrDoctor && documents && documents.length > 0) {
      const docIds = documents.map(doc => doc.id).filter(id => id != null)
      console.log('=== FRONTEND DEBUG: Document IDs for permissions:', docIds)
      
      if (docIds.length > 0) {
        // Use a ref or delay to avoid race conditions
        const timer = setTimeout(() => {
          console.log('=== FRONTEND DEBUG: Calling loadDocumentPermissions with:', docIds)
          loadDocumentPermissions(docIds).catch(error => {
            console.error('=== FRONTEND DEBUG: Error loading document permissions:', error)
            console.error('=== FRONTEND DEBUG: Permission error details:', error.response?.data || error.message)
          })
        }, 50)
        return () => {
          console.log('=== FRONTEND DEBUG: Clearing permissions timer')
          clearTimeout(timer)
        }
      }
    } else {
      console.log('=== FRONTEND DEBUG: Skipping permissions load', {
        isAdminOrDoctor,
        hasDocuments: documents && documents.length > 0,
        documentsLength: documents?.length
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [documents.length, selectedProject, isAdminOrDoctor])

  const loadUserPermissions = async () => {
    try {
      // Get all permissions for this user
      const permissions = await permissionService.getUserPermissions(user.id)
      console.log('Loaded permissions for user:', user.id, 'Count:', permissions?.length || 0)
      console.log('Permissions:', permissions)
      setUserPermissions(permissions || [])
      // Load all projects (backend will filter documents)
      await loadProjects()
      // If user has permissions, load all permitted documents
      if (permissions && permissions.length > 0) {
        await loadAllPermittedDocuments(permissions)
      } else {
        setDocuments([])
      }
    } catch (error) {
      console.error('Failed to load permissions:', error)
      message.error('Failed to load your document permissions: ' + (error.response?.data?.message || error.message))
      setUserPermissions([])
    }
  }

  const loadProjects = async () => {
    try {
      const data = await projectService.getAllProjects()
      setProjects(data || [])
    } catch (error) {
      console.error('Error loading projects:', error)
      message.error('Failed to load projects')
      setProjects([])
    }
  }

  const loadFolders = async () => {
    if (!selectedProject) return
    try {
      const data = await folderService.getProjectFolders(selectedProject)
      setFolders(data || [])
    } catch (error) {
      console.error('Error loading folders:', error)
      message.error('Failed to load folders')
      setFolders([])
    }
  }

  const loadDocuments = async (projectId, folderId = null) => {
    console.log('=== FRONTEND DEBUG: loadDocuments called', { projectId, folderId, hasRestrictedAccess })
    
    if (!projectId) {
      console.log('=== FRONTEND DEBUG: No projectId, checking restricted access')
      // For restricted users, if no project selected, show all documents they have permission to
      if (hasRestrictedAccess) {
        await loadAllPermittedDocuments()
      }
      return
    }
    
    console.log('=== FRONTEND DEBUG: Setting loading to true')
    setLoading(true)
    try {
      console.log('=== FRONTEND DEBUG: About to fetch documents', { projectId, folderId })
      let docs
      if (folderId) {
        console.log('=== FRONTEND DEBUG: Fetching folder documents for folderId:', folderId)
        docs = await documentService.getFolderDocuments(folderId)
        console.log('=== FRONTEND DEBUG: Folder documents response:', docs)
      } else {
        console.log('=== FRONTEND DEBUG: Fetching project documents for projectId:', projectId)
        docs = await documentService.getProjectDocuments(projectId)
        console.log('=== FRONTEND DEBUG: Project documents response:', docs)
        console.log('=== FRONTEND DEBUG: Response type:', typeof docs)
        console.log('=== FRONTEND DEBUG: Is array?', Array.isArray(docs))
        console.log('=== FRONTEND DEBUG: Response length:', docs?.length)
      }
      
      // Backend already filters for restricted users
      console.log('=== FRONTEND DEBUG: Raw documents received:', docs?.length || 0, 'for project:', projectId, 'folder:', folderId)
      console.log('=== FRONTEND DEBUG: Full response:', JSON.stringify(docs, null, 2))
      
      // Filter out any null or invalid documents (safety check)
      console.log('=== FRONTEND DEBUG: Starting document filtering')
      const docsList = (docs || []).filter((doc, index) => {
        console.log(`=== FRONTEND DEBUG: Filtering document ${index}:`, doc)
        if (!doc || !doc.id) {
          console.warn('=== FRONTEND DEBUG: Invalid document found (no doc or no id):', doc)
          return false
        }
        // Ensure documentType is valid
        if (!doc.documentType) {
          console.warn('=== FRONTEND DEBUG: Document missing documentType, setting to OTHER:', doc.id)
          doc.documentType = 'OTHER'
        }
        console.log(`=== FRONTEND DEBUG: Document ${index} passed filter:`, doc.id, doc.originalFileName)
        return true
      })
      
      console.log('=== FRONTEND DEBUG: Filtered documents count:', docsList.length)
      console.log('=== FRONTEND DEBUG: Setting documents state')
      setDocuments(docsList)
      console.log('=== FRONTEND DEBUG: Documents state set successfully')
      
      // Load stats separately with error handling
      try {
        console.log('=== FRONTEND DEBUG: Loading stats for projectId:', projectId)
        const statsData = await documentService.getProjectDocumentStats(projectId)
        console.log('=== FRONTEND DEBUG: Stats received:', statsData)
        setStats(statsData || { total: 0, totalSize: 0, byType: {} })
      } catch (statsError) {
        console.error('=== FRONTEND DEBUG: Error loading stats (non-critical):', statsError)
        console.error('=== FRONTEND DEBUG: Stats error details:', statsError.response?.data || statsError.message)
        // Don't fail the whole page if stats fail
        setStats({ total: docsList.length, totalSize: 0, byType: {} })
      }
      
      console.log('=== FRONTEND DEBUG: loadDocuments completed successfully')
      // Load permissions for all documents (only for ADMIN and DOCTOR)
      // Note: This will be triggered by useEffect when documents change
    } catch (error) {
      console.error('=== FRONTEND DEBUG: ERROR in loadDocuments')
      console.error('=== FRONTEND DEBUG: Error object:', error)
      console.error('=== FRONTEND DEBUG: Error type:', error?.constructor?.name)
      console.error('=== FRONTEND DEBUG: Error message:', error?.message)
      console.error('=== FRONTEND DEBUG: Error response:', error?.response)
      console.error('=== FRONTEND DEBUG: Error response data:', error?.response?.data)
      console.error('=== FRONTEND DEBUG: Error response status:', error?.response?.status)
      console.error('=== FRONTEND DEBUG: Error stack:', error?.stack)
      message.error('Failed to load documents: ' + (error.response?.data?.message || error.message))
      setDocuments([])
      setStats({ total: 0, totalSize: 0, byType: {} })
    } finally {
      console.log('=== FRONTEND DEBUG: Setting loading to false')
      setLoading(false)
    }
  }

  const loadDocumentPermissions = async (documentIds) => {
    console.log('=== FRONTEND DEBUG: loadDocumentPermissions called', {
      isAdminOrDoctor,
      documentIds,
      documentIdsLength: documentIds?.length
    })
    
    if (!isAdminOrDoctor || !documentIds || documentIds.length === 0) {
      console.log('=== FRONTEND DEBUG: Skipping permissions load - conditions not met')
      return
    }
    
    try {
      console.log('=== FRONTEND DEBUG: Starting to load permissions for', documentIds.length, 'documents')
      // Load all permissions first, then update state once
      const permissionsMap = {}
      
      for (const docId of documentIds) {
        try {
          console.log('=== FRONTEND DEBUG: Loading permissions for document ID:', docId)
          // Get ONLY direct document permissions (NOT folder permissions)
          // Folder permissions apply to access control but should NOT be displayed as document permissions
          const docPerms = await permissionService.getDocumentPermissions(docId)
          console.log('=== FRONTEND DEBUG: Permissions for document', docId, ':', docPerms)
          permissionsMap[docId] = docPerms || []
        } catch (error) {
          console.error(`=== FRONTEND DEBUG: Error loading permissions for document ${docId}:`, error)
          console.error('=== FRONTEND DEBUG: Permission error details:', error.response?.data || error.message)
          // Don't set empty array - preserve existing permissions
          permissionsMap[docId] = []
        }
      }
      
      console.log('=== FRONTEND DEBUG: All permissions loaded, updating state with:', permissionsMap)
      // Update state once with all new permissions, preserving existing ones
      setDocumentPermissionsMap(prevMap => {
        const newMap = {
          ...prevMap, // Keep existing permissions for documents not being reloaded
          ...permissionsMap // Update with new permissions
        }
        console.log('=== FRONTEND DEBUG: New permissions map:', newMap)
        return newMap
      })
      console.log('=== FRONTEND DEBUG: Permissions state updated successfully')
    } catch (error) {
      console.error('=== FRONTEND DEBUG: FATAL ERROR in loadDocumentPermissions:', error)
      console.error('=== FRONTEND DEBUG: Error type:', error?.constructor?.name)
      console.error('=== FRONTEND DEBUG: Error message:', error?.message)
      console.error('=== FRONTEND DEBUG: Error stack:', error?.stack)
    }
  }

  const loadAllPermittedDocuments = async (permissions = null) => {
    const permsToUse = permissions || userPermissions
    if (!hasRestrictedAccess || !permsToUse || permsToUse.length === 0) {
      console.log('No permissions to load documents')
      setDocuments([])
      return
    }
    
    setLoading(true)
    try {
      // Get all projects first
      const allProjects = await projectService.getAllProjects()
      console.log('Loading documents from', allProjects.length, 'projects')
      
      // Load documents from all projects and combine
      const allDocs = []
      for (const project of allProjects) {
        try {
          const docs = await documentService.getProjectDocuments(project.id)
          if (docs && docs.length > 0) {
            console.log(`Found ${docs.length} documents in project ${project.projectName}`)
            allDocs.push(...docs)
          }
        } catch (error) {
          console.error(`Error loading documents for project ${project.id}:`, error)
        }
      }
      
      console.log('Loaded all permitted documents:', allDocs.length)
      setDocuments(allDocs)
      setStats({ 
        total: allDocs.length, 
        totalSize: allDocs.reduce((sum, doc) => sum + (doc.fileSize || 0), 0),
        byType: {}
      })
      
      // Load permissions for all documents (only for ADMIN and DOCTOR)
      if (isAdminOrDoctor && allDocs.length > 0) {
        await loadDocumentPermissions(allDocs.map(doc => doc.id))
      }
    } catch (error) {
      console.error('Error loading all permitted documents:', error)
      message.error('Failed to load documents: ' + (error.response?.data?.message || error.message))
      setDocuments([])
    } finally {
      setLoading(false)
    }
  }

  const filterDocumentsByPermissions = (docs, permissions) => {
    if (!hasRestrictedAccess || !permissions || permissions.length === 0) {
      return []
    }
    
    // Get document IDs and folder IDs that user has permission to
    const allowedDocumentIds = new Set()
    const allowedFolderIds = new Set()
    
    permissions.forEach(perm => {
      if (perm.documentId) {
        allowedDocumentIds.add(perm.documentId)
      }
      if (perm.folderId) {
        allowedFolderIds.add(perm.folderId)
      }
    })
    
    // Filter documents: user has permission to document OR document is in folder with permission
    return docs.filter(doc => {
      return allowedDocumentIds.has(doc.id) || 
             (doc.folderId && allowedFolderIds.has(doc.folderId))
    })
  }

  const checkDocumentPermission = useCallback((documentId, permissionType, folderId = null) => {
    if (!hasRestrictedAccess || !userPermissions) return false
    
    // Check direct document permission
    const docPermission = userPermissions.find(p => {
      if (p.documentId === documentId) {
        // READ permission allows READ
        // WRITE permission allows READ and WRITE
        // DELETE permission allows READ, WRITE, and DELETE
        if (permissionType === 'READ') {
          return p.permissionType === 'READ' || p.permissionType === 'WRITE' || p.permissionType === 'DELETE'
        }
        if (permissionType === 'DELETE') {
          return p.permissionType === 'DELETE'
        }
        if (permissionType === 'WRITE') {
          return p.permissionType === 'WRITE' || p.permissionType === 'DELETE'
        }
        return false
      }
      return false
    })
    if (docPermission) return true
    
    // Check folder permission (if document is in a folder)
    if (folderId) {
      const folderPermission = userPermissions.find(p => {
        if (p.folderId === folderId) {
          if (permissionType === 'READ') {
            return p.permissionType === 'READ' || p.permissionType === 'WRITE' || p.permissionType === 'DELETE'
          }
          if (permissionType === 'DELETE') {
            return p.permissionType === 'DELETE'
          }
          if (permissionType === 'WRITE') {
            return p.permissionType === 'WRITE' || p.permissionType === 'DELETE'
          }
          return false
        }
        return false
      })
      if (folderPermission) return true
    }
    
    return false
  }, [hasRestrictedAccess, userPermissions])

  const handleProjectChange = (projectId) => {
    console.log('=== FRONTEND DEBUG: handleProjectChange called with projectId:', projectId)
    console.log('=== FRONTEND DEBUG: Current selectedProject:', selectedProject)
    setSelectedProject(projectId)
    setSelectedFolder(null)
    console.log('=== FRONTEND DEBUG: State updated, projectId:', projectId, 'folder cleared')
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
      // Reload documents - permissions will be loaded automatically via useEffect
      await loadDocuments(selectedProject, selectedFolder)
    } catch (error) {
      message.error('Failed to upload document')
    }
  }

  const handleDownload = useCallback(async (record) => {
    try {
      await documentService.downloadDocument(record.id, record.originalFileName)
      message.success('Download started')
    } catch (error) {
      message.error('Failed to download document')
    }
  }, [])

  const handleDelete = useCallback(async (id) => {
    try {
      await documentService.deleteDocument(id)
      message.success('Document deleted successfully')
      loadDocuments(selectedProject, selectedFolder)
    } catch (error) {
      message.error('Failed to delete document')
    }
  }, [selectedProject, selectedFolder])

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
      
      // Reload permissions for ALL documents (not just the one updated)
      // This ensures all documents show updated permissions, including those affected by folder permissions
      // Use a small delay to ensure the backend has processed the permission change
      if (isAdminOrDoctor && documents && documents.length > 0) {
        setTimeout(async () => {
          const docIds = documents.map(doc => doc.id).filter(id => id != null)
          if (docIds.length > 0) {
            await loadDocumentPermissions(docIds)
          }
        }, 200)
      }
      
      setPermissionTarget({ type: null, id: null })
      
      // Reload existing permissions after granting
      if (permissionTarget.type === 'document') {
        const permissions = await permissionService.getDocumentPermissions(permissionTarget.id)
        setExistingPermissions(permissions || [])
      } else if (permissionTarget.type === 'folder') {
        const permissions = await permissionService.getFolderPermissions(permissionTarget.id)
        setExistingPermissions(permissions || [])
      }
    } catch (error) {
      message.error('Failed to grant permission')
    }
  }

  const openSignatureModal = useCallback((document) => {
    if (!isAdminOrDoctor) {
      message.error('Access denied. Admin or Doctor only.')
      return
    }
    setSelectedDocumentForSignature(document)
    setSignatureModalVisible(true)
    loadUsers() // Load users for selection
  }, [isAdminOrDoctor])

  const handleAssignSignature = useCallback(async (values) => {
    setAssigningSignature(true)
    
    // Add timeout to prevent hanging
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('Request timeout - signature request may still be processing')), 45000) // 45 second timeout
    })
    
    try {
      await Promise.race([
        signatureService.assignDocument(
          selectedDocumentForSignature.id,
          values.assignedToUserId,
          values.message || null
        ),
        timeoutPromise
      ])
      
      // Show success message
      message.success({
        content: 'Signature sent successfully!',
        duration: 3,
      })
      
      // Close modal after a brief delay so user sees the success message
      setTimeout(() => {
        setSignatureModalVisible(false)
        signatureForm.resetFields()
        setSelectedDocumentForSignature(null)
        setAssigningSignature(false)
      }, 1500)
    } catch (error) {
      console.error('Error assigning signature:', error)
      if (error.message && error.message.includes('timeout')) {
        message.warning('Request is taking longer than expected. The signature request may still be processing. Please check the Signatures page.')
      } else {
        message.error(error.response?.data?.message || 'Failed to assign document for signing')
      }
      setAssigningSignature(false)
    }
  }, [selectedDocumentForSignature, signatureForm])

  const handleRevokePermission = async (permissionId) => {
    try {
      await permissionService.revokePermission(permissionId)
      message.success('Permission revoked successfully')
      
      // Reload existing permissions
      if (permissionTarget.type === 'document') {
        const permissions = await permissionService.getDocumentPermissions(permissionTarget.id)
        setExistingPermissions(permissions || [])
      } else if (permissionTarget.type === 'folder') {
        const permissions = await permissionService.getFolderPermissions(permissionTarget.id)
        setExistingPermissions(permissions || [])
      }
      
      // Reload document permissions display
      if (isAdminOrDoctor && documents && documents.length > 0) {
        setTimeout(async () => {
          const docIds = documents.map(doc => doc.id).filter(id => id != null)
          if (docIds.length > 0) {
            await loadDocumentPermissions(docIds)
          }
        }, 200)
      }
    } catch (error) {
      message.error('Failed to revoke permission')
    }
  }

  const loadUsers = async () => {
    setLoadingUsers(true)
    try {
      const data = await userService.getAllUsers()
      console.log('Loaded users:', data) // Debug log
      setUsers(data || [])
    } catch (error) {
      console.error('Error loading users:', error)
      // If user is not admin, they can't fetch users - that's okay
      // Permission modal will just show role option
      if (error.response?.status === 403) {
        message.warning('Admin access required to view user list. You can still grant permissions by role.')
      } else {
        message.warning('Unable to load user list. You can still grant permissions by role.')
      }
      setUsers([])
    } finally {
      setLoadingUsers(false)
    }
  }

  const openPermissionModal = useCallback(async (type, id) => {
    // Only allow admins to open permission modal
    if (!isAdmin) {
      message.error('Access denied. Admin only.')
      return
    }
    setPermissionTarget({ type, id })
    setPermissionModalVisible(true)
    
    // Load users when opening permission modal
    await loadUsers()
    
    // Load existing permissions
    try {
      let permissions = []
      if (type === 'document') {
        permissions = await permissionService.getDocumentPermissions(id)
      } else if (type === 'folder') {
        permissions = await permissionService.getFolderPermissions(id)
      }
      setExistingPermissions(permissions || [])
    } catch (error) {
      console.error('Error loading existing permissions:', error)
      setExistingPermissions([])
    }
  }, [isAdmin])

  // Convert folders to tree data
  const buildTreeData = (folders, parentId = null) => {
    if (!folders || !Array.isArray(folders)) return []
    return folders
      .filter(f => f && (parentId === null ? !f.parentFolderId : f.parentFolderId === parentId))
      .map(folder => ({
        title: (
          <Space>
            <FolderOutlined />
            <span>{folder.folderName}</span>
            <span style={{ color: '#999', fontSize: 12 }}>
              ({folder.documentCount} docs)
            </span>
            {isAdmin && (
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
            )}
            {isAdminOrDoctor && (
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
            )}
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

  console.log('=== FRONTEND DEBUG: Creating columns array, isAdminOrDoctor:', isAdminOrDoctor, 'isAdmin:', isAdmin, 'hasRestrictedAccess:', hasRestrictedAccess, 'documentPermissionsMap keys:', Object.keys(documentPermissionsMap))
  
  const columns = useMemo(() => {
    console.log('=== FRONTEND DEBUG: useMemo columns function executing')
    try {
      return [
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
      render: (type, record) => {
        // Fix: Always use record.documentType, ensure it's a valid document type
        // Clean the documentType to remove any extra characters or folder names
        let docType = record.documentType
        
        // If docType is null or undefined, use OTHER
        if (!docType) {
          console.warn('Missing documentType for document:', record.id)
          return <Tag color="default">OTHER</Tag>
        }
        
        // Convert to string and trim whitespace
        docType = String(docType).trim()
        
        // Validate that docType is a valid DocumentType enum value
        const validTypes = ['CONTRACT', 'PROPOSAL', 'DELIVERABLE', 'REPORT', 'TRAINING_MATERIAL', 'OTHER']
        
        // Check if docType matches a valid type exactly (case-sensitive)
        const isValidType = validTypes.includes(docType)
        
        // Also check if docType contains folderName (which would be invalid)
        const containsFolderName = record.folderName && docType.includes(record.folderName)
        
        if (!isValidType || containsFolderName || docType === record.folderName) {
          // If documentType is missing, invalid, or incorrectly set to folderName, show default
          console.warn('Invalid documentType for document:', record.id, 'type:', docType, 'folderName:', record.folderName)
          return <Tag color="default">OTHER</Tag>
        }
        
        // Render ONLY the document type - no folder icons, no folder names
        return (
          <Tag color={documentTypeColors[docType] || 'default'}>
            {docType.replace(/_/g, ' ')}
          </Tag>
        )
      },
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
    // Permissions column - only for ADMIN and DOCTOR
    ...(isAdminOrDoctor ? [{
      title: 'Permissions',
      key: 'permissions',
      width: 250,
      render: (_, record) => {
        try {
          const permissions = documentPermissionsMap[record.id] || []
          
          if (!Array.isArray(permissions) || permissions.length === 0) {
            return <span style={{ color: '#999', fontSize: '12px' }}>No permissions set</span>
          }
          
          // Get unique users/roles with their highest permission type
          const permissionMap = new Map()
          permissions.forEach(perm => {
            if (!perm) return // Skip null/undefined permissions
            const key = perm.userName || perm.role || 'Unknown'
            const existing = permissionMap.get(key)
            const permType = perm.permissionType || 'READ'
            
            // Keep the highest permission level
          if (!existing) {
            permissionMap.set(key, permType)
          } else if (permType === 'DELETE') {
            permissionMap.set(key, 'DELETE')
          } else if (permType === 'WRITE' && existing === 'READ') {
            permissionMap.set(key, 'WRITE')
          }
        })
        
        return (
          <Space direction="vertical" size={2} style={{ width: '100%' }}>
            {Array.from(permissionMap.entries()).map(([name, permType], idx) => (
              <div key={idx} style={{ fontSize: '12px', lineHeight: '1.5' }}>
                <strong>{name}</strong>: <Tag 
                  size="small"
                  color={permType === 'READ' ? 'blue' : permType === 'WRITE' ? 'orange' : 'red'}
                >
                  {permType}
                </Tag>
              </div>
            ))}
          </Space>
        )
        } catch (error) {
          console.error('Error rendering permissions for document:', record.id, error)
          return <span style={{ color: '#999', fontSize: '12px' }}>Error loading permissions</span>
        }
      }
    }] : []),
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => {
        // Check if restricted user has permission to download/delete this document
        const hasReadPermission = !hasRestrictedAccess || checkDocumentPermission(record.id, 'READ', record.folderId)
        const hasDeletePermission = !hasRestrictedAccess || checkDocumentPermission(record.id, 'DELETE', record.folderId)
        
        return (
          <Space>
            {hasReadPermission && (
              <Button
                type="link"
                icon={<DownloadOutlined />}
                onClick={() => handleDownload(record)}
              >
                Download
              </Button>
            )}
            {isAdmin && (
              <Button
                type="link"
                icon={<LockOutlined />}
                onClick={() => openPermissionModal('document', record.id)}
              >
                Permissions
              </Button>
            )}
            {isAdminOrDoctor && (
              <Button
                type="link"
                icon={<FileTextOutlined />}
                onClick={() => openSignatureModal(record)}
              >
                Request Signature
              </Button>
            )}
            {(isAdminOrDoctor || (hasRestrictedAccess && hasDeletePermission)) && (
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
            )}
          </Space>
        )
      }
    }
      ]
    } catch (error) {
      console.error('=== FRONTEND DEBUG: ERROR creating columns array:', error)
      console.error('=== FRONTEND DEBUG: Columns error stack:', error.stack)
      // Return minimal columns on error
      return [
        { title: 'File Name', dataIndex: 'originalFileName', key: 'originalFileName' },
        { title: 'Error', key: 'error', render: () => 'Error loading columns - check console' }
      ]
    }
  }, [isAdminOrDoctor, isAdmin, hasRestrictedAccess, documentPermissionsMap, checkDocumentPermission, handleDownload, openPermissionModal, openSignatureModal, handleDelete]);

  console.log('=== FRONTEND DEBUG: About to render Documents component')
  console.log('=== FRONTEND DEBUG: Current state:', {
    selectedProject,
    selectedFolder,
    documentsCount: documents?.length || 0,
    projectsCount: projects?.length || 0,
    foldersCount: folders?.length || 0,
    loading,
    isAdminOrDoctor,
    hasRestrictedAccess,
    columnsCount: columns?.length
  })
  console.log('=== FRONTEND DEBUG: Documents array:', documents)
  console.log('=== FRONTEND DEBUG: Columns array:', columns)
  
  // CRITICAL: Check if columns is valid before rendering
  if (!columns || !Array.isArray(columns) || columns.length === 0) {
    console.error('=== FRONTEND DEBUG: FATAL - columns is invalid:', columns)
    return (
      <Content style={{ margin: '24px 16px 0' }}>
        <div style={{ padding: 24, minHeight: 360 }}>
          <Title level={3} style={{ marginBottom: 24, color: 'red' }}>Error: Invalid Columns</Title>
          <Card>
            <p>Columns array is invalid. Check console for details.</p>
            <p>Columns: {JSON.stringify(columns)}</p>
          </Card>
        </div>
      </Content>
    )
  }
  
  try {
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
                placeholder={hasRestrictedAccess ? "Choose a project (or leave empty to see all)" : "Choose a project"}
                onChange={handleProjectChange}
                value={selectedProject}
                showSearch
                optionFilterProp="children"
                allowClear={hasRestrictedAccess}
                onClear={() => {
                  setSelectedProject(null)
                  setSelectedFolder(null)
                }}
              >
                {(projects || []).map(project => (
                  <Select.Option key={project.id} value={project.id}>
                    {project.projectName}
                  </Select.Option>
                ))}
              </Select>
              {selectedProject && isAdminOrDoctor && (
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
            {!hasRestrictedAccess && (
              <Col span={6}>
                <Card title="Folders" size="small">
                  {isAdminOrDoctor && (
                    <Button
                      type="dashed"
                      block
                      icon={<PlusOutlined />}
                      onClick={() => setFolderModalVisible(true)}
                      style={{ marginBottom: 16 }}
                    >
                      New Folder
                    </Button>
                  )}
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
            )}
            <Col span={hasRestrictedAccess ? 24 : 18}>
              <Card>
                {/* DEBUG INFO - Remove after fixing */}
                <div style={{ padding: 10, background: '#f0f0f0', marginBottom: 10, fontSize: '12px' }}>
                  <strong>DEBUG:</strong> Documents: {documents?.length || 0} | 
                  Loading: {loading ? 'Yes' : 'No'} | 
                  Project: {selectedProject} | 
                  Columns: {columns?.length || 0}
                </div>
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
                {(() => {
                  console.log('=== FRONTEND DEBUG: Rendering Table with documents:', documents?.length, 'loading:', loading)
                  console.log('=== FRONTEND DEBUG: Documents array:', documents)
                  console.log('=== FRONTEND DEBUG: Columns:', columns?.length)
                  try {
                    return (
                      <Table
                        columns={columns}
                        dataSource={documents || []}
                        rowKey="id"
                        loading={loading}
                        locale={{
                          emptyText: hasRestrictedAccess 
                            ? 'No documents found. You may not have permission to view documents in this project, or no documents exist.'
                            : 'No documents found'
                        }}
                        pagination={{
                          pageSize: 10,
                          showSizeChanger: true,
                          showTotal: (total) => `Total ${total} documents`
                        }}
                      />
                    )
                  } catch (tableError) {
                    console.error('=== FRONTEND DEBUG: ERROR rendering Table:', tableError)
                    return (
                      <div style={{ padding: 20, color: 'red' }}>
                        <p>Error rendering table: {tableError.message}</p>
                        <p>Documents count: {documents?.length || 0}</p>
                        <p>Check console for details</p>
                      </div>
                    )
                  }
                })()}
              </Card>
            </Col>
          </Row>
        ) : (
          <Card>
            <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
              <FolderOutlined style={{ fontSize: 48, marginBottom: 16 }} />
              <p>
                {hasRestrictedAccess 
                  ? 'Select a project to view documents you have permission to access'
                  : 'Select a project to view and manage documents'}
              </p>
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
                {(projects || []).map(project => (
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
                {(folders || []).map(folder => (
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
            setExistingPermissions([])
            setUsers([]) // Reset users when closing
          }}
          footer={null}
          width={600}
          afterOpenChange={(open) => {
            // Load users when modal opens
            if (open) {
              loadUsers()
            }
          }}
        >
          {/* Existing Permissions List */}
          <div style={{ marginBottom: 24 }}>
            <Title level={5}>Current Permissions</Title>
            {existingPermissions.length === 0 ? (
              <div style={{ padding: '16px', background: '#f5f5f5', borderRadius: '4px', color: '#999' }}>
                No permissions set
              </div>
            ) : (
              <div style={{ maxHeight: '200px', overflowY: 'auto' }}>
                {existingPermissions.map((perm) => (
                  <div
                    key={perm.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '12px',
                      marginBottom: '8px',
                      background: '#fafafa',
                      borderRadius: '4px',
                      border: '1px solid #e8e8e8'
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 'bold', marginBottom: '4px' }}>
                        {perm.userName ? `${perm.userName}` : perm.role || 'Unknown'}
                        {perm.userRole && perm.userRole !== perm.role && ` (${perm.userRole})`}
                      </div>
                      <Tag
                        color={
                          perm.permissionType === 'READ' ? 'blue' :
                          perm.permissionType === 'WRITE' ? 'orange' : 'red'
                        }
                        size="small"
                      >
                        {perm.permissionType}
                      </Tag>
                    </div>
                    <Popconfirm
                      title="Revoke Permission"
                      description={`Are you sure you want to remove ${perm.userName || perm.role}'s ${perm.permissionType} permission?`}
                      onConfirm={() => handleRevokePermission(perm.id)}
                      okText="Yes, Remove"
                      cancelText="Cancel"
                      okButtonProps={{ danger: true }}
                    >
                      <Button
                        type="text"
                        danger
                        icon={<CloseOutlined />}
                        size="small"
                      >
                        Remove
                      </Button>
                    </Popconfirm>
                  </div>
                ))}
              </div>
            )}
          </div>

          <Divider>Grant New Permission</Divider>

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
                      <Select 
                        placeholder={loadingUsers ? "Loading users..." : "Select user"} 
                        showSearch
                        loading={loadingUsers}
                        notFoundContent={loadingUsers ? "Loading..." : "No users found"}
                        filterOption={(input, option) =>
                          (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                        }
                        options={users
                          .filter(user => user.active) // Only show active users
                          .map(user => ({
                            value: user.id,
                            label: `${user.fullName} (${user.email})`,
                            title: `${user.fullName} - ${user.email} - ${user.role}`
                          }))}
                      />
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

        {/* Signature Assignment Modal */}
        <Modal
          title="Request Document Signature"
          open={signatureModalVisible}
          onCancel={() => {
            if (!assigningSignature) {
              setSignatureModalVisible(false)
              signatureForm.resetFields()
              setSelectedDocumentForSignature(null)
            }
          }}
          footer={null}
          width={500}
          closable={!assigningSignature}
          maskClosable={!assigningSignature}
        >
          <Form form={signatureForm} layout="vertical" onFinish={handleAssignSignature}>
            <Form.Item label="Document">
              <Input
                value={selectedDocumentForSignature?.originalFileName}
                disabled
              />
            </Form.Item>

            <Form.Item
              name="assignedToUserId"
              label="Assign To User"
              rules={[{ required: true, message: 'Please select a user' }]}
            >
              <Select
                placeholder={loadingUsers ? "Loading users..." : "Select user"}
                showSearch
                loading={loadingUsers}
                notFoundContent={loadingUsers ? "Loading..." : "No users found"}
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
                options={users
                  .filter(user => user.active)
                  .map(user => ({
                    value: user.id,
                    label: `${user.fullName} (${user.email})`,
                    title: `${user.fullName} - ${user.email} - ${user.role}`
                  }))}
              />
            </Form.Item>

            <Form.Item
              name="message"
              label="Message (Optional)"
            >
              <TextArea
                rows={3}
                placeholder="Add a message for the signer..."
              />
            </Form.Item>

            <Form.Item>
              <Space>
                <Button type="primary" htmlType="submit" loading={assigningSignature}>
                  {assigningSignature ? 'Sending...' : 'Send for Signature'}
                </Button>
                <Button
                  onClick={() => {
                    setSignatureModalVisible(false)
                    signatureForm.resetFields()
                    setSelectedDocumentForSignature(null)
                  }}
                  disabled={assigningSignature}
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
  } catch (error) {
    console.error('=== FRONTEND DEBUG: FATAL ERROR in Documents component render')
    console.error('=== FRONTEND DEBUG: Error:', error)
    console.error('=== FRONTEND DEBUG: Error stack:', error.stack)
    return (
      <Content style={{ margin: '24px 16px 0' }}>
        <div style={{ padding: 24, minHeight: 360 }}>
          <Title level={3} style={{ marginBottom: 24, color: 'red' }}>Error Loading Documents</Title>
          <Card>
            <p>An error occurred while rendering the Documents page.</p>
            <p>Error: {error.message}</p>
            <p>Please check the browser console for details.</p>
          </Card>
        </div>
      </Content>
    )
  }
}

export default Documents
