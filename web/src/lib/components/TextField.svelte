<script lang="ts">
  import { autosize } from '$lib/ui/autosize';

  let { label, value = $bindable(''), single = true, minLines = 1, type = 'text', placeholder = '', autofocus = false }: {
    label?: string;
    value?: string;
    single?: boolean;
    minLines?: number;
    type?: string;
    placeholder?: string;
    autofocus?: boolean;
  } = $props();
</script>

<div class="field">
  {#if label}<label class="label">{label}</label>{/if}
  {#if single}
    <input class="text-input" {type} bind:value {placeholder} {autofocus} />
  {:else}
    <textarea class="text-input" bind:value {placeholder} rows={minLines} {autofocus} use:autosize={value}></textarea>
  {/if}
</div>
