<script lang="ts">
  import type { Snippet } from 'svelte';
  let { title, onclose, children, actions }: {
    title: string;
    onclose?: () => void;
    children: Snippet;
    actions?: Snippet;
  } = $props();
</script>

<div class="dialog-backdrop" onclick={onclose}>
  <div class="dialog" onclick={(e) => e.stopPropagation()}>
    <div class="title">{title}</div>
    <div class="dialog-body">{@render children()}</div>
    {#if actions}<div class="actions">{@render actions()}</div>{/if}
  </div>
</div>
