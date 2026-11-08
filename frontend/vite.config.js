import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Khi phat trien: giao dien chay o cong 5173, goi /api sang Spring Boot cong 8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Cho phep truy cap qua dia chi chuyen tiep cua GitHub Codespaces
    allowedHosts: ['.app.github.dev', 'localhost'],
    proxy: { '/api': 'http://localhost:8080' },
  },
});
