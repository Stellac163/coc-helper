<script lang="ts">
  import { goto } from '$app/navigation';
  import { page } from '$app/state';
  import { repo } from '$lib/data/repository.svelte';
  import { FILE_ORIGINAL } from '$lib/data/types';
  import { newFile } from '$lib/data/factories';
  import { R } from '$lib/nav/routes';
  import { pickFile } from '$lib/data/fileStore';
  import { snackbar } from '$lib/ui/snackbar.svelte';
  import Icon from '$lib/components/Icon.svelte';
  import EmptyState from '$lib/components/EmptyState.svelte';
  import SectionTopBar from '$lib/components/SectionTopBar.svelte';
  import FileContent from '$lib/components/FileContent.svelte';

  const moduleId = Number(page.params.moduleId);
  const originals = $derived(repo.files.forModuleKind(moduleId, FILE_ORIGINAL));
  const original = $derived(originals[0] ?? null);

  async function pick() {
    const picked = await pickFile([
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ]);
    if (!picked) return;
    const base64 = picked.dataUrl.slice(picked.dataUrl.indexOf('base64,') + 7);
    for (const f of originals) repo.files.delete(f);
    repo.files.insert(
      newFile(moduleId, picked.name, picked.mimeType, base64, picked.sizeBytes, FILE_ORIGINAL)
    );
    const m = repo.modules.getById(moduleId);
    if (m) repo.modules.update({ ...m, hasOriginalDoc: true });
    snackbar.show(`已上传原文：${picked.name}`);
  }
</script>

<div class="page">
  <SectionTopBar title="原文" onback={() => history.back()} rightIcon="cloud_upload" onright={pick} />
  <div class="page-body">
    {#if !original}
      <EmptyState text="尚未上传原文，点击右上角上传 PDF 或 Word 文档" icon="description" />
    {:else}
      <FileContent file={original} />
    {/if}
  </div>

  <button class="fab" onclick={() => goto(R.files(moduleId))} aria-label="配套组件">
    <Icon name="folder_open" size={24} />
  </button>
</div>
