<script lang="ts">
  import { snackbar } from '$lib/ui/snackbar.svelte';
</script>

{#if snackbar.visible}
  <div class="snackbar">{snackbar.message}</div>
{/if}
