<script setup lang="ts">
import { computed } from 'vue'
import CitySelector from '@/components/CitySelector.vue'
import WeatherCard from '@/components/WeatherCard.vue'
import TraceInfo from '@/components/TraceInfo.vue'
import { useWeather } from '@/composables/useWeather'
import type { CityCode, WeatherSuccessResponse } from '@/types/weather'

const { weather, loading, error, fetchWeather } = useWeather()

function onCitySelect(cityCode: CityCode) {
  fetchWeather(cityCode)
}

// Extract weather data if response is successful
const weatherData = computed(() => {
  if (weather.value?.success) {
    return (weather.value as WeatherSuccessResponse).data
  }
  return null
})

// Extract trace info if available
const traceInfo = computed(() => {
  return weather.value?.traceInfo || null
})

// Jaeger UI base URL
const jaegerBaseUrl = 'http://localhost:16686'
</script>

<template>
  <div id="app">
    <header>
      <h1>天氣查詢可觀測性展示系統</h1>
      <p>Weather Tracing PoC</p>
    </header>

    <main>
      <CitySelector @select="onCitySelect" />

      <div v-if="loading" class="loading-state">
        <span class="spinner"></span>
        載入中...
      </div>

      <div v-else-if="error" class="error-state">
        <p class="error-message">{{ error }}</p>
        <button @click="() => {}">重試</button>
      </div>

      <template v-else-if="weatherData">
        <WeatherCard :data="weatherData" :loading="loading" />
        <TraceInfo
          v-if="traceInfo"
          :trace-info="traceInfo"
          :jaeger-base-url="jaegerBaseUrl"
        />
      </template>

      <div v-else class="empty-state">
        <p>請選擇一個城市查詢天氣</p>
      </div>
    </main>

    <footer>
      <div class="quick-links">
        <h4>快速連結</h4>
        <ul>
          <li><a :href="jaegerBaseUrl" target="_blank">Jaeger UI (追蹤)</a></li>
          <li><a href="http://localhost:3000" target="_blank">Grafana (指標儀表板)</a></li>
          <li><a href="http://localhost:9090" target="_blank">Prometheus (指標資料)</a></li>
        </ul>
      </div>
    </footer>
  </div>
</template>

<style>
* {
  box-sizing: border-box;
}

#app {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
}

header {
  text-align: center;
  margin-bottom: 30px;
}

header h1 {
  color: #2c3e50;
  margin-bottom: 5px;
}

header p {
  color: #7f8c8d;
  font-size: 14px;
}

main {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  min-height: 300px;
}

.loading-state,
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 20px;
  color: #7f8c8d;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #e0e0e0;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state {
  text-align: center;
  padding: 40px 20px;
}

.error-message {
  color: #e74c3c;
  margin-bottom: 15px;
}

.error-state button {
  padding: 10px 20px;
  background: #3498db;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

footer {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ecf0f1;
}

.quick-links h4 {
  font-size: 14px;
  color: #7f8c8d;
  margin-bottom: 10px;
}

.quick-links ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.quick-links a {
  color: #3498db;
  text-decoration: none;
  font-size: 14px;
}

.quick-links a:hover {
  text-decoration: underline;
}
</style>
