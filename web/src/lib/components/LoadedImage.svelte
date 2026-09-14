<script lang="ts">
  import Icon from './Icon.svelte';
  let { uri, radius = 0, icon = 'image', iconSize = 40, fit = 'cover' }: {
    uri?: string | null;
    radius?: number;
    icon?: string;
    iconSize?: number;
    fit?: string;
  } = $props();
</script>

{#if uri}
  <img class="loaded-image" src={uri} alt="" style="border-radius:{radius}px;object-fit:{fit};" />
{:else}
  <div class="image-placeholder" style="border-radius:{radius}px;">
    <Icon name={icon} size={iconSize} />
  </div>
{/if}
