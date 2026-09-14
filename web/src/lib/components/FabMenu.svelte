<script lang="ts">
  import Icon from './Icon.svelte';

  export interface FabMenuItem {
    icon: string;
    label: string;
    onClick: () => void;
  }

  let { expanded, onToggle, items, fabIcon, fabDescription = '' }: {
    expanded: boolean;
    onToggle: () => void;
    items: FabMenuItem[];
    fabIcon: string;
    fabDescription?: string;
  } = $props();
</script>

<div class="fab-menu">
  {#if expanded}
    <div class="fab-items">
      {#each items as item (item.label)}
        <button class="fab-item" onclick={item.onClick}>
          <Icon name={item.icon} size={24} />
          <span>{item.label}</span>
        </button>
      {/each}
    </div>
  {/if}
  <button class="fab" onclick={onToggle} aria-label={fabDescription}>
    <Icon name={expanded ? 'close' : fabIcon} size={24} />
  </button>
</div>
