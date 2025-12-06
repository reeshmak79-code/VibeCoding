import axios from 'axios'

const API_URL = '/api/folders'

export const folderService = {
  createFolder: async (folderData) => {
    const response = await axios.post(API_URL, folderData)
    return response.data
  },

  getProjectFolders: async (projectId) => {
    const response = await axios.get(`${API_URL}/project/${projectId}`)
    return response.data
  },

  getFolder: async (id) => {
    const response = await axios.get(`${API_URL}/${id}`)
    return response.data
  },

  updateFolder: async (id, folderData) => {
    const response = await axios.put(`${API_URL}/${id}`, folderData)
    return response.data
  },

  deleteFolder: async (id) => {
    const response = await axios.delete(`${API_URL}/${id}`)
    return response.data
  }
}
