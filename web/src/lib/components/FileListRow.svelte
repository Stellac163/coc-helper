<script lang="ts">
  import Icon from './Icon.svelte';
  import GroupedListItem from './GroupedListItem.svelte';
  import { fileTypeIcon, humanSize } from '$lib/ui/files';
  import type { FileEntity } from '$lib/data/types';

  let { file, index, count, onOpen, onDownload, onDelete }: {
    file: FileEntity;
    index: number;
    count: number;
    onOpen: () => void;
    onDownload: () => void;
    onDelete: () => void;
  } = $props();
</script>

<GroupedListItem
  {index}
  {count}
  icon={fileTypeIcon(file.mimeType)}
  title={file.name}
  subtitle={humanSize(file.sizeBytes)}
  onclick={onOpen}
>
  {#snippet trailing()}
    <button
      class="icon-btn"
      onclick={(e) => {
        e.stopPropagation();
        onDownload();
      }}
      aria-label="下载"
    >
      <Icon name="download" size={24} />
    </button>
    <button
      class="icon-btn"
      onclick={(e) => {
        e.stopPropagation();
        onDelete();
      }}
      aria-label="删除"
    >
      <Icon name="delete" size={24} />
    </button>
    <Icon name="chevron_right" size={24} />
  {/snippet}
</GroupedListItem>
