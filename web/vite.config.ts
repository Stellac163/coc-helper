import { sveltekit } from '@sveltejs/kit/vite';
import { SvelteKitPWA } from '@vite-pwa/sveltekit';
import { defineConfig } from 'vite';

const base = process.env.BASE_PATH || '/';

export default defineConfig({
  // 允许通过临时隧道域名（如 *.trycloudflare.com）访问本地开发服务器
  server: {
    allowedHosts: true
  },
  plugins: [
    sveltekit(),
    SvelteKitPWA({
      base,
      registerType: 'autoUpdate',
      manifest: {
        name: 'COC 跑团助手',
        short_name: '跑团助手',
        description: '克苏鲁的呼唤（COC）跑团模组与角色管理助手',
        lang: 'zh-CN',
        start_url: base,
        scope: base,
        display: 'standalone',
        orientation: 'any',
        background_color: '#141317',
        theme_color: '#6750A4',
        icons: [
          { src: 'pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png' },
          { src: 'pwa-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' }
        ]
      }
    })
  ]
});
