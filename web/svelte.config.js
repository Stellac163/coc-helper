import adapter from '@sveltejs/adapter-static';

// GitHub Pages 项目站点的路径前缀（部署时由 CI 通过 BASE_PATH 注入），
// 本地开发/预览默认为根路径。SvelteKit 要求 paths.base 不能以 '/' 结尾，
// 而 PWA 插件（vite.config.ts）需要带斜杠，故这里统一去掉末尾斜杠。
const base = (process.env.BASE_PATH || '').replace(/\/+$/, '');

/** @type {import('@sveltejs/kit').Config} */
const config = {
  kit: {
    adapter: adapter({
      pages: 'build',
      assets: 'build',
      fallback: 'index.html',
      precompress: false,
      strict: false
    }),
    paths: {
      base,
      relative: false
    }
  }
};

export default config;
