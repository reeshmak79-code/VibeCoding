import axios from 'axios'

const API_URL = '/api/users'

export const userService = {
  getAllUsers: async () => {
    const response = await axios.get(API_URL)
    return response.data
  },

  getUser: async (id) => {
    const response = await axios.get(`${API_URL}/${id}`)
    return response.data
  },

  createUser: async (userData) => {
    const response = await axios.post(API_URL, userData)
    return response.data
  },

  updateUser: async (id, userData) => {
    const response = await axios.put(`${API_URL}/${id}`, userData)
    return response.data
  },

  deactivateUser: async (id) => {
    const response = await axios.put(`${API_URL}/${id}/deactivate`)
    return response.data
  },

  activateUser: async (id) => {
    const response = await axios.put(`${API_URL}/${id}/activate`)
    return response.data
  },

  deleteUser: async (id) => {
    const response = await axios.delete(`${API_URL}/${id}`)
    return response.data
  }
}
