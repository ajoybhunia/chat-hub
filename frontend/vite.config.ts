import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import { bffPlugin } from './bff/proxy'
import { configureLogger } from './src/lib/logger'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Server-side env (any prefix) — UPSTREAM_URL is never exposed to the bundle.
  const env = loadEnv(mode, process.cwd(), '')
  const isProd = mode === 'production'

  // `vite dev` -> pretty debug logs; `vite build`/`vite preview` -> single-line JSON.
  configureLogger({
    format: isProd ? 'json' : 'pretty',
    minLevel: isProd ? 'info' : 'debug',
  })

  return {
    plugins: [
      react(),
      bffPlugin({ upstream: env.UPSTREAM_URL || 'http://localhost:8000' }),
    ],
  }
})
