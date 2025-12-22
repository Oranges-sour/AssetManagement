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

const decodeUnicodeEscapes = (value: string) =>
  value.replace(/\\u([0-9a-fA-F]{4})/g, (_, code) => String.fromCharCode(parseInt(code, 16)))

const decodeResponse = <T>(value: T): T => {
  if (typeof value === 'string') {
    return decodeUnicodeEscapes(value) as T
  }
  if (Array.isArray(value)) {
    return value.map((item) => decodeResponse(item)) as T
  }
  if (value && typeof value === 'object') {
    const result: Record<string, unknown> = {}
    Object.entries(value as Record<string, unknown>).forEach(([key, item]) => {
      result[key] = decodeResponse(item)
    })
    return result as T
  }
  return value
}

client.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>
    if (payload && typeof payload.code === 'number') {
      if (payload.code === 0) {
        return decodeResponse(payload.data)
      }
      const error = new Error(decodeUnicodeEscapes(payload.msg || '请求失败'))
      ;(error as Error & { code?: number }).code = payload.code
      return Promise.reject(error)
    }
    return decodeResponse(response.data)
  },
  (error) => Promise.reject(error),
)

export default client
