import { ref } from 'vue'
import type { WeatherResponse, CityCode } from '@/types/weather'
import { getWeather } from '@/services/weatherApi'

/**
 * Composable for managing weather data fetching.
 * Provides reactive state for weather queries.
 */
export function useWeather() {
  const weather = ref<WeatherResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  /**
   * Fetch weather data for a city.
   * @param cityCode The city code (TPE, TXG, KHH)
   */
  async function fetchWeather(cityCode: CityCode) {
    loading.value = true
    error.value = null

    try {
      const response = await getWeather(cityCode)
      weather.value = response
    } catch (err) {
      error.value = err instanceof Error ? err.message : 'Unknown error'
      weather.value = null
    } finally {
      loading.value = false
    }
  }

  /**
   * Clear current weather data and error state.
   */
  function clearWeather() {
    weather.value = null
    error.value = null
  }

  return {
    weather,
    loading,
    error,
    fetchWeather,
    clearWeather
  }
}
