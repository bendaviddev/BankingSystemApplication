FROM node:22-alpine
WORKDIR /app

# Copy manifests first so the npm install layer is cached separately from source.
COPY package*.json ./
RUN npm ci

COPY . .

EXPOSE 5173

CMD ["npm", "run", "dev"]
