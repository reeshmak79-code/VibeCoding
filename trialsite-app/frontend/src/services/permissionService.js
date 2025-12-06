import axios from 'axios'

const API_URL = '/api/permissions'

export const permissionService = {
  grantPermission: async (permissionData) => {
    const response = await axios.post(API_URL, permissionData)
    return response.data
  },

  getDocumentPermissions: async (documentId) => {
    const response = await axios.get(`${API_URL}/document/${documentId}`)
    return response.data
  },

  getFolderPermissions: async (folderId) => {
    const response = await axios.get(`${API_URL}/folder/${folderId}`)
    return response.data
  },

  getRolePermissions: async (role) => {
    const response = await axios.get(`${API_URL}/role/${role}`)
    return response.data
  },

  revokePermission: async (id) => {
    const response = await axios.delete(`${API_URL}/${id}`)
    return response.data
  }
}
