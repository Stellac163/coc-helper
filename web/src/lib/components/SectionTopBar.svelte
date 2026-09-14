<script lang="ts">
  import Icon from './Icon.svelte';
  let { title, onback, rightIcon, onright }: {
    title: string;
    onback?: () => void;
    rightIcon?: string;
    onright?: () => void;
  } = $props();
</script>

<div class="top-bar">
  {#if onback}
    <button class="icon-btn" onclick={onback} aria-label="返回"><Icon name="arrow_back" /></button>
  {/if}
  <div class="title ellipsis">{title}</div>
  {#if rightIcon && onright}
    <button class="icon-btn" onclick={onright} aria-label={rightIcon}><Icon name={rightIcon} /></button>
  {/if}
</div>
