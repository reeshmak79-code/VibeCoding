import axios from 'axios'

const API_URL = '/api/signatures'

export const signatureService = {
  assignDocument: async (documentId, assignedToUserId, message) => {
    const response = await axios.post(`${API_URL}/assign`, {
      documentId,
      assignedToUserId,
      message
    })
    return response.data
  },

  getPendingSignatures: async () => {
    const response = await axios.get(`${API_URL}/pending`)
    return response.data
  },

  getSigningUrl: async (signatureId) => {
    const response = await axios.get(`${API_URL}/${signatureId}/sign-url`)
    return response.data
  },

  getDocumentSignatures: async (documentId) => {
    const response = await axios.get(`${API_URL}/document/${documentId}`)
    return response.data
  }
}
