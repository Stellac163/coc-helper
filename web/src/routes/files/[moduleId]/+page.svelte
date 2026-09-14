<script lang="ts">
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { FILE_COMPANION, type FileEntity } from '$lib/data/types';
  import { newFile } from '$lib/data/factories';
  import { pickFile, downloadFile } from '$lib/data/fileStore';
  import { snackbar } from '$lib/ui/snackbar.svelte';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import FileListRow from '$lib/components/FileListRow.svelte';
  import FileViewer from '$lib/components/FileViewer.svelte';
  import Dialog from '$lib/components/Dialog.svelte';

  const moduleId = Number(page.params.moduleId);
  const files = $derived(repo.files.forModuleKind(moduleId, FILE_COMPANION));

  let viewing = $state<FileEntity | null>(null);
  let deleting = $state<FileEntity | null>(null);

  async function pick() {
    const picked = await pickFile(['*/*']);
    if (!picked) return;
    const base64 = picked.dataUrl.slice(picked.dataUrl.indexOf('base64,') + 7);
    repo.files.insert(newFile(moduleId, picked.name, picked.mimeType, base64, picked.sizeBytes, FILE_COMPANION));
    snackbar.show(`已添加：${picked.name}`);
  }
</script>

<div class="page">
  <SectionTopBar title="配套组件" onback={() => history.back()} rightIcon="cloud_upload" onright={pick} />
  <div class="page-body">
    {#if files.length === 0}
      <EmptyState text="还没有配套文件，点击右上角上传" icon="insert_drive_file" />
    {:else}
      <div class="grouped">
        {#each files as file, i (file.id)}
          <FileListRow
            {file}
            index={i}
            count={files.length}
            onOpen={() => (viewing = file)}
            onDownload={() => downloadFile(file.name, file.mimeType, file.contentBase64)}
            onDelete={() => (deleting = file)}
          />
        {/each}
      </div>
    {/if}
  </div>
</div>

{#if viewing}
  <FileViewer file={viewing} onclose={() => (viewing = null)} />
{/if}

{#if deleting}
  {@const f = deleting}
  <Dialog title="删除文件" onclose={() => (deleting = null)}>
    {#snippet children()}
      <p>确定要删除“{f.name}”吗？此操作不可撤销。</p>
    {/snippet}
    {#snippet actions()}
      <button class="btn text" onclick={() => (deleting = null)}>取消</button>
      <button
        class="btn text"
        style="color:var(--danger);"
        onclick={() => {
          repo.files.delete(f);
          deleting = null;
        }}
      >删除</button>
    {/snippet}
  </Dialog>
{/if}
