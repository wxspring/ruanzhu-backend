# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Cache dependencies
COPY package.json package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com

# Build application
COPY . .
RUN npm run build

# Runtime stage - nginx
FROM nginx:alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets
COPY --from=build /app/dist /usr/share/nginx/html

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
