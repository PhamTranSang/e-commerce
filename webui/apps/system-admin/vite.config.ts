import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { resolve } from 'node:path';

/** Same reasoning as commerce-admin: keep API calls same-origin instead of needing CORS. */
const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

const proxy = {
  '/api': { target: apiTarget, changeOrigin: true }
};

export default defineConfig({
  // The app (and its index.html) lives here, while nx runs vite from the workspace root.
  root: __dirname,
  plugins: [react(), tailwindcss()],
  build: {
    outDir: resolve(__dirname, '../../dist/apps/system-admin'),
    emptyOutDir: true
  },
  server: {
    port: 4201,
    proxy
  },
  preview: {
    port: 4201,
    proxy
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '@shell': resolve(__dirname, '../../packages/shell/src'),
      '@ui': resolve(__dirname, '../../packages/ui/src'),
      '@api': resolve(__dirname, '../../packages/api/src'),
      '@domain': resolve(__dirname, '../../packages/types/src'),
      '@config': resolve(__dirname, '../../packages/config/src')
    }
  }
});
