import axios, { type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from '../types/api.types'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  withCredentials: false,
  timeout: 60000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token')
  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export async function get<T>(url: string) {
  const response = await api.get<ApiResponse<T>>(url)
  return response.data
}

export async function post<T, B = unknown>(url: string, body: B, config?: AxiosRequestConfig) {
  const response = await api.post<ApiResponse<T>>(url, body, config)
  return response.data
}

export async function put<T, B = unknown>(url: string, body?: B) {
  const response = await api.put<ApiResponse<T>>(url, body)
  return response.data
}

export async function del<T>(url: string) {
  const response = await api.delete<ApiResponse<T>>(url)
  return response.data
}

export default api
