<script lang="ts">
  import Icon from './Icon.svelte';
  let { text, icon = 'warning' }: { text: string; icon?: string } = $props();
</script>

<div class="empty">
  <div class="icon"><Icon name={icon} size={40} /></div>
  <div class="hint">{text}</div>
</div>
