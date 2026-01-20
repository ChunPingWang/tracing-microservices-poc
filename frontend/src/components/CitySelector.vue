<script setup lang="ts">
import { CITIES, type CityCode } from '@/types/weather'

const emit = defineEmits<{
  (e: 'select', cityCode: CityCode): void
}>()

function selectCity(cityCode: CityCode) {
  emit('select', cityCode)
}
</script>

<template>
  <div class="city-selector">
    <h2>選擇城市</h2>
    <div class="city-buttons">
      <button
        v-for="city in CITIES"
        :key="city.code"
        class="city-button"
        @click="selectCity(city.code)"
      >
        {{ city.name }}
        <span class="city-code">({{ city.code }})</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.city-selector {
  margin-bottom: 20px;
}

.city-selector h2 {
  font-size: 18px;
  color: #2c3e50;
  margin-bottom: 15px;
}

.city-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.city-button {
  padding: 12px 24px;
  font-size: 16px;
  border: 2px solid #3498db;
  border-radius: 8px;
  background: white;
  color: #3498db;
  cursor: pointer;
  transition: all 0.2s ease;
}

.city-button:hover {
  background: #3498db;
  color: white;
}

.city-button:active {
  transform: scale(0.98);
}

.city-code {
  font-size: 12px;
  opacity: 0.7;
  margin-left: 4px;
}
</style>
