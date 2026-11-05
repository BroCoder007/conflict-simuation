import { defineConfig, loadEnv } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
export default defineConfig(({mode}) => ({
  base: loadEnv(mode, '.', 'VITE_').VITE_PAGES === 'true' ? '/conflict-simuation/' : '/',
  plugins: [svelte()],
  server: { proxy: { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: false } } },
  build: { outDir: 'dist' }
}));
