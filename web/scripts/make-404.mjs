// GitHub Pages SPA 兜底：把 build/index.html 复制为 build/404.html。
// 当访问不存在的深链（如 /coc-helper/pc/5）时，GitHub Pages 会返回 404.html
// 的内容，但浏览器 URL 保持不变，SvelteKit 客户端路由据此渲染正确页面。
// 由于 adapter-static 使用绝对资源路径（/coc-helper/_app/...），
// 复制后的 404.html 从任意路径都能正确加载资源。
import { copyFileSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const src = join(root, 'build', 'index.html');
const dst = join(root, 'build', '404.html');

if (!existsSync(src)) {
  console.error('build/index.html not found — run `vite build` first');
  process.exit(1);
}

copyFileSync(src, dst);
console.log('wrote build/404.html (SPA fallback)');
