import axios from 'axios'
import type { ApiResponse } from './types'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

const client = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

client.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 0) {
        return payload.data
      }
      const error = new Error(payload.msg || '请求失败')
      ;(error as Error & { code?: number }).code = payload.code
      return Promise.reject(error)
    }
    return response.data
  },
  (error) => Promise.reject(error),
)

export default client
