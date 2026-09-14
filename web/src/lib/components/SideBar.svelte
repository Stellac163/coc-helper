<script lang="ts">
  import Icon from './Icon.svelte';
  import { R } from '$lib/nav/routes';

  const navItems = [
    { route: R.home, label: '模组', icon: 'home', iconOutline: 'home_outline' },
    { route: R.characters, label: '角色', icon: 'person', iconOutline: 'person_outline' },
    { route: R.dice, label: '骰子', icon: 'star_half', iconOutline: 'star_half' },
    { route: R.tools, label: '工具', icon: 'build', iconOutline: 'build' }
  ];

  let { currentRoute, onNavigate }: { currentRoute: string; onNavigate: (r: string) => void } = $props();
</script>

<aside class="sidebar">
  <div class="sidebar-title">COC 跑团助手</div>
  {#each navItems as item (item.route)}
    {@const selected = currentRoute === item.route}
    <button class="nav-item" class:active={selected} onclick={() => onNavigate(item.route)}>
      <Icon name={selected ? item.icon : item.iconOutline} size={24} color={selected ? 'var(--on-secondary-container)' : 'var(--text-muted)'} />
      <span>{item.label}</span>
    </button>
  {/each}
</aside>
