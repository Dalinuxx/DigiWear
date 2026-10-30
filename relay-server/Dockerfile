# Ultra-lightweight Node.js Alpine base (< 50MB image, < 15MB runtime RAM)
FROM node:20-alpine

WORKDIR /app

# Install dependencies
COPY package.json ./
RUN npm install --omit=dev --ignore-scripts

# Copy server code
COPY server.js ./

# Set environment defaults
ENV PORT=8080
ENV HOST=0.0.0.0
ENV NODE_ENV=production

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s CMD wget -qO- http://127.0.0.1:8080/health || exit 1

CMD ["node", "server.js"]
