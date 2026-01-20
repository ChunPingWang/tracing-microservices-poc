import axios, { type AxiosInstance, type AxiosResponse } from 'axios'
import type { WeatherResponse, CityCode } from '@/types/weather'

// Create axios instance with default configuration
const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Response interceptor for extracting trace headers
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    // Extract trace headers and attach to response data
    const traceId = response.headers['x-trace-id']
    const spanId = response.headers['x-span-id']
    const duration = response.headers['x-request-duration']

    if (traceId && response.data) {
      response.data._headers = {
        traceId,
        spanId,
        duration: duration ? parseInt(duration, 10) : undefined
      }
    }

    return response
  },
  (error) => {
    // Handle error responses
    if (error.response) {
      const traceId = error.response.headers['x-trace-id']
      if (traceId && error.response.data) {
        error.response.data._headers = {
          traceId,
          spanId: error.response.headers['x-span-id'],
          duration: error.response.headers['x-request-duration']
        }
      }
    }
    return Promise.reject(error)
  }
)

// API functions
export async function getWeather(cityCode: CityCode): Promise<WeatherResponse> {
  const response = await apiClient.get<WeatherResponse>(`/weather/${cityCode}`)
  return response.data
}

export default apiClient
