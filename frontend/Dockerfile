# Development image — compose mounts ./frontend over /app and keeps this
# node_modules via the anonymous volume declared in docker-compose.yml.
FROM node:22-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

EXPOSE 5173

# --host: Vite must bind 0.0.0.0 inside the container for port mapping.
CMD ["npm", "run", "dev", "--", "--host"]
