import axios from 'axios'

const API_URL = '/api/projects'

export const projectService = {
  getAllProjects: async () => {
    const response = await axios.get(API_URL)
    return response.data
  },

  getProjectById: async (id) => {
    const response = await axios.get(`${API_URL}/${id}`)
    return response.data
  },

  searchProjects: async (query) => {
    const response = await axios.get(`${API_URL}/search`, {
      params: { q: query }
    })
    return response.data
  },

  getProjectsByClient: async (clientId) => {
    const response = await axios.get(`${API_URL}/client/${clientId}`)
    return response.data
  },

  getProjectsByStatus: async (status) => {
    const response = await axios.get(`${API_URL}/status/${status}`)
    return response.data
  },

  getProjectStats: async () => {
    const response = await axios.get(`${API_URL}/stats`)
    return response.data
  },

  createProject: async (projectData) => {
    const response = await axios.post(API_URL, projectData)
    return response.data
  },

  updateProject: async (id, projectData) => {
    const response = await axios.put(`${API_URL}/${id}`, projectData)
    return response.data
  },

  deleteProject: async (id) => {
    const response = await axios.delete(`${API_URL}/${id}`)
    return response.data
  }
}
