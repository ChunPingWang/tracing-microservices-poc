<script setup lang="ts">
import type { TraceInfo } from '@/types/weather'

const props = defineProps<{
  traceInfo: TraceInfo
  jaegerBaseUrl: string
}>()

// Generate Jaeger trace URL
const traceUrl = `${props.jaegerBaseUrl}/trace/${props.traceInfo.traceId}`
</script>

<template>
  <div class="trace-info">
    <h4>追蹤資訊</h4>
    <div class="trace-details">
      <div class="trace-item">
        <span class="label">Trace ID:</span>
        <a :href="traceUrl" target="_blank" class="trace-link">
          {{ traceInfo.traceId }}
        </a>
      </div>
      <div class="trace-item">
        <span class="label">Span ID:</span>
        <span class="value">{{ traceInfo.spanId }}</span>
      </div>
      <div class="trace-item">
        <span class="label">回應時間:</span>
        <span class="value duration">{{ traceInfo.duration }} ms</span>
      </div>
    </div>
    <p class="trace-hint">
      點擊 Trace ID 可在 Jaeger UI 查看完整請求鏈路
    </p>
  </div>
</template>

<style scoped>
.trace-info {
  background: #fff;
  border-radius: 8px;
  padding: 15px;
  margin-top: 15px;
  border: 1px solid #e0e0e0;
}

.trace-info h4 {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0 0 12px 0;
}

.trace-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.trace-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.trace-item .label {
  color: #95a5a6;
  min-width: 80px;
}

.trace-item .value {
  font-family: 'Monaco', 'Consolas', monospace;
  color: #2c3e50;
}

.trace-link {
  font-family: 'Monaco', 'Consolas', monospace;
  color: #3498db;
  text-decoration: none;
  word-break: break-all;
}

.trace-link:hover {
  text-decoration: underline;
}

.duration {
  color: #27ae60;
  font-weight: 600;
}

.trace-hint {
  font-size: 11px;
  color: #bdc3c7;
  margin: 12px 0 0 0;
  font-style: italic;
}
</style>
