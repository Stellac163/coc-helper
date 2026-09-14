import { base } from '$app/paths';

/** 给应用内相对路径显式拼上 base 前缀。
 *  SvelteKit 的 goto('/xxx') 会把以 '/' 开头的路径按「相对源站根」解析
 *  （new URL('/xxx', document.baseURI)），从而丢掉 /coc-helper 前缀、跳成 404。
 *  所以这里统一把 base 前缀加到 URL 上，保证跳转地址始终落在部署目录下。 */
function withBase(p: string): string {
  if (!base || base === '/') return p;
  return p === '/' ? base : base + p;
}

/** 路由表：与 Kotlin Routes 一一对应（返回值已含 base 前缀，可直接传给 goto）。 */
export const R = {
  home: withBase('/'),
  characters: withBase('/characters'),
  dice: withBase('/dice'),
  tools: withBase('/tools'),
  clues: withBase('/clues'),
  login: withBase('/login'),
  combat: withBase('/combat'),
  chase: withBase('/chase'),
  timer: withBase('/timer'),
  module: (id: number) => withBase(`/module/${id}`),
  original: (id: number) => withBase(`/original/${id}`),
  files: (id: number) => withBase(`/files/${id}`),
  intro: (id: number) => withBase(`/intro/${id}`),
  timeline: (id: number) => withBase(`/timeline/${id}`),
  locations: (id: number) => withBase(`/locations/${id}`),
  location: (id: number) => withBase(`/location/${id}`),
  npcs: (id: number) => withBase(`/npcs/${id}`),
  pcs: (id: number) => withBase(`/pcs/${id}`),
  pc: (id: number, avatar = false) => withBase(`/pc/${id}?avatar=${avatar}`),
  npc: (id: number) => withBase(`/npc/${id}`)
};

/** 去掉 base 前缀，得到应用内相对路径（如 /coc-helper/characters → /characters）。 */
export function relPath(pathname: string): string {
  let p = pathname;
  if (base && base !== '/' && p.startsWith(base)) {
    p = p.slice(base.length);
    if (!p.startsWith('/')) p = '/' + p;
  }
  return p || '/';
}

/** 当前是否处于 4 个底部 Tab 之一；返回 tab 名（home/characters/dice/tools），否则空串。 */
export function currentTab(pathname: string): string {
  const rel = relPath(pathname);
  if (rel === '/') return 'home';
  const seg = rel.replace(/^\//, '').split('/')[0];
  return seg === 'characters' || seg === 'dice' || seg === 'tools' ? seg : '';
}

export function goBack() {
  history.back();
}

/** 路由在应用导航流中的位置（用于决定页面切换的滑动方向）。
 *  4 个主 Tab 顺序：模组 → 角色 → 骰子 → 工具；每个 Tab 内部的子页按自然展开顺序排在其后。 */
const KIND_ORDER: Record<string, number> = {
  '': 0,          // 首页（模组）
  module: 10,     // 模组详情
  original: 20,   // 原文
  files: 30,      // 文件
  intro: 40,      // 简介与招募
  timeline: 50,   // 时间轴
  locations: 60,  // 地点列表
  location: 70,   // 地点详情
  npcs: 80,       // NPC 列表
  npc: 90,        // NPC 详情
  pcs: 100,       // 模组角色卡列表
  characters: 110, // 角色（调查员）
  pc: 120,        // 角色详情
  dice: 130,      // 骰子
  tools: 140,     // 工具
  login: 150,     // 登录
  combat: 160,    // 战斗轮
  chase: 170,     // 追逐战
  timer: 180,     // 计时器
  clues: 190      // 线索板
};

/** 返回路由在导航流中的位置：越大越「靠后」。 */
export function routePos(pathname: string): number {
  const rel = relPath(pathname);
  if (rel === '/') return KIND_ORDER[''];
  const seg = rel.replace(/^\//, '').split('/')[0];
  return KIND_ORDER[seg] ?? 0;
}
