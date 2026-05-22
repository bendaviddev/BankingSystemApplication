import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // host: true binds to 0.0.0.0 so the dev server is reachable through
    // Docker port mapping. Without it Vite listens on 127.0.0.1 only and
    // the container port never reaches the host machine.
    host: true,
    proxy: {
      "/api": {
        // Inside Docker "localhost" resolves to the frontend container itself,
        // not the backend. BACKEND_URL lets docker-compose inject the correct
        // service name (http://backend:8080). Falls back to localhost for
        // running outside Docker.
        target: process.env.BACKEND_URL ?? "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
