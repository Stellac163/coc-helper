<script lang="ts">
  import Icon from './Icon.svelte';
  import FileContent from './FileContent.svelte';
  import type { FileEntity } from '$lib/data/types';

  let { file, onclose }: { file: FileEntity; onclose: () => void } = $props();
</script>

<div class="viewer">
  <div class="top-bar">
    <button class="icon-btn" onclick={onclose} aria-label="关闭"><Icon name="arrow_back" /></button>
    <div class="title ellipsis">{file.name}</div>
  </div>
  <div class="viewer-body">
    <FileContent {file} />
  </div>
</div>
