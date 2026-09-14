<script lang="ts">
  import type { Snippet } from 'svelte';
  import Icon from './Icon.svelte';

  let { index, count, icon, title, subtitle = '', onclick, trailing }: {
    index: number;
    count: number;
    icon: string;
    title: string;
    subtitle?: string;
    onclick: () => void;
    trailing?: Snippet;
  } = $props();

  function radius(): string {
    const outer = '28px';
    const inner = '8px';
    if (count === 1) return outer;
    if (index === 0) return `${outer} ${outer} ${inner} ${inner}`;
    if (index === count - 1) return `${inner} ${inner} ${outer} ${outer}`;
    return inner;
  }
</script>

<div
  class="grouped-item"
  style:border-radius={radius()}
  role="button"
  tabindex="0"
  onclick={onclick}
  onkeydown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onclick();
    }
  }}
>
  <span class="circle-badge"><Icon name={icon} size={24} /></span>
  <div class="grow">
    <div class="gi-title ellipsis">{title}</div>
    {#if subtitle}<div class="gi-sub ellipsis">{subtitle}</div>{/if}
  </div>
  {@render trailing?.()}
</div>
