import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { resolve } from 'node:path';

/**
 * The Spring backend declares no CORS policy, so the browser would block direct
 * calls from :4200 to :8080. Proxying /api through the dev server keeps every
 * request same-origin and sidesteps CORS entirely.
 */
const apiTarget = process.env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080';

const proxy = {
  '/api': { target: apiTarget, changeOrigin: true }
};

export default defineConfig({
  // The app (and its index.html) lives here, while nx runs vite from the workspace root.
  root: __dirname,
  plugins: [react(), tailwindcss()],
  build: {
    outDir: resolve(__dirname, '../../dist/apps/commerce-admin'),
    emptyOutDir: true
  },
  server: {
    port: 4200,
    proxy
  },
  preview: {
    port: 4200,
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
