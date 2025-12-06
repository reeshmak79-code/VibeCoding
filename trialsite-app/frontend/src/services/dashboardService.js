import axios from 'axios'

const API_URL = '/api/dashboard'

export const dashboardService = {
  getOverview: async () => {
    const response = await axios.get(`${API_URL}/overview`)
    return response.data
  },

  getProjectsByService: async () => {
    const response = await axios.get(`${API_URL}/projects-by-service`)
    return response.data
  },

  getProjectsByStatus: async () => {
    const response = await axios.get(`${API_URL}/projects-by-status`)
    return response.data
  },

  getRevenueStats: async () => {
    const response = await axios.get(`${API_URL}/revenue-stats`)
    return response.data
  },

  getRecentProjects: async () => {
    const response = await axios.get(`${API_URL}/recent-projects`)
    return response.data
  }
}
