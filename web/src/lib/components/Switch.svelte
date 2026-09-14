<script lang="ts">
  let { checked = $bindable(false), onchange }: {
    checked?: boolean;
    onchange?: (v: boolean) => void;
  } = $props();
</script>

<label class="switch">
  <input type="checkbox" bind:checked onchange={(e) => onchange?.(e.currentTarget.checked)} />
  <span class="track"><span class="thumb"></span></span>
</label>
