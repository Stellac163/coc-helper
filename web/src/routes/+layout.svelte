<script lang="ts">
  import '../app.css';
  import { onMount } from 'svelte';
  import { page } from '$app/state';
  import { goto, onNavigate } from '$app/navigation';
  import { repo } from '$lib/data/repository.svelte';
  import { settingsStore } from '$lib/data/settingsStore.svelte';
  import { applyTheme } from '$lib/ui/theme';
  import { initRipple } from '$lib/ui/ripple';
  import { relPath, currentTab, routePos } from '$lib/nav/routes';
  import SideBar from '$lib/components/SideBar.svelte';
  import AppBottomBar from '$lib/components/AppBottomBar.svelte';
  import Snackbar from '$lib/components/Snackbar.svelte';
  import type { Snippet } from 'svelte';

  let { children }: { children: Snippet } = $props();

  let isDesktop = $state(false);
  let inited = $state(false);
  let direction = $state<'forward' | 'back'>('forward');

  onMount(() => {
    const mq = matchMedia('(min-width: 720px)');
    isDesktop = mq.matches;
    const onMq = () => (isDesktop = mq.matches);
    mq.addEventListener('change', onMq);

    const darkMq = matchMedia('(prefers-color-scheme: dark)');
    const onColor = () => applyTheme();
    darkMq.addEventListener('change', onColor);

    const removeRipple = initRipple();

    repo.init().then(() => {
      inited = true;
      document.getElementById('app-loading')?.remove();
    });

    return () => {
      mq.removeEventListener('change', onMq);
      darkMq.removeEventListener('change', onColor);
      removeRipple();
    };
  });

  // 滑动方向由「目标路由在导航流中的位置」决定（对齐原 Compose slideEnter / slidePopEnter）：
  //   目标在当前位置之后（模组→角色→骰子→工具，或子页展开）→ 从右往左滑入；
  //   目标在当前位置之前（回退 / 回退到前面的 Tab）→ 从左往右滑入。
  onNavigate((nav) => {
    const from = nav.from?.url?.pathname ? relPath(nav.from.url.pathname) : currentRoute;
    const to = nav.to?.url?.pathname ? relPath(nav.to.url.pathname) : currentRoute;
    direction = routePos(to) >= routePos(from) ? 'forward' : 'back';
  });

  // 主题切换（明/暗/跟随系统）变化时即时应用；跟随系统时由上面的 listener 兜底。
  $effect(() => {
    applyTheme();
  });

  const currentRoute = $derived(relPath(page.url.pathname));
  const tab = $derived(currentTab(page.url.pathname));
  const showTabBar = $derived(!isDesktop && tab !== '');

  function onNavigateTab(route: string) {
    if (route !== currentRoute) goto(route);
  }

  // 路由变化时回到顶部（滚动容器 .app-content 跨路由持久存在）。
  $effect(() => {
    currentRoute;
    const el = document.querySelector('.app-content');
    if (el) el.scrollTop = 0;
  });

  // 页面切换动效统一由 app.css 里的关键帧动画负责（nav-forward / nav-back）。
  // 这里只负责算出「滑入方向」并挂到 .route-view 的 data-nav 上：
  //   forward（目标在导航流之后）→ 从右往左滑入；back → 从左往右滑入。
  // 旧页瞬时卸载、新页纯 transform 滑入且快速变实体，避免两页半透明交叠「透出底下」的僵硬感。
</script>

{#if inited}
  <div class="app-shell" class:desktop={isDesktop} class:has-tab={showTabBar}>
    {#if isDesktop}
      <SideBar {currentRoute} onNavigate={onNavigateTab} />
    {/if}
    <div class="app-content">
      <div class="content-frame">
        <div class="route-stage">
          {#key currentRoute}
            <div class="route-view" data-nav={direction}>
              {@render children()}
            </div>
          {/key}
        </div>
      </div>
    </div>
    {#if showTabBar}
      <AppBottomBar {currentRoute} onNavigate={onNavigateTab} />
    {/if}
  </div>
  <Snackbar />
{/if}
