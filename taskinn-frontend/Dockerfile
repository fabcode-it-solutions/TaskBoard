# Multi-stage build for Angular frontend
# Stage 1: Build the Angular application
FROM registry.reset.inso-w.at/pub/docker/node:current-alpine AS build

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies (including devDependencies needed for build)
RUN npm ci

# Copy source code
COPY . .

# Build the application
# Note: Build configuration will be set via build args in CI/CD
ARG BUILD_CONFIGURATION=production
ARG BASE_HREF=/
RUN npm run build -- --configuration=${BUILD_CONFIGURATION} --base-href=${BASE_HREF}

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copy custom nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf

# Copy built application from build stage
COPY --from=build /app/dist/taskinn-frontend/browser /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Start nginx
CMD ["nginx", "-g", "daemon off;"]