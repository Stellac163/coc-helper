<script lang="ts">
  import Icon from './Icon.svelte';

  export interface ToolbarAction {
    icon: string;
    description: string;
    onClick: () => void;
  }

  let { actions }: { actions: ToolbarAction[] } = $props();
</script>

<div class="floating-toolbar">
  {#each actions as action (action.description)}
    <button class="icon-btn" onclick={action.onClick} aria-label={action.description}>
      <Icon name={action.icon} size={24} />
    </button>
  {/each}
</div>
