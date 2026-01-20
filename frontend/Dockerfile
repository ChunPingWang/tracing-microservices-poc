# Frontend Dockerfile
# 多階段建置：Node.js 建置 + Nginx 服務

# ===== 建置階段 =====
FROM node:18-alpine AS builder

WORKDIR /app

# 複製 package 檔案
COPY package*.json ./

# 安裝依賴
RUN npm ci

# 複製原始碼
COPY . .

# 建置
RUN npm run build

# ===== 執行階段 =====
FROM nginx:alpine

# 複製 Nginx 設定
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 複製建置產物
COPY --from=builder /app/dist /usr/share/nginx/html

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
