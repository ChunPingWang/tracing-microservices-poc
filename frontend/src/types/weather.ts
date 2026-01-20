// Weather API Types - per contracts/weather-api.yaml

export interface WeatherData {
  cityCode: 'TPE' | 'TXG' | 'KHH'
  cityName: string
  temperature: number
  rainfall: number
  updatedAt: string
}

export interface TraceInfo {
  traceId: string
  spanId: string
  duration: number
}

export interface WeatherSuccessResponse {
  success: true
  data: WeatherData
  traceInfo: TraceInfo
}

export interface ErrorInfo {
  code: 'CITY_NOT_FOUND' | 'INVALID_CITY_CODE' | 'SERVICE_UNAVAILABLE' | 'INTERNAL_ERROR'
  message: string
}

export interface WeatherErrorResponse {
  success: false
  error: ErrorInfo
  traceInfo: TraceInfo
}

export type WeatherResponse = WeatherSuccessResponse | WeatherErrorResponse

export type CityCode = 'TPE' | 'TXG' | 'KHH'

export interface City {
  code: CityCode
  name: string
}

export const CITIES: City[] = [
  { code: 'TPE', name: '台北' },
  { code: 'TXG', name: '台中' },
  { code: 'KHH', name: '高雄' }
]
