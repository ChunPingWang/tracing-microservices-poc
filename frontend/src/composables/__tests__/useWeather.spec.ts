import { describe, it, expect, vi, beforeEach } from 'vitest'
import { useWeather } from '../useWeather'
import * as weatherApi from '@/services/weatherApi'

vi.mock('@/services/weatherApi')

describe('useWeather', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should initialize with default state', () => {
    const { weather, loading, error } = useWeather()

    expect(weather.value).toBeNull()
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
  })

  it('should fetch weather successfully', async () => {
    const mockResponse = {
      success: true,
      data: {
        cityCode: 'TPE',
        cityName: '台北',
        temperature: 26.5,
        rainfall: 12.3,
        updatedAt: '2026-01-21T10:30:00Z'
      },
      traceInfo: {
        traceId: '4bf92f3577b34da6a3ce929d0e0e4736',
        spanId: '00f067aa0ba902b7',
        duration: 45
      }
    }

    vi.mocked(weatherApi.getWeather).mockResolvedValue(mockResponse)

    const { weather, loading, error, fetchWeather } = useWeather()

    await fetchWeather('TPE')

    expect(weatherApi.getWeather).toHaveBeenCalledWith('TPE')
    expect(weather.value).toEqual(mockResponse)
    expect(loading.value).toBe(false)
    expect(error.value).toBeNull()
  })

  it('should set loading state during fetch', async () => {
    vi.mocked(weatherApi.getWeather).mockImplementation(
      () => new Promise(resolve => setTimeout(resolve, 100))
    )

    const { loading, fetchWeather } = useWeather()

    const fetchPromise = fetchWeather('TPE')
    expect(loading.value).toBe(true)

    await fetchPromise
    expect(loading.value).toBe(false)
  })

  it('should handle fetch error', async () => {
    const mockError = new Error('Network error')
    vi.mocked(weatherApi.getWeather).mockRejectedValue(mockError)

    const { weather, error, fetchWeather } = useWeather()

    await fetchWeather('TPE')

    expect(weather.value).toBeNull()
    expect(error.value).toBe('Network error')
  })

  it('should clear previous error on new fetch', async () => {
    vi.mocked(weatherApi.getWeather)
      .mockRejectedValueOnce(new Error('First error'))
      .mockResolvedValueOnce({
        success: true,
        data: { cityCode: 'TPE', cityName: '台北', temperature: 25, rainfall: 10, updatedAt: '' },
        traceInfo: { traceId: 'abc', spanId: 'def', duration: 10 }
      })

    const { error, fetchWeather } = useWeather()

    await fetchWeather('TPE')
    expect(error.value).toBe('First error')

    await fetchWeather('TPE')
    expect(error.value).toBeNull()
  })
})
