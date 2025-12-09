import axios from 'axios'

const API_URL = '/api/documents'

export const documentService = {
  uploadDocument: async (file, projectId, documentType, description, folderId) => {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('projectId', projectId)
    formData.append('documentType', documentType)
    if (description) {
      formData.append('description', description)
    }
    if (folderId) {
      formData.append('folderId', folderId)
    }
    
    const response = await axios.post(`${API_URL}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  },

  getProjectDocuments: async (projectId) => {
    console.log('=== FRONTEND DEBUG: documentService.getProjectDocuments called for projectId:', projectId)
    try {
      console.log('=== FRONTEND DEBUG: Making axios GET request to:', `${API_URL}/project/${projectId}`)
      const response = await axios.get(`${API_URL}/project/${projectId}`)
      console.log('=== FRONTEND DEBUG: Axios response received:', response)
      console.log('=== FRONTEND DEBUG: Response status:', response.status)
      console.log('=== FRONTEND DEBUG: Response data type:', typeof response.data)
      console.log('=== FRONTEND DEBUG: Response data is array?', Array.isArray(response.data))
      console.log('=== FRONTEND DEBUG: Response data length:', response.data?.length)
      console.log('=== FRONTEND DEBUG: Response data:', JSON.stringify(response.data, null, 2))
      return response.data
    } catch (error) {
      console.error('=== FRONTEND DEBUG: Error in documentService.getProjectDocuments')
      console.error('=== FRONTEND DEBUG: Error object:', error)
      console.error('=== FRONTEND DEBUG: Error response:', error.response)
      console.error('=== FRONTEND DEBUG: Error response data:', error.response?.data)
      console.error('=== FRONTEND DEBUG: Error response status:', error.response?.status)
      throw error
    }
  },

  getFolderDocuments: async (folderId) => {
    const response = await axios.get(`${API_URL}/folder/${folderId}`)
    return response.data
  },

  getDocument: async (id) => {
    const response = await axios.get(`${API_URL}/${id}`)
    return response.data
  },

  downloadDocument: async (id, fileName) => {
    const response = await axios.get(`${API_URL}/download/${id}`, {
      responseType: 'blob'
    })
    
    // Create download link
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', fileName)
    document.body.appendChild(link)
    link.click()
    link.remove()
  },

  deleteDocument: async (id) => {
    const response = await axios.delete(`${API_URL}/${id}`)
    return response.data
  },

  getProjectDocumentStats: async (projectId) => {
    const response = await axios.get(`${API_URL}/stats/project/${projectId}`)
    return response.data
  }
}
