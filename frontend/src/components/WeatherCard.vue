<script setup lang="ts">
import type { WeatherData } from '@/types/weather'

defineProps<{
  data: WeatherData
  loading?: boolean
}>()
</script>

<template>
  <div class="weather-card">
    <div v-if="loading" class="loading">
      <span class="spinner"></span>
      載入中...
    </div>
    <div v-else class="weather-content">
      <div class="city-info">
        <h3>{{ data.cityName }}</h3>
        <span class="city-code">{{ data.cityCode }}</span>
      </div>

      <div class="weather-data">
        <div class="data-item temperature">
          <span class="label">溫度</span>
          <span class="value">{{ data.temperature.toFixed(1) }}°C</span>
        </div>
        <div class="data-item rainfall">
          <span class="label">雨量</span>
          <span class="value">{{ data.rainfall.toFixed(1) }} mm</span>
        </div>
      </div>

      <div class="update-time">
        更新時間: {{ new Date(data.updatedAt).toLocaleString('zh-TW') }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.weather-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 40px;
  color: #7f8c8d;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid #e0e0e0;
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.city-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.city-info h3 {
  font-size: 24px;
  color: #2c3e50;
  margin: 0;
}

.city-code {
  background: #ecf0f1;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  color: #7f8c8d;
}

.weather-data {
  display: flex;
  gap: 30px;
  margin-bottom: 20px;
}

.data-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.data-item .label {
  font-size: 14px;
  color: #7f8c8d;
}

.data-item .value {
  font-size: 28px;
  font-weight: bold;
}

.temperature .value {
  color: #e74c3c;
}

.rainfall .value {
  color: #3498db;
}

.update-time {
  font-size: 12px;
  color: #95a5a6;
  border-top: 1px solid #ecf0f1;
  padding-top: 15px;
}
</style>
