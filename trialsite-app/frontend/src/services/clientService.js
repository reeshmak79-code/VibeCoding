import axios from 'axios'

const API_URL = '/api/clients'

export const clientService = {
  getAllClients: async () => {
    const response = await axios.get(API_URL)
    return response.data
  },

  getClientById: async (id) => {
    const response = await axios.get(`${API_URL}/${id}`)
    return response.data
  },

  searchClients: async (query) => {
    const response = await axios.get(`${API_URL}/search`, {
      params: { q: query }
    })
    return response.data
  },

  getClientsByStatus: async (status) => {
    const response = await axios.get(`${API_URL}/status/${status}`)
    return response.data
  },

  getClientStats: async () => {
    const response = await axios.get(`${API_URL}/stats`)
    return response.data
  },

  createClient: async (clientData) => {
    const response = await axios.post(API_URL, clientData)
    return response.data
  },

  updateClient: async (id, clientData) => {
    const response = await axios.put(`${API_URL}/${id}`, clientData)
    return response.data
  },

  deleteClient: async (id) => {
    const response = await axios.delete(`${API_URL}/${id}`)
    return response.data
  }
}
